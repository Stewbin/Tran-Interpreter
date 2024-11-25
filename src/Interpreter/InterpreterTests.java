package Interpreter;

import AST.TranNode;
import Lexer.Lexer;
import Parser.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class InterpreterTests {
    private static List<String> getConsole(TranNode tn) {
        for (var c : tn.Classes)
            if (c.name.equals("console")) {
                for (var m : c.methods)  {
                    if (m.name.equals("write")) {
                        return ((ConsoleWrite)m).console;
                    }
                }
            }
        throw new RuntimeException("Unable to find console");
    }

    private static TranNode run(String program) {
        var l  = new Lexer(program);
        try {
            var tokens = l.Lex();
//            tokens.forEach(System.out::println);
            var tran = new TranNode();
            var p = new Parser(tran,tokens);
            p.Tran();
            var i = new Interpreter(tran);
            i.start();
            return tran;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void SimpleAdd() {
        String program = "class SimpleAdd\n" +
                         "\n" +
                         "    shared start()\n" +
                         "        number x\n" +
                         "        number y\n" +
                         "        number z\n" +
                         "\n" +
                         "        x = 6\n" +
                         "        y = 6\n" +
                         "        z = x + y\n" +
                         "        console.write(z)\n";
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(1,c.size());
        Assertions.assertEquals("12.0",c.getFirst());
    }

    @Test
    public void SimpleAddInstantiate() {
        String program = "class SimpleAdd\n" +
                         "    number x\n" +
                         "    number y\n" +
                         "    \n" +
                         "    construct()\n" +
                         "        x = 6\n" +
                         "        y = 6\n" +
                         "    \n" +
                         "    add()\n" +
                         "        number z\n" +
                         "        z = x + y\n" +
                         "        console.write(z)\n" +
                         "    \n" +
                         "    shared start()\n" +
                         "        SimpleAdd t\n" +
                         "        t = new SimpleAdd()\n" +
                         "        t.add()\n" +
                         "    \n";
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(1,c.size());
        Assertions.assertEquals("12.0",c.getFirst());
    }

    @Test
    public void SimpleAddInstantiateAndPrint() {
        String program = "class SimpleAdd\n" +
                         "    number x\n" +
                         "    number y\n" +
                         "\n" +
                         "    construct()\n" +
                         "        x = 6\n" +
                         "        y = 6\n" +
                         "\n" +
                         "    add()\n" +
                         "        number z\n" +
                         "        z = x + y\n" +
                         "        console.write(z)\n" +
                         "\n" +
                         "    shared start()\n" +
                         "        SimpleAdd t\n" +
                         "        t = new SimpleAdd()\n" +
                         "        t.add()\n" +
                         "\n";
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(1,c.size());
        Assertions.assertEquals("12.0",c.getFirst());
    }

    @Test
    public void Loop1() {
        String program = "class LoopOne\n" +
                         "    shared start()\n" +
                         "        boolean keepGoing\n" +
                         "        number n\n" +
                         "        n = 0\n" +
                         "        keepGoing = true\n" +
                         "        loop keepGoing\n" +
                         "        	  if n >= 15\n" +
                         "                keepGoing = false\n" +
                         "            else\n" +
                         "                n = n + 1\n" +
                         "                console.write(n)\n";
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(15,c.size());
        Assertions.assertEquals("1.0",c.getFirst());
        Assertions.assertEquals("15.0",c.getLast());
    }

    @Test
    public void student() {
        String program = "class student\n" +
                "    number gradea\n" +
                "    number gradeb\n" +
                "    number gradec\n" +
                "    string firstname\n" +
                "    string lastname\n" +
                "    \n" +
                "    construct (string fname, string lname, number ga, number gb, number gc)\n" +
                "        firstname = fname\n" +
                "        lastname = lname\n" +
                "        gradea = ga\n" +
                "        gradeb = gb\n" +
                "        gradec = gc\n" +
                "    \n" +
                "    getAverage() : number avg \n" +
                "        avg = (gradea + gradeb + gradec)/3\n" +
                "    \n" +
                "    print() \n" +
                "        console.write(firstname, \" \", lastname, \" \", getAverage())\n" + // What is this logging syntax?
                "    \n" +
                "    shared start()\n" +
                "        student sa\n" +
                "        student sb\n" +
                "        student sc\n" +
                "        sa = new student(\"michael\",\"phipps\",100,99,98)\n" +
                "        sb = new student(\"tom\",\"johnson\",80,75,83)\n" +
                "        sc = new student(\"bart\",\"simpson\",32,25,33)\n" +
                "        sa.print()\n" +
                "        sb.print()\n" +
                "        sc.print()\n";
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(3,c.size());
        Assertions.assertEquals("michael phipps 99.0",c.getFirst());
        Assertions.assertEquals("bart simpson 30.0",c.getLast());
    }


    @Test
    public void functionShouldNotMutateOutsideVariables() {
        String program = "class Math\n" +
                         "    shared avg(number a, number b) : number prod\n" +
                         "        a = a + b\n" +
                         "        b = a / 2\n" +
                         "        prod = b\n" +
                         "        \n" +
                         "    shared start()\n" +
                         "        number a\n" +
                         "        a = 10" +
                         "        number b\n" +
                         "        number c\n" +
                         "        c = avg(a, b)\n" +
                         "        console.write(c)\n";

    }

    public void throwExceptionAt_ReturnTypeMismatch() {
        String program = "class Matrix\n" +
                         "    number numRows\n" +
                         "        accessor:\n" +
                         "            value = numRows\n" +
                         "    number numColumns\n" +
                         "        accessor:\n" +
                         "            value = numColumns\n" +
                         "            \n" +
                         "    shared getShape() : number m, number n\n" +
                         "        m = numRows\n" +
                         "        n = numColumns" +
                         "        \n" +
                         "    shared start()\n" +
                         "        string m\n" + // These should be numbers
                         "        string n\n" +
                         "        m, n = getShape()\n";
        Assertions.assertThrows(RuntimeException.class, () -> run(program));
    }

    public void timesIteratorShouldWork() {
        String program = """
                class Counter implements iterator
                
                class CountToTwenty
                 
                    shared start()
                        number n
                        n = 100
                        Counter c 
                        c = n.times()
                        loop l = c
                            console.write(c + " times!")
                """;
        var tranNode = run(program);


        // Inspect console
        var c = getConsole(tranNode);
        Assertions.assertEquals(101,c.size());
        for (int i = 0; i < c.size(); i++) {
            Assertions.assertEquals(i + " times!", c.get(i));
        }
    }

    public void throwExceptionAt_nonIteratorAndNonBooleanDatumInLoop() {
        String program = """
            class Tran
                character char
                number counter
            
                construct(character c)
                    char = c
                    counter = 0
            
                shared start()
                    string str
                    loop i = str
                        counter = i
            """;

    }

    public void throwExceptionAt_nonIteratorObjectInIteratorLoop() {
        String program = """
                class Tran
                    character char
                    number counter
                
                    construct(character c)
                        char = c
                        counter = 0
                
                    shared start()
                        
                        Tran tron = new Tran('a')
                        loop i = tron
                            counter = i     
                """;
    }

    public void noCallerMethod_fromInsideSharedMethod_shouldSomething() {
        String program = """
                class Tran
                
                    shared awake()
                        console.write("1 + 1 = 11\\n")
                
                    shared start()
                        awake()
                        console.write("is false\\n")
                """;
    }
}

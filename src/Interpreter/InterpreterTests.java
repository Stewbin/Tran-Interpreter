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
            System.out.println(tokens);
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
                "\n" +
                "    construct (string fname, string lname, number ga, number gb, number gc)\n" +
                "        firstname = fname\n" +
                "        lastname = lname\n" +
                "        gradea = ga\n" +
                "        gradeb = gb\n" +
                "        gradec = gc\n" +
                "\n" +
                "    getAverage() : number avg \n" +
                "        avg = (gradea + gradeb + gradec)/3\n" +
                "\n" +
                "    print() \n" +
                "        console.write(firstname, \" \", lastname, \" \", getAverage())\n" + // What is this logging syntax?
                "\n" +
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
    public void functionShouldNotMutate_outsidePrimitiveVariables() {
        String program = """
                class Mutate
                    shared messUpNums(number a, number b) : number mean
                        a = a + b
                        b = a / 2
                        mean = b
                
                    shared messUpString(string in) : string out
                        in = in + " messified"
                        out = in
                
                    shared messUpChar(character in) : character out
                        in = 'b'
                        out = in
                        
                
                    shared start()
                        number a
                        number b
                        number c
                        a = 10
                        b = 20
                        c = Mutate.messUpNums(a, b)
                        console.write(a)
                        console.write(b)
                        console.write(c)
                
                        string s
                        s = "foobar"
                        console.write(Mutate.messUpString(s))
                        console.write(s)
                        
                        character char
                        char = 'a'
                        console.write(Mutate.messUpChar(char))
                        console.write(char)
                """;

        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(7,c.size());
        Assertions.assertEquals("10.0",c.get(0));
        Assertions.assertEquals("20.0",c.get(1));
        Assertions.assertEquals("15.0",c.get(2));
        Assertions.assertEquals("foobar messified",c.get(3));
        Assertions.assertEquals("foobar",c.get(4));
        Assertions.assertEquals("b",c.get(5));
        Assertions.assertEquals("a",c.get(6));
    }

    @Test
    public void throwExceptionAt_ReturnTypeMismatch() {
        String program = "class Matrix\n" +
                         "    number numRows\n" +
                         "        accessor:\n" +
                         "            value = numRows\n" +
                         "    number numColumns\n" +
                         "        accessor:\n" +
                         "            value = numColumns\n" +
                         "\n" +
                         "    shared getShape() : number m, number n\n" +
                         "        m = numRows\n" +
                         "        n = numColumns" +
                         "\n" +
                         "    shared start()\n" +
                         "        string m\n" + // These should be numbers
                         "        string n\n" +
                         "        m, n = getShape()\n";
        Assertions.assertThrows(RuntimeException.class, () -> run(program));
    }

    @Test
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

    @Test
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

        Assertions.assertThrows(RuntimeException.class, () -> run(program));
    }

    @Test
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
        Assertions.assertThrows(RuntimeException.class, () -> run(program));
    }

    @Test
    public void throwExceptionAt_noCallerMethod_fromInsideSharedMethod() {
        String program = """
                class Tran
                
                    shared awake()
                        console.write("1 + 1 = 11\\n")
                
                    shared start()
                        awake()
                        console.write("is false\\n")
                """;
        Assertions.assertThrows(RuntimeException.class, () -> run(program));
    }

    @Test
    public void successfullyInterpret_InteratorClass() {
        String program = """
                class Interator implements iterator
                    number max
                    number cur
                    
                    construct(number int)
                        max = int
                        cur = 1
                    
                    isAtEnd() : boolean b
                        if cur == max
                            b = true
                        else
                            b = false
                    
                    getNext() : boolean b, number i
                        b = isAtEnd()
                        if not b
                            i = cur
                            cur = cur + 1
                        else
                            i = -1
                
                class TestInterator
                    shared start()
                        Interator inter = new Interator(42)
                        loop i = inter
                            console.write(i)
                        loop j = 42.times()
                            console.write(j)
                """;
        var tranNode = run(program);
        // AST assertions
        Assertions.assertEquals(2,tranNode.Classes.size());
        Assertions.assertEquals("Interator", tranNode.Classes.get(0).name);
        Assertions.assertEquals(2, tranNode.Classes.get(0).members.size());
        Assertions.assertEquals(1, tranNode.Classes.get(0).constructors.size());
        Assertions.assertEquals(2, tranNode.Classes.get(0).methods.size());
        Assertions.assertEquals("TestInterator", tranNode.Classes.get(1).name);
        Assertions.assertEquals(0, tranNode.Classes.get(1).members.size());
        Assertions.assertEquals(0, tranNode.Classes.get(1).constructors.size());
        Assertions.assertEquals(1, tranNode.Classes.get(1).methods.size());
        // Console assertions
        var c = getConsole(tranNode);
        Assertions.assertEquals(84, c.size());
        for (int i = 0; i < 84; i++) {
            Assertions.assertEquals(String.valueOf(i % 42), c.get(i));
        }
    }
}

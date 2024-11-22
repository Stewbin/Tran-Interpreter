import AST.BuiltInMethodDeclarationNode;
import AST.TranNode;
import Interpreter.Interpreter;
import Interpreter.ConsoleWrite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class InterpreterTests {
    @Test
    public void SimpleAdd() {
        String program = "class SimpleAdd\n" +
                         "\n" +
                         "    shared start()\n" +
                         "        number x\n" +
                         "        number y\n" +
                         "        number z\n" +
                         "\n" + // Would this break ever happen in real code?
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
                "        console.write(firstname, \" \", lastname, \" \", getAverage())\n" +
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

    // TODO: Test for methods of the same name & return size, but different return types
    // TODO: Test for mutation of outside variables inside method calls
    @Test
    public void functionShouldNotMutateOutsideVariables() {
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
                         "        n = numColumns\n" +
                         "        \n" +
                         "    shared start()\n" +
                         "        string m\n" +
                         "        string n\n" +
                         "        m, n = getShape()\n";
        Assertions.assertThrows(RuntimeException.class, () -> run(program));
    }

    // TODO: Test for putting non-iterator and non-boolean in Loop expression
}

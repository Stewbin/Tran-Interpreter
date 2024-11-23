package Interpreter;

import AST.*;
import Interpreter.DataTypes.*;

import java.util.*;
import java.util.stream.IntStream;

public class Interpreter {
    private final TranNode top;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        this.top = top;
    }

    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() {
        // Find 'start' method
        var start = top.Classes.stream()
                .flatMap(classNode -> classNode.methods.stream())
                .filter(method -> method.name.equals("start") && method.isShared && !method.isPrivate)
                .findFirst();
        if (start.isEmpty())
            throw new RuntimeException("No 'start' method found");
        interpretMethodCall(Optional.empty(), start.get(), new LinkedList<>());
    }

    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        // Evaluate parameters
        var parameters = getParameters(object, locals, mc);
        // Find declaration for method //
        MethodDeclarationNode mDec;

        // Method caller is the object we're inside
        if (mc.objectName.isEmpty()) {
            if (object.isPresent()) {
                mDec = getMethodFromObject(object.get(), mc, parameters);
                return interpretMethodCall(object, mDec, parameters);
            }
            throw new RuntimeException("Caller object needed"); // 'mc' should be a static method no?
            // TODO: Handle `object` being empty, and method-caller being empty
        }
        // Method has caller
        var maybeClass = getClassByName(mc.objectName.get());
        if (maybeClass.isPresent()) {
            // The caller is a class, and the method is shared
            mDec = maybeClass.get().methods.stream()
                    .filter(m -> doesMatch(m, mc, parameters) && m.isShared && !m.isPrivate)
                    .findFirst()
                    .orElseThrow(
                            () -> new RuntimeException("shared method '%s' not found in '%s'".formatted(mc.methodName, maybeClass.get().name))
                    );
        // The method is shared, but the caller's class is the built-in-class `console`
        } else if (mc.objectName.get().equals("console")) {
            if (!mc.methodName.equals("write")) // `write` is the only method console has
                throw new RuntimeException("Method %s not found in 'console'".formatted(mc.methodName));
            return interpretMethodCall(object, new ConsoleWrite(), parameters);
        }
        var caller = findVariable(mc.objectName.get(), locals, object);
        // The caller is a local or member object
        if (caller instanceof ObjectIDT callingObject) {
            if (mc.methodName.equals("clone"))
                mDec = new CloneObjectMethod(callingObject);
            else
                mDec = getMethodFromObject(callingObject, mc, parameters);
        } else if (caller instanceof NumberIDT callingNumber) {
            if (!mc.methodName.equals("times"))
                throw new RuntimeException("Method %s not found for type <Number> ".formatted(mc.methodName));
            mDec = new CreateTimesIterator(callingNumber);
        } else {
            throw new RuntimeException("Method %s not found in %s".formatted(mc.methodName, mc.objectName));
        }
        return interpretMethodCall(object, mDec, parameters);
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        // Number of arguments passed must match expectation
        if (m.parameters.size() != values.size())
            throw new RuntimeException("Unexpected number of parameters");

        // Case: m is a built-in method
        if (m instanceof BuiltInMethodDeclarationNode builtInM) {
            return builtInM.Execute(values);
        }
        // Case: m is not built-in
        var locals = new HashMap<String, InterpreterDataType>(); // Make hashmap for local variables
        // Add parameters of m to local variables
        IntStream.range(0, m.parameters.size()).forEach(i -> locals.put(m.parameters.get(i).name, instantiate(m.parameters.get(i).type)));
        // Add locals of m to local variables
        IntStream.range(0, m.locals.size()).forEach(i -> locals.put(m.locals.get(i).name, instantiate(m.locals.get(i).type)));
        // Add return targets of m to local variables
        IntStream.range(0, m.returns.size()).forEach(i -> locals.put(m.returns.get(i).name, instantiate(m.returns.get(i).type)));

        interpretStatementBlock(object, m.statements, locals); // 'locals' is now modified
        // Collect return-values from locals, then return them
        var retVals = new LinkedList<InterpreterDataType>();
        for (var ret : m.returns) {
            if (locals.containsKey(ret.name)) {
                retVals.add(locals.get(ret.name));
            }
        }
        return retVals;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
        // Find Constructor //
        var constructorClass = getClassByName(mc.methodName).orElseThrow(() -> new RuntimeException("Class not found for constructor"));
        // Convert the parameters of 'mc' into IDT's
        var parameterValues = getParameters(callerObj, locals, mc);
        var constructorDeclaration = constructorClass.constructors.stream()
                .filter(c -> doesConstructorMatch(c, mc, parameterValues))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Constructor not found for %s".formatted(constructorClass.name)));
        // Actually Execute Constructor //
        interpretConstructorCall(newOne, constructorDeclaration, parameterValues); // Members of 'newOne' will be populated
    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
        // Check that the right number of parameters were passed in, if not throw.
        if (values.size() != c.parameters.size())
            throw new RuntimeException("Unexpected number of parameters");
        // Create local variables hashmap
        var locals = new HashMap<String, InterpreterDataType>(object.members); // Add members of 'object' to locals-hashmap
        // TODO: Handle local-variable and member name clash
        // Add local variables of 'c' to locals-hashmap
        for (var localVar : c.locals) {
            locals.put(localVar.name, instantiate(localVar.type));
        }
        // Add parameters to locals-hashmap
        for (int i = 0; i < c.parameters.size(); i++) {
            var param = c.parameters.get(i);
            if (!typeMatchToIDT(param.type, values.get(i)))
                throw new RuntimeException("Expected argument of type %s for %s".formatted(param.type, param.name));
            locals.put(param.name, values.get(i));
        }
        // Call interpretStatementBlock() on constructor body
        interpretStatementBlock(Optional.empty(), c.statements, locals);
    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block), run each statement.
     * Blocks, by definition, do every statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call interpretMethodCall(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
        for (var statement : statements) {
            if (statement instanceof AssignmentNode assignment) {
                var target = findVariable(assignment.target.name, locals, object);
                var value = evaluate(locals, object, assignment.expression);
                target.Assign(value);
            } else if (statement instanceof MethodCallStatementNode methodCall) {
                var retVals = findMethodForMethodCallAndRunIt(object, locals, methodCall);
                IntStream
                        .range(0, methodCall.returnValues.size())
                        .forEach( i -> locals.put(methodCall.returnValues.get(i).name, retVals.get(i)));
            } else if (statement instanceof LoopNode loop) {
                Optional<MethodDeclarationNode> getNextMethod = Optional.empty();
                var condition = evaluate(locals, object, loop.expression);
                if (condition instanceof ObjectIDT iterator) {
                    // Check if iterator implements 'iterator' interface
                    if (!typeMatchToIDT("iterator", iterator))
                        throw new RuntimeException("Object implementing 'iterator' expected");
                    // Create node that matches the 'getNext' method we're looking for
                    var getNextNode = new MethodCallStatementNode();
                    getNextNode.methodName = "getNext";
                    getNextNode.parameters = new ArrayList<>(0); // Empty parameters
                    getNextNode.returnValues = new ArrayList<>(3);
                    // Look for declaration that matches that node
                    getNextMethod = Optional.ofNullable(getMethodFromObject(iterator, getNextNode, new LinkedList<>()));
                } else if (!(condition instanceof BooleanIDT)) {
                    throw new RuntimeException("Iterator or Boolean expected as condition");
                }

                // Add loop "variable of iteration" to locals
                if (loop.assignment.isPresent()) {
                    var returnType = getNextMethod.map(m -> m.returns.get(1).type).orElse("boolean");
                    locals.put(loop.assignment.get().name, instantiate(returnType));
                }

                while (true) {
                    var returnedValues = getNextMethod.map(getNext -> interpretMethodCall(object, getNext, new LinkedList<>()));
                    // Determine if we should loop
                    boolean shouldContinue = returnedValues.isPresent()
                            ? ((BooleanIDT) returnedValues.get().getFirst()).Value // Condition is an iterator
                            : ((BooleanIDT) evaluate(locals, object, loop.expression)).Value; // Condition is a boolean
                    if (!shouldContinue)
                        break;
                    // Check if this loop is being assigned to a variable
                    if (loop.assignment.isPresent()) {
                        // Value of loop expression gets assigned
                        var loopExpVal = returnedValues.isPresent()
                                ? returnedValues.get().getFirst()
                                : new BooleanIDT(true); // The boolean expression is always true if execution reached here
                        findVariable(loop.assignment.get().name, locals, object).Assign(loopExpVal);
                    }
                    // Interpret loop body
                    interpretStatementBlock(object, loop.statements, locals);
                }
            } else if (statement instanceof IfNode ifStatement) {
                var condition = evaluate(locals, object, ifStatement.condition);
                if (condition instanceof BooleanIDT boolExp) {
                    if (boolExp.Value) // Value == true
                        interpretStatementBlock(object, ifStatement.statements, locals);
                    else
                        ifStatement.elseStatement.ifPresent(elseNode -> interpretStatementBlock(object, elseNode.statements, locals));
                    return;
                }
                throw new RuntimeException("Expected boolean expression");
            }
        }
    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call interpretMethodCall() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {
        // Boolean Literals (BooleanLiteralNode)
        if (expression instanceof BooleanLiteralNode booleanLiteral) {
            return new BooleanIDT(booleanLiteral.value);
        // Boolean Expressions (BooleanOpNode)
        } else if (expression instanceof BooleanOpNode booleanOpNode) {
            boolean l = ((BooleanIDT) evaluate(locals, object, booleanOpNode.left)).Value;
            boolean r = ((BooleanIDT) evaluate(locals, object, booleanOpNode.right)).Value;

            return new BooleanIDT(switch (booleanOpNode.op) {
                case and -> l && r;
                case or -> l || r;
            });
        // Comparisons (CompareNode)
        } else if (expression instanceof CompareNode compareNode) {
            var l = evaluate(locals, object, compareNode.left);
            var r = evaluate(locals, object, compareNode.right);

            return evaluateCompareExp(compareNode, l, r);
        // Number Literals (NumericLiteralNode)
        } else if (expression instanceof NumericLiteralNode numberLiteral) {
            return new NumberIDT(numberLiteral.value);
        // Math Expressions (MathOpNode)
        } else if (expression instanceof MathOpNode mathOpNode) {
            return evaluateMathExp(locals, object, mathOpNode);
            // String Literals (StringLiteralNode)
        } else if (expression instanceof StringLiteralNode stringLiteral) {
            return new StringIDT(stringLiteral.value);
        // Method Calls (MethodCallExpressionNode)
        } else if (expression instanceof MethodCallExpressionNode methodCallExp) {
            return findMethodForMethodCallAndRunIt(object, locals, new MethodCallStatementNode(methodCallExp)).getFirst();
        // Variable Reference (VariableReferenceNode)
        } else if (expression instanceof VariableReferenceNode variableReference) {
            return findVariable(variableReference.name, locals, object);
        // Object instantiation (NewNode)
        } else if (expression instanceof NewNode constructExp) {
            return evaluateObjectInstantiation(locals, object, constructExp);
        }
        throw new RuntimeException("Unknown expression: " + expression);
    }

    private InterpreterDataType evaluateMathExp(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, MathOpNode mathOpNode) {
        var l = evaluate(locals, object, mathOpNode.left);
        var r = evaluate(locals, object, mathOpNode.right);
        // If both l & r are numbers, do math operations
        if (l instanceof NumberIDT leftNum && r instanceof NumberIDT rightNum) {
            return new NumberIDT(switch (mathOpNode.op) {
                case add -> leftNum.Value + rightNum.Value;
                case subtract -> leftNum.Value - rightNum.Value;
                case multiply -> leftNum.Value * rightNum.Value;
                case divide -> leftNum.Value / rightNum.Value;
                case modulo -> leftNum.Value % rightNum.Value;
            });
        // If l & r are both strings, do string operations
        } else if (l instanceof StringIDT leftStr && r instanceof StringIDT rightStr) {
            return new StringIDT(leftStr.Value + rightStr.Value);
        } else {
            throw new RuntimeException(String.format("Undefined operation: '%s %s %s'", l, mathOpNode.op, r));
        }
    }

    private ObjectIDT evaluateObjectInstantiation(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, NewNode constructExp) {
        // Create MethodCallStatementNode to hold constructor information
        var mc = new MethodCallStatementNode();
        mc.parameters = constructExp.parameters;
        mc.methodName = constructExp.className;
        // Get AST-Node of the constructor's class
        var classNode = getClassByName(constructExp.className).orElseThrow(() -> new RuntimeException("Class not found for constructor: %s".formatted(constructExp)));
        // Create a new ObjectIDT according to the specifications of the class, to be returned
        var instance = new ObjectIDT(classNode);
        for (int i = 0; i < classNode.members.size(); i++) {
            var classField = classNode.members.get(i).declaration;
            instance.members.put(classField.name, instantiate(classField.type));
        }
        // Run constructor with allocated object
        findConstructorAndRunIt(object, locals, mc, instance); // Fields oof 'instance' will be populated
        return instance;
    }

    private static BooleanIDT evaluateCompareExp(CompareNode compareNode, InterpreterDataType l, InterpreterDataType r) {
        BooleanIDT retVal;
        if (l instanceof NumberIDT leftNum && r instanceof NumberIDT rightNum) {
            retVal = new BooleanIDT(switch (compareNode.op) {
                case lt -> leftNum.Value < rightNum.Value;
                case le -> leftNum.Value <= rightNum.Value;
                case gt -> leftNum.Value > rightNum.Value;
                case ge -> leftNum.Value >= rightNum.Value;
                case eq -> leftNum.Value == rightNum.Value;
                case ne -> leftNum.Value != rightNum.Value;
            });
        } else {
            retVal = new BooleanIDT(switch (compareNode.op) {
                case eq -> l == r;
                case ne -> l != r;
                default -> throw new RuntimeException(String.format("Undefined operation: %s %s %s", l, compareNode.op, r));
            });
        }
        return retVal;
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this method call?
     * We double-check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        boolean namesMatch = m.name.equals(mc.methodName);
        boolean returnsMatch = mc.returnValues.size() <= m.returns.size();
        boolean parametersMatch;
        // Check parameters //
        // Skip parameter check for built-in methods
        if (m instanceof BuiltInMethodDeclarationNode builtInMethod && builtInMethod.isVariadic) {
            parametersMatch = true;
        } else {
            boolean declaredAndCallMatch = mc.parameters.size() == m.parameters.size();
            boolean declaredAndArgumentsMatch = m.parameters.size() == parameters.size();
            boolean typesMatch = IntStream
                    .range(0, parameters.size())
                    .allMatch(i -> typeMatchToIDT(m.parameters.get(i).type, parameters.get(i)));
            parametersMatch = declaredAndCallMatch && declaredAndArgumentsMatch && typesMatch;
        }
        return namesMatch && returnsMatch && parametersMatch;
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        boolean parameterCountsMatch = mc.parameters.size() == c.parameters.size() && c.parameters.size() == parameters.size();
        boolean parameterTypesMatch = true;
        for (int i = 0; i < c.parameters.size(); i++) {
            if (!typeMatchToIDT(c.parameters.get(i).type, parameters.get(i)))
                parameterTypesMatch = false;
        }
        return parameterCountsMatch && parameterTypesMatch;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        return mc.parameters.stream().map(param -> evaluate(locals, object, param)).toList();
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc.) - does the string type match the name of that IDT ("boolean", etc.)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (referred to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {
        return switch (idt) {
            case BooleanIDT ignored -> type.equals("boolean");
            case NumberIDT ignored -> type.equals("number");
            case StringIDT ignored -> type.equals("string");
            case ObjectIDT obj -> type.equals(obj.astNode.name) || obj.astNode.interfaces.stream().anyMatch(type::equals); // astNode.name is Class name
            case ReferenceIDT ref -> typeMatchToIDT(type, ref.refersTo.orElseThrow(() -> new RuntimeException("Null Reference Exception: " + ref)));
            default -> throw new RuntimeException(String.format("Undefined type: '%s'", idt));
        };
    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        return object.astNode.methods.stream()
                .filter(m -> doesMatch(m, mc, parameters))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to resolve method call " + mc));
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        return top.Classes.stream().filter(classNode -> name.equals(classNode.name)).findFirst();
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object) {
        // Check in locals-hashmap
        if (locals.containsKey(name))
            return locals.get(name);
        if (object.isPresent()) {
            // Check in object members
            if (object.get().members.containsKey(name))
                return object.get().members.get(name);
        }
        throw new RuntimeException("Unable to find variable " + name);
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {
        return switch (type) {
            case "string" -> new StringIDT("");
            case "number" -> new NumberIDT(0);
            case "boolean" -> new BooleanIDT(false);
            case "character" -> new CharIDT(' ');
            default -> new ReferenceIDT();
        };
    }
}

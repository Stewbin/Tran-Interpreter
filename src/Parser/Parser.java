package Parser;

import AST.*;
import Lexer.SyntaxErrorException;
import Lexer.Token;

import java.util.*;

public class Parser {
    private final TranNode tranNode;
    private final TokenManager tokenManager;

    public Parser(TranNode top, List<Token> tokens) {
        tranNode = top;
        tokenManager = new TokenManager(tokens);
    }

    // Tran = { Class | Interface }
    public void Tran() throws SyntaxErrorException {
        while (!tokenManager.done()) {
            var token = tokenManager.peek(0);
            // At end of stream
            if (token.isEmpty())
                break;
            // Class
            if (token.get().getType() == Token.TokenTypes.CLASS)
                parseClass().ifPresent(tranNode.Classes::add);
            // Interface
            else if (token.get().getType() == Token.TokenTypes.INTERFACE)
                parseInterface().ifPresent(tranNode.Interfaces::add);

            consumeExcessiveNewLines(); // Newlines between classes/interfaces
        }
    }

    // Interface = "interface" Word { MethodHeader }
    private Optional<InterfaceNode> parseInterface() throws SyntaxErrorException {
        var interfaceNode = new InterfaceNode();
        // "interface" Keyword
        if (tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE).isEmpty())
            return Optional.empty();

        // Name
        interfaceNode.name = tokenManager.matchAndRemove(Token.TokenTypes.WORD)
                .orElseThrow(() -> new SyntaxErrorException(
                        "Interfaces must have a name",
                        tokenManager.getCurrentLine(),
                        tokenManager.getCurrentColumnNumber()))
                .getValue();

        // Newline
        requireNewLine();

        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            throw new SyntaxErrorException("Indent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // Method headers
        do {
            var method = parseMethodHeader();
            if (method.isEmpty()) break;
            interfaceNode.methods.add(method.get());

            // Require newlines, if not at End Of File
            requireNewLine();
        } while (true);

        // Dedent
        if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        return Optional.of(interfaceNode);
    }

    // MethodHeader = WORD "(" VariableDeclarations ")" [":" VariableDeclaration {"," VariableDeclaration}]
    private Optional<MethodHeaderNode> parseMethodHeader() throws SyntaxErrorException {

        // This was called on something that's not a MethodHeader
        if (!tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN))
            return Optional.empty();

        var methodHeaderNode = new MethodHeaderNode();
        // Name
        tokenManager.matchAndRemove(Token.TokenTypes.WORD)
                .map(Token::getValue)
                .ifPresent(word -> methodHeaderNode.name = word);
        // Left Paren
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty())
            throw new SyntaxErrorException("Lparen expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // VariableDeclarations
        parseVariableDeclarations().ifPresent(params -> methodHeaderNode.parameters = params);

        // Right Paren
        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty())
            throw new SyntaxErrorException("Rparen expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // Colon
        if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent())  {
            // Return types
            do {
                // Add returnDeclaration to return types list
                methodHeaderNode.returns.add(
                        parseVariableDeclaration()
                                .orElseThrow(() -> new SyntaxErrorException("Must specify at least one return type after Colon", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()))
                );
            } while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()); // In case of multiple returns, look for comma
        }

        return Optional.of(methodHeaderNode);
    }

    // VariableDeclaration = WORD WORD {"," WORD}
    private Optional<VariableDeclarationNode> parseVariableDeclaration() {
        // Must be two WORD tokens next to each other
        if (!tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD))
            return Optional.empty();

        var varDeclareNode = new VariableDeclarationNode();
        // Type
        tokenManager.matchAndRemove(Token.TokenTypes.WORD).ifPresent(typeToken -> varDeclareNode.type = typeToken.getValue());
        // Name(s)
        tokenManager.matchAndRemove(Token.TokenTypes.WORD).ifPresent(nameToken -> varDeclareNode.name = nameToken.getValue());

        return Optional.of(varDeclareNode);
    }

    // VariableDeclarations = [VariableDeclaration] | Identifier { "," Identifier }
    private Optional<List<VariableDeclarationNode>> parseVariableDeclarations() throws SyntaxErrorException {
        List<VariableDeclarationNode> variableDeclarations = new LinkedList<>();

        // [ VariableDeclaration ]
        var firstParameter = parseVariableDeclaration();
        if (firstParameter.isEmpty()) return Optional.empty(); // 0 declarations
        variableDeclarations.add(firstParameter.get()); // 1 declaration

        // > 1 Parameters need to be separated by Commas
        while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            variableDeclarations.add(parseVariableDeclaration()
                            .orElseThrow(() -> new SyntaxErrorException("Parameter expected after Comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())));
        }
        return Optional.of(variableDeclarations);
    }

    private void requireNewLine() throws SyntaxErrorException {
        boolean foundNewLine = false;

        while (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
            foundNewLine = true;
        }

        if (!foundNewLine) {
            // If at EOF, Dedents don't need to be preceded with Newlines
            if (!tokenManager.isOnlyDedentsLeft())
                throw new SyntaxErrorException("Newline Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
    }

    // Class  = "class" WORD ["implements" WORD {"," WORD}] NEWLINE INDENT {(Constructor NEWLINE) | (MethodDeclaration NEWLINE) | (Member NEWLINE)} DEDENT
    private Optional<ClassNode> parseClass() throws SyntaxErrorException {
        var classNode = new ClassNode();
        // Class
        if (tokenManager.matchAndRemove(Token.TokenTypes.CLASS).isEmpty())
            return Optional.empty();

        // Name
        classNode.name = tokenManager.matchAndRemove(Token.TokenTypes.WORD)
                .orElseThrow(() -> new SyntaxErrorException("Class must have name", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()))
                .getValue();

        // "Implements"
        if (tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()) {
            // Interfaces
            // Check for at least one interface name
            classNode.interfaces.add(
                tokenManager.matchAndRemove(Token.TokenTypes.WORD)
                    .orElseThrow(() -> new SyntaxErrorException(
                            "At least one name of interface must be specified after 'implements'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()))
                    .getValue()
            );

            // > 1 interfaces specified must have comma
            while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                var interfaceName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
                if (interfaceName.isEmpty()) {
                    throw new SyntaxErrorException("Interface name must be specified after COMMA", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                } else {
                    classNode.interfaces.add(interfaceName.get().getValue());
                }
            }
        }
        // Newline
        requireNewLine();

        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            var nextToke = tokenManager.peek(0).map(Token::getType);
            // Case: Empty class
            if (nextToke.isEmpty() || nextToke.get() == Token.TokenTypes.CLASS || nextToke.get() == Token.TokenTypes.INTERFACE)
                return Optional.of(classNode);
            throw new SyntaxErrorException("Indent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        // Parse class-body
        // {(Constructor NEWLINE) | (MethodDeclaration NEWLINE) | (Member NEWLINE)}
        do {
            consumeExcessiveNewLines();

            // Constructors
            var constructor = parseConstructor();
            if (constructor.isPresent()) {
                classNode.constructors.add(constructor.get());
                // Newline needed only if body was not found
                if (constructor.get().statements.isEmpty())
                    requireNewLine();
            }
            // Fields
            var field = parseField();
            if (field.isPresent()) {
                classNode.members.add(field.get());
                // Newline needed only if body was not found
                if (field.get().mutator.isEmpty() && field.get().accessor.isEmpty())
                    requireNewLine();
            }
            // Methods
            var method = parseMethodDeclaration();
            if (method.isPresent()) {
                classNode.methods.add(method.get());
                // Newline needed only if body was not found
                if (method.get().statements.isEmpty())
                    requireNewLine();
            }

            if (tokenManager.done())
                throw new SyntaxErrorException("Dedent expected at end of class", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        } while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()); // Dedent


        return Optional.of(classNode);
    }

    private void consumeExcessiveNewLines() {
        while (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent())
            ;
    }

    // Field (officially: Member) = VariableDeclaration NEWLINE ["accessor" ":" Statements] ["mutator" ":" Statements]
    private Optional<MemberNode> parseField() throws SyntaxErrorException {
        var fieldNode = new MemberNode();

        // VariableDeclaration is field name and type e.g. "number listSize"
        var nameAndType = parseVariableDeclaration();
        if (nameAndType.isEmpty())
            return Optional.empty();
        fieldNode.declaration = nameAndType.get();

        // If accessors/mutators present, they need to be in their own block
        // NewLine, Indent
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            // Consume Newline & Indent
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);

            consumeExcessiveNewLines(); // At the accessor/mutator level of indentation

            // 0 or 1 Accessors
            if (tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isEmpty())
                    throw new SyntaxErrorException("Colon expected after Accessor keyword", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                // Newline
                requireNewLine();
                // Add statement block
                fieldNode.accessor = parseStatementBlock();
            }

            consumeExcessiveNewLines(); // At the accessor/mutator level of indentation

            // 0 or 1 Mutators
            if (tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isEmpty())
                    throw new SyntaxErrorException("Colon expected after Mutator keyword", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                // Newline
                requireNewLine();
                // Add statement block
                fieldNode.mutator = parseStatementBlock();
            }

            consumeExcessiveNewLines(); // At the accessor/mutator level of indentation

            // In case 'accessor:' was put after 'mutator:'
            if (tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent()) {
                if (fieldNode.accessor.isPresent())
                    throw new SyntaxErrorException("Cannot have more than one accessor", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isEmpty())
                    throw new SyntaxErrorException("Colon expected after 'accessor' keyword", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                // Newline
                requireNewLine();
                // Add statement block
                fieldNode.accessor = parseStatementBlock();
            }

            consumeExcessiveNewLines(); // At the accessor/mutator level of indentation

            // Dedent
            if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
                throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        return Optional.of(fieldNode);
    }

    // MethodDeclaration = ["private"] ["shared"] MethodHeader NEWLINE MethodBody
    private Optional<MethodDeclarationNode> parseMethodDeclaration() throws SyntaxErrorException {
        var methodNode = new MethodDeclarationNode();

        // Private
        methodNode.isPrivate = tokenManager.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent();
        // Shared
        methodNode.isShared = tokenManager.matchAndRemove(Token.TokenTypes.SHARED).isPresent();

        // Parse a method header
        var maybeMethodHeader = parseMethodHeader();
        if (maybeMethodHeader.isEmpty()) {
            // If "shared" or "private" found, but header missing -> throw SyntaxError
            if (methodNode.isShared || methodNode.isPrivate)
                throw new SyntaxErrorException("MethodHeader Expected after keyword Shared/Private", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            // Probably not a method
            else
                return Optional.empty();
        }
        // Copy over all fields of methodHeader to methodNode
        var methodHeader = maybeMethodHeader.get();
        methodNode.name = methodHeader.name;
        methodNode.parameters = methodHeader.parameters;
        methodNode.returns = methodHeader.returns;

        // Check if empty body
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            // Newline
            requireNewLine();
            // MethodBody
            parseMethodBody(methodNode.locals, methodNode.statements);
        }

        return Optional.of(methodNode);
    }

    // Constructor = "construct" "(" VariableDeclarations ")" NEWLINE MethodBody
    private Optional<ConstructorNode> parseConstructor() throws SyntaxErrorException {
        var constructorNode = new ConstructorNode();

        // Construct
        if (tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isEmpty())
            return Optional.empty();
        // Left paren
        tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).orElseThrow(() -> new SyntaxErrorException("Lparen Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        // VariableDeclarations
        parseVariableDeclarations().ifPresent(params -> constructorNode.parameters = params);
        // Right paren
        tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).orElseThrow(() -> new SyntaxErrorException("Rparen Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        // Newline
        requireNewLine();
        // MethodBody
        parseMethodBody(constructorNode.locals, constructorNode.statements);

        return Optional.of(constructorNode);
    }

    // MethodBody = INDENT { VariableDeclaration NEWLINE } { Statement NEWLINE } DEDENT
    private void parseMethodBody(List<VariableDeclarationNode> locals, List<StatementNode> statements) throws SyntaxErrorException {
        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            return;

        // { VariableDeclaration | Statement NEWLINE }
        while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {

            // Local VariableDeclaration
            var variable = parseVariableDeclaration();
            if (variable.isPresent()) {
                locals.add(variable.get());
                requireNewLine(); // Newline
            }

            // Statement
            var statement = parseStatement();
            if (statement.isPresent()) {
                statements.add(statement.get());
                requireNewLine(); // Newline
            }

            consumeExcessiveNewLines();

            // If no Dedent found
            if (tokenManager.done())
                throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
    }

    // Statement = If | Loop | Assignment | MethodCall
    private Optional<? extends StatementNode> parseStatement() throws SyntaxErrorException {
        Optional<? extends StatementNode> retval;
        // If
        retval = parseIfStatement();
        if (retval.isPresent()) return retval;
        // Loop
        retval = parseLoopStatement();
        if (retval.isPresent()) return retval;
        // Neither Loop nor If
        retval = disambiguateStatements(); // btwn Assignment or MethodCall
        return retval;
    }

    private Optional<? extends StatementNode> disambiguateStatements() throws SyntaxErrorException {
        // Handle void method calls, e.g. "myMethod()\n"
        var maybeMethodCallExp = parseMethodCallExpression();
        if (maybeMethodCallExp.isPresent())
            return Optional.of(new MethodCallStatementNode(maybeMethodCallExp.get()));

        // Check if it's a multi-assignment MethodCall e.g. "x, y, z = myMethod()\n"
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.COMMA))
            return parseMethodCallStatement();

        // If not, then it may or may not be an Assignment
        return parseAssignment(); // parseAssignment() already handles if it's not a valid statement
    }

    // Loop = "loop" [ VariableReference "=" ] BoolExpTerm NEWLINE Statements
    private Optional<LoopNode> parseLoopStatement() throws SyntaxErrorException {
        var loopNode = new LoopNode();

        // "loop" keyword
        if (tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isEmpty()) return Optional.empty();

        // Optional assignment to a variable of type 'loop'
        // e.g. loop temp = x.times() \n\t{...}
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)) {
            // VariableReference
            loopNode.assignment = parseVariableReference();
            // Consume "="
            tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
        } else {
            loopNode.assignment = Optional.empty(); // LoopNode's `assignment` field will be null otherwise
        }

        // BoolExpTerm
        loopNode.expression = parseBoolExpTerm().orElseThrow(() -> new SyntaxErrorException("Boolean Expression Expected after 'loop'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        // Optional Statement-Body
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            // Consume Newline
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            // Body (Statements)
            parseStatementBlock().ifPresent(statements -> loopNode.statements = statements);
//                .orElseThrow(() -> new Lexer.SyntaxErrorException("Body expected in Loop-statement", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        }

        return Optional.of(loopNode);
    }

    // "if" BoolExp NEWLINE Statements ["else" If | (NEWLINE Statements)]
    private Optional<IfNode> parseIfStatement() throws SyntaxErrorException {
        var ifNode = new IfNode();

        // "if"
        if (tokenManager.matchAndRemove(Token.TokenTypes.IF).isEmpty()) return Optional.empty();
        // BoolExp
        ifNode.condition = parseBoolExpTerm().orElseThrow(() -> new SyntaxErrorException("Boolean expression expected after 'if'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        // Optional Statement-Body
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            // Consume Newline
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            // Body (Statements)
            parseStatementBlock().ifPresent(statements -> ifNode.statements = statements);
        }

        if (tokenManager.matchAndRemove(Token.TokenTypes.ELSE).isPresent()) {
            // Make elseNode
            var elseNode = new ElseNode();
            elseNode.statements = new LinkedList<>();
            // Else-If branch
            parseIfStatement().ifPresent(elseNode.statements::add);
            // Else branch
            // Only add if no Else-If branch already
            if (elseNode.statements.isEmpty()) {
                // Newline
                requireNewLine();
                // Statements
                parseStatementBlock().ifPresent(statements -> elseNode.statements = statements);
            }
            // Add elseNode to ifNode
            ifNode.elseStatement = Optional.of(elseNode);
        } else {
            ifNode.elseStatement = Optional.empty();
        }

        return Optional.of(ifNode);
    }

    // StatementBlock (officially: Statements) = INDENT { Statement NEWLINE } DEDENT
    private Optional<List<StatementNode>> parseStatementBlock() throws SyntaxErrorException {
        List<StatementNode> statements = new LinkedList<>();
        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            return Optional.empty();

        // While DEDENT not found
        while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            consumeExcessiveNewLines(); // Statement-level Newlines are unhandled anywhere else here

            var statement = parseStatement();
            if (statement.isPresent()) {
                // Add statement to list
                statements.add(statement.get());

                // Newline needed only if statements have no body
                if (statement.get() instanceof IfNode ifNode) {
                    if (ifNode.statements.isEmpty()) requireNewLine();
                } else if (statement.get() instanceof LoopNode loopNode) {
                    if (loopNode.statements.isEmpty()) requireNewLine();
                // Only statements w/ bodies are If and Loop
                } else {
                    requireNewLine();
                }
            }

            // If at end of tokens, and still no Dedent
            if (tokenManager.done())
                throw new SyntaxErrorException("Dedent Expected at end of Statement Block", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        return Optional.of(statements);
    }

    // BoolExpTerm = BoolExpFactor {("and"|"or") BoolExpFactor}
    private Optional<? extends ExpressionNode> parseBoolExpTerm() throws SyntaxErrorException {
        // L = BoolExpFactor()
        var left = parseUnaryBoolTerm();
        if (left.isEmpty())
            return Optional.empty();

        // Get operator
        var operator = parseBoolOperator(); // "and" | "or"
        // While (next = "and" or "or")...
        while (operator.isPresent()) {
            // R = BoolExpFactor()
            var right = parseUnaryBoolTerm();
            if (right.isEmpty())
                    throw new SyntaxErrorException("BoolTerm expected after operator", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

            // Make new operation node, and copy over everything
            var boolTerm = new BooleanOpNode();
            boolTerm.left = left.get();
            boolTerm.op = operator.get();
            boolTerm.right = right.get();

            // L = this new node
            left = Optional.of(boolTerm); // Wrap in optional before sending

            // Get next operator
            operator = parseBoolOperator();
        }

        return left;
    }

    private Optional<BooleanOpNode.BooleanOperations> parseBoolOperator() {
        return tokenManager.matchAndRemove(Token.TokenTypes.AND)
                .or(() -> tokenManager.matchAndRemove(Token.TokenTypes.OR))
                .map(t -> switch (t.getType()) {
                    case Token.TokenTypes.AND -> BooleanOpNode.BooleanOperations.and;
                    case Token.TokenTypes.OR -> BooleanOpNode.BooleanOperations.or;
                    default -> null;
                });
    }

    // UnaryBoolFactor (Unofficial) = BoolExpFactor | ( "not" UnaryBoolFactor )
    // Needed to make negation higher priority than "and"/"or", but lower priority than comparisons
    private Optional<? extends ExpressionNode> parseUnaryBoolTerm() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.NOT).isPresent()) {
            var notOpNode = new NotOpNode();
            notOpNode.left = parseUnaryBoolTerm().orElseThrow(
                    () -> new SyntaxErrorException("BoolFactor expected after 'not'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
            );
            return Optional.of(notOpNode);
        }
        // Base case: No "not"'s
        return parseBoolExpFactor();
    }

    // BoolExpFactor = MethodCallExpression | Comparison | VariableReference | ( "(" BoolExpTerm ")" )
    private Optional<? extends ExpressionNode> parseBoolExpFactor() throws SyntaxErrorException {
        // "(" BoolExpTerm ")"//
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) { // Lparen "("
            // BoolExpTerm
            var boolExpTerm = parseBoolExpTerm();
            // Rparen ")"
            if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty())
                throw new SyntaxErrorException("Rparen expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

            if (boolExpTerm.isPresent()) return boolExpTerm;
        }
        // Method Call
        var methodCall = parseMethodCallExpression();
        if (methodCall.isPresent()) return methodCall;
        // Comparison
        var comparison = parseComparison();
        if (comparison.isPresent()) return comparison;
        // Variable Reference
        return parseVariableReference();
        // If not BoolExp at all, returns Optional.empty()
    }

    // Comparison (Unofficial) = (Expression ( "==" | "!=" | "<=" | ">=" | ">" | "<" ) Expression)
    private Optional<? extends ExpressionNode> parseComparison() throws SyntaxErrorException {
        var comparison = new CompareNode();
        // Left Expression
        var lexp = parseExpression();
        if (lexp.isEmpty()) // Tokens are not a Comparison
            return Optional.empty();

        // Operators
        if (tokenManager.matchAndRemove(Token.TokenTypes.EQUAL).isPresent()) {
            comparison.op = CompareNode.CompareOperations.eq;
        } else if (tokenManager.matchAndRemove(Token.TokenTypes.NOTEQUAL).isPresent()) {
            comparison.op = CompareNode.CompareOperations.ne;
        } else if (tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL).isPresent()) {
            comparison.op = CompareNode.CompareOperations.ge;
        } else if (tokenManager.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL).isPresent()) {
            comparison.op = CompareNode.CompareOperations.le;
        } else if (tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHAN).isPresent()) {
            comparison.op = CompareNode.CompareOperations.gt;
        } else if (tokenManager.matchAndRemove(Token.TokenTypes.LESSTHAN).isPresent()) {
            comparison.op = CompareNode.CompareOperations.lt;
        } else {
            // If no operator found
            return lexp; // Return whatever lexp is
        }
        // Only after operator found, are we sure this is a Comparison
        comparison.left = lexp.get();

        // Right Expression
        var rexp = parseExpression();
        if (rexp.isEmpty())
            throw new SyntaxErrorException("Right hand side of Comparison expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        comparison.right = rexp.get();

        return Optional.of(comparison);
    }

    // VariableReference = Identifier
    private Optional<VariableReferenceNode> parseVariableReference() {
        // Identifier = WORD
        var identifier = tokenManager.matchAndRemove(Token.TokenTypes.WORD).map(Token::getValue);
        if (identifier.isPresent()) {
            var variable = new VariableReferenceNode();
            variable.name = identifier.get();
            return Optional.of(variable);
        }
        return Optional.empty();
    }

    // Assignment = VariableReference "=" Expression
    private Optional<AssignmentNode> parseAssignment() throws SyntaxErrorException {
        var assignmentNode = new AssignmentNode();

        // If no (WORD "="), this is not an Assignment
        if (!tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN))
            return Optional.empty();

        // VariableReference
        parseVariableReference().ifPresent(variable -> assignmentNode.target = variable);
        // "=" operator
        tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
        // Expression
        assignmentNode.expression = parseExpression().orElseThrow(
                () -> new SyntaxErrorException("Expression expected after assignment", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
        );

        return Optional.of(assignmentNode);
    }

    // MethodCall = [VariableReference { "," VariableReference } "="] MethodCallExpression
    private Optional<MethodCallStatementNode> parseMethodCallStatement() throws SyntaxErrorException {
        // Store return targets
        List<VariableReferenceNode> returnTargets = new LinkedList<>();

        // [ VariableReference ]
        var firstReference = parseVariableReference();
        if (firstReference.isPresent()) {
            // Add first target reference
            returnTargets.add(firstReference.get());

            // Any more refs must be preceded by comma: {"," VariableReference}
            while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                returnTargets.add(
                        parseVariableReference().orElseThrow(
                                () -> new SyntaxErrorException("Assignment target expected after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
                        )
                );
            }
            // "="
            if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty())
                throw new SyntaxErrorException("Assignment operator expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        // MethodCallExpression
        var mcExpression = parseMethodCallExpression();

        if (mcExpression.isEmpty()) {
            // No components of a MethodCall at all means this was not a MethodCall
            if (returnTargets.isEmpty())
                return Optional.empty();
            // Missing MethodCall part of statement
            else
                throw new SyntaxErrorException("MethodCallExpression expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        // Create MethodCall statement
        var mcStatementNode = new MethodCallStatementNode(mcExpression.get());
        // Add return targets
        mcStatementNode.returnValues = returnTargets;
        return Optional.of(mcStatementNode);
    }

    // MethodCallExpression = [Identifier "."] Identifier "(" [Expression {"," Expression }] ")"
    private Optional<MethodCallExpressionNode> parseMethodCallExpression() throws SyntaxErrorException {
        var methodCallExp = new MethodCallExpressionNode(); // Make node
        methodCallExp.objectName = Optional.empty(); // Fix: objectName is originally null in class implementation

        // Optional caller object e.g. "myObj.method();"
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) {
            methodCallExp.objectName = tokenManager.matchAndRemove(Token.TokenTypes.WORD).map(Token::getValue);
            tokenManager.matchAndRemove(Token.TokenTypes.DOT); // Consume '.'
        }

        // Method name
        if (!tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
            // Method was not found, and object not found => not a method
            if (methodCallExp.objectName.isEmpty())
                return Optional.empty();
            // Object found, but no method
            else
                throw new SyntaxErrorException("MethodCallExp expected after '.'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        tokenManager.matchAndRemove(Token.TokenTypes.WORD).map(Token::getValue).ifPresent(name -> methodCallExp.methodName = name); // Add name

        // "("
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty())
            throw new SyntaxErrorException("LPAREN expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        // Optional arguments
        var firstArgument = parseExpression();
        if (firstArgument.isPresent()) {
            // Add first argument
            methodCallExp.parameters.add(firstArgument.get());

            // Any more refs must be preceded by comma
            // {"," VariableReference}
            while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                methodCallExp.parameters.add(
                        parseExpression().orElseThrow(
                                () -> new SyntaxErrorException("Argument expected after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
                        )
                );
            }
        }
        // ")"
        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty())
            throw new SyntaxErrorException("RPAREN expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        return Optional.of(methodCallExp);
    }

    // Expression = Term { ("+"|"-") Term }
    private Optional<? extends ExpressionNode> parseExpression() throws SyntaxErrorException {
        var left = parseTerm();
        // Not an Expression
        if (left.isEmpty())
            return Optional.empty();

        // While operator = + | -
        var operator = parseExpOperator();
        while (operator.isPresent()) {
            // R = Term()
            var right = parseTerm().orElseThrow(
                    () -> new SyntaxErrorException("Term expected after operator", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
            );
            // Make new MathOpNode with current operation & copy over everything
            var newOpNode = new MathOpNode();
            newOpNode.left = left.get();
            newOpNode.op = operator.get();
            newOpNode.right = right;
            // Left = this new node
            left = Optional.of(newOpNode);
            // Get next operator
            operator = parseExpOperator();
        }
        return left;
    }

    // Term = Factor { ("*"|"/"|"%") Factor }
    private Optional<? extends ExpressionNode> parseTerm() throws SyntaxErrorException {
        var left = parseFactor();
        // Not a Term
        if (left.isEmpty())
            return Optional.empty();

        // While operator = * | / | %
        var operator = parseTermOperator();
        while (operator.isPresent()) {
            // R = Factor()
            var right = parseFactor().orElseThrow(
                    () -> new SyntaxErrorException("Factor expected after operator", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
            );
            // Make new MathOpNode with current operation & copy over everything
            var newOpNode = new MathOpNode();
            newOpNode.left = left.get();
            newOpNode.op = operator.get();
            newOpNode.right = right;
            // L = this new node
            left = Optional.of(newOpNode);
            // Get next operator
            operator = parseTermOperator();
        }
        return left;
    }

    private Optional<MathOpNode.MathOperations> parseExpOperator() {
            if (tokenManager.matchAndRemove(Token.TokenTypes.PLUS).isPresent())
                return Optional.of(MathOpNode.MathOperations.add);
            if (tokenManager.matchAndRemove(Token.TokenTypes.MINUS).isPresent())
                return Optional.of(MathOpNode.MathOperations.subtract);
            return Optional.empty();
    }

    private Optional<MathOpNode.MathOperations> parseTermOperator() {
        if (tokenManager.matchAndRemove(Token.TokenTypes.TIMES).isPresent())
            return Optional.of(MathOpNode.MathOperations.multiply);
        if (tokenManager.matchAndRemove(Token.TokenTypes.DIVIDE).isPresent())
            return Optional.of(MathOpNode.MathOperations.divide);
        if (tokenManager.matchAndRemove(Token.TokenTypes.MODULO).isPresent())
            return Optional.of(MathOpNode.MathOperations.modulo);

        return Optional.empty();
    }

    // Factor = NumberLiteral | "true" | "false" | StringLiteral | CharacterLiteral | MethodCallExpression
    // | "(" Expression ")" | Instantiation | VariableReference
    private Optional<? extends ExpressionNode> parseFactor() throws SyntaxErrorException {
        Optional<? extends ExpressionNode> retVal;
        // NumberLiteral
        retVal = parseNumberLiteral();
        if (retVal.isPresent()) return retVal;
        // "true'
        if (tokenManager.matchAndRemove(Token.TokenTypes.TRUE).isPresent())
            return Optional.of(new BooleanLiteralNode(true));
        // "false"
        if (tokenManager.matchAndRemove(Token.TokenTypes.FALSE).isPresent())
            return Optional.of(new BooleanLiteralNode(false));
        // StringLiteral
        retVal = parseStringLiteral();
        if (retVal.isPresent()) return retVal;
        // CharacterLiteral
        retVal = parseCharLiteral();
        if (retVal.isPresent()) return retVal;
        // MethodCallExpression
        retVal = parseMethodCallExpression();
        if (retVal.isPresent()) return retVal;
        // "(" Expression ")" //
        // Lparen
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            // Expression
            retVal = parseExpression();
            // Rparen
            if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty())
                throw new SyntaxErrorException("RPAREN expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        if (retVal.isPresent()) return retVal;
        // (Object) Instantiation
        retVal = parseInstantiation();
        if (retVal.isPresent()) return retVal;
        // VariableReference
        retVal = parseVariableReference();
        return retVal; // If empty, return the empty
    }

    // NumberLiteral = { 0-9 } [. { 0-9 }]
    private Optional<NumericLiteralNode> parseNumberLiteral() {
        var numLit = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER).map(Token::getValue);
        if (numLit.isEmpty())
            return Optional.empty();
        var numberLiteralNode = new NumericLiteralNode();
        numberLiteralNode.value = Float.parseFloat(numLit.get());
        return Optional.of(numberLiteralNode);
    }

    // StringLiteral = " { any non-" } "
    private Optional<StringLiteralNode> parseStringLiteral() {
        var strLit = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING).map(Token::getValue);
        if (strLit.isEmpty())
            return Optional.empty();
        var stringLiteralNode = new StringLiteralNode();
        stringLiteralNode.value = strLit.get();
        return Optional.of(stringLiteralNode);
    }

    // CharacterLiteral = ' (one character not a ') '
    private Optional<CharLiteralNode> parseCharLiteral() {
        var charLit = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER).map(Token::getValue);
        if (charLit.isEmpty())
            return Optional.empty();
        var charLiteralNode = new CharLiteralNode();
        charLiteralNode.value = charLit.get().charAt(0);
        return Optional.of(charLiteralNode);
    }

    // Instantiation (Unofficial) = "new" Identifier "(" [Expression {"," Expression }] ")"
    private Optional<NewNode> parseInstantiation() throws SyntaxErrorException {
        var instantiationNode = new NewNode();
        // "new"
        if (tokenManager.matchAndRemove(Token.TokenTypes.NEW).isEmpty())
            return Optional.empty(); // Not an Instantiation expression
        // Class name
        instantiationNode.className = tokenManager.matchAndRemove(Token.TokenTypes.WORD)
                .map(Token::getValue)
                .orElseThrow(
                () -> new SyntaxErrorException("Class constructor expected after new", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
        );

        // "("
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty())
            throw new SyntaxErrorException("LPAREN expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // Optional arguments
        var firstArgument = parseExpression();
        if (firstArgument.isPresent()) {
            // Add first argument
            instantiationNode.parameters.add(firstArgument.get());

            // Any more refs must be preceded by comma: {"," VariableReference}
            while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                instantiationNode.parameters.add(
                        parseExpression().orElseThrow(
                                () -> new SyntaxErrorException("Argument expected after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
                        )
                );
            }
        }

        // ")"
        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty())
            throw new SyntaxErrorException("RPAREN expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        return Optional.of(instantiationNode);
    }
} // EOC
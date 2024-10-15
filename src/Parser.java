import AST.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                Class().ifPresent(tranNode.Classes::add);
            // Interface
            else if (token.get().getType() == Token.TokenTypes.INTERFACE)
                Interface().ifPresent(tranNode.Interfaces::add);
            // Consume Empty-Space between classes & interfaces
//            else
//                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }
    }

    // Interface = "interface" Word { MethodHeader }
    private Optional<InterfaceNode> Interface() throws SyntaxErrorException {
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
            var method = MethodHeader();
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
    private Optional<MethodHeaderNode> MethodHeader() throws SyntaxErrorException {

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
        VariableDeclarations().ifPresent(params -> methodHeaderNode.parameters = params);

        // Right Paren
        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty())
            throw new SyntaxErrorException("Rparen expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // Colon
        if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent())  {
            // Return types
            do {
                // Add returnDeclaration to return types list
                methodHeaderNode.returns.add(
                        VariableDeclaration()
                                .orElseThrow(() -> new SyntaxErrorException("Must specify at least one return type after Colon", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()))
                );
            } while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()); // In case of multiple returns, look for comma
        }

        return Optional.of(methodHeaderNode);
    }

    // VariableDeclaration = WORD WORD {"," WORD}
    private Optional<VariableDeclarationNode> VariableDeclaration() {
        // Must be two WORD tokens next to each other
        if (!tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD))
            return Optional.empty();

        var varDeclareNode = new VariableDeclarationNode();
        // Type
        tokenManager.matchAndRemove(Token.TokenTypes.WORD).ifPresent(typeToken -> varDeclareNode.type = typeToken.getValue());
        // Name
        tokenManager.matchAndRemove(Token.TokenTypes.WORD).ifPresent(nameToken -> varDeclareNode.name = nameToken.getValue());

        return Optional.of(varDeclareNode);
    }

    // VariableDeclarations = [VariableDeclaration] | VariableDeclaration { "," VariableDeclaration }
    private Optional<List<VariableDeclarationNode>> VariableDeclarations() throws SyntaxErrorException {
        List<VariableDeclarationNode> variableDeclarations = new ArrayList<>();

        // [ VariableDeclaration ]
        var firstParameter = VariableDeclaration();
        if (firstParameter.isEmpty()) return Optional.empty(); // 0 declarations
        variableDeclarations.add(firstParameter.get()); // 1 declaration

        // > 1 Parameters need to be separated by Commas
        while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            variableDeclarations.add(
                    VariableDeclaration()
                            .orElseThrow(() -> new SyntaxErrorException("Parameter expected after Comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()))
            );
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
    private Optional<ClassNode> Class() throws SyntaxErrorException {
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
                            "At least one name of interface must be specified after \"implements\"", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()))
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
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            throw new SyntaxErrorException("Indent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // Class body
        // {(Constructor NEWLINE) | (MethodDeclaration NEWLINE) | (Member NEWLINE)}
        do {
            // Constructors
            var constructor = Constructor();
            if (constructor.isPresent()) {
                classNode.constructors.add(constructor.get());
                // Newline
                requireNewLine();
            }
            // Fields
            var field = Field();
            if (field.isPresent()) {
                classNode.members.add(field.get());
                // Newline
                requireNewLine();
            }
            // Methods
            var method = MethodDeclaration();
            if (method.isPresent()) {
                classNode.methods.add(method.get());
                // Newline
                requireNewLine();
            }

            if (tokenManager.done())
                throw new SyntaxErrorException("Dedent expected at end of class", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        } while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()); // Dedent


        return Optional.of(classNode);
    }

    // Field (officially: Member) = VariableDeclaration NEWLINE ["accessor" ":" Statements] ["mutator" ":" Statements]
    private Optional<MemberNode> Field() throws SyntaxErrorException {
        var fieldNode = new MemberNode();

        // Declaration
        var maybeDeclaration = VariableDeclaration();
        if (maybeDeclaration.isEmpty())
            return Optional.empty();
        fieldNode.declaration = maybeDeclaration.get();

        // If accessors/mutators present, they need to be in their own block
        // NewLine, Indent
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            // Consume Newline & Indent
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);

            // 0 or 1 Accessors
            if (tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isEmpty())
                    throw new SyntaxErrorException("Colon expected after Accessor keyword", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                // Newline
                requireNewLine();
                // Add statement block
                fieldNode.accessor = StatementBlock();
            }

            // 0 or 1 Mutators
            if (tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isEmpty())
                    throw new SyntaxErrorException("Colon expected after Mutator keyword", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                // Newline
                requireNewLine();
                // Add statement block
                fieldNode.mutator = StatementBlock();
            }

            // Dedent
            if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
                throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        return Optional.of(fieldNode);
    }

    // MethodDeclaration = ["private"] ["shared"] MethodHeader NEWLINE MethodBody
    private Optional<MethodDeclarationNode> MethodDeclaration() throws SyntaxErrorException {
        var methodNode = new MethodDeclarationNode();

        // Private
        methodNode.isPrivate = tokenManager.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent();
        // Shared
        methodNode.isShared = tokenManager.matchAndRemove(Token.TokenTypes.SHARED).isPresent();

        // Parse a method header
        var maybeMethodHeader = MethodHeader();
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
            MethodBody(methodNode.locals, methodNode.statements);
        }

        return Optional.of(methodNode);
    }

    // Constructor = "construct" "(" VariableDeclarations ")" NEWLINE MethodBody
    private Optional<ConstructorNode> Constructor() throws SyntaxErrorException {
        var constructorNode = new ConstructorNode();

        // Construct
        if (tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isEmpty())
            return Optional.empty();

        // Left paren
        tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).orElseThrow(() -> new SyntaxErrorException("Lparen Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        // VariableDeclarations
        VariableDeclarations().ifPresent(params -> constructorNode.parameters = params);
        // Right paren
        tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).orElseThrow(() -> new SyntaxErrorException("Rparen Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        // Newline
        requireNewLine();
        // MethodBody
        MethodBody(constructorNode.locals, constructorNode.statements);

        return Optional.of(constructorNode);
    }

    // MethodBody = INDENT { VariableDeclaration NEWLINE } { Statement NEWLINE } DEDENT
    private void MethodBody(List<VariableDeclarationNode> locals, List<StatementNode> statements) throws SyntaxErrorException {
        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            return;

        // { VariableDeclaration | Statement NEWLINE }
        while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {

            // VariableDeclaration
            var variable = VariableDeclaration();
            if (variable.isPresent()) {
                locals.add(variable.get());
                requireNewLine(); // Newline
            }

            // Statement
            var statement = Statement();
            if (statement.isPresent()) {
                statements.add(statement.get());
                requireNewLine(); // Newline
            }

            // If no Dedent found
            if (tokenManager.done())
                throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
    }

    // Statement = If | Loop | Assignment | MethodCall
    private Optional<? extends StatementNode> Statement() throws SyntaxErrorException {
        Optional<? extends StatementNode> retval;
        // If
        retval = IfStatement();
        if (retval.isPresent()) return retval;
        // Loop
        retval = LoopStatement();
        if (retval.isPresent()) return retval;
        // Assignment
        retval = Assignment();
        if (retval.isPresent()) return retval;

        // Unknown statement type == Not a statement?
        return Optional.empty();
    }

    // Loop = [VariableReference "=" ] "loop" BoolExpTerm NEWLINE Statements

    private Optional<LoopNode> LoopStatement() throws SyntaxErrorException {
        var loopNode = new LoopNode();

        // 0 or 1 Assignments
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)) {
            // Variable Name
            var referenceName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if (referenceName.isPresent()) {
                var variableReference = new VariableReferenceNode();
                variableReference.name = referenceName.get().getValue();
                loopNode.assignment = Optional.of(variableReference);
            }
            // Consume "="
            tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
        }

        // "loop"
        if (tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isEmpty()) return Optional.empty();
        // BoolExpTerm
        loopNode.expression = BoolExpTerm().orElseThrow(() -> new SyntaxErrorException("Boolean Expression Expected after \"loop\"", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        // Optional Statement-Body
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            // Consume Newline
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            // Body (Statements)
            StatementBlock().ifPresent(statements -> loopNode.statements = statements);
//                .orElseThrow(() -> new SyntaxErrorException("Body expected in Loop-statement", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        }

        return Optional.of(loopNode);
    }
    // "if" BoolExp NEWLINE Statements ["else" NEWLINE (Statement | Statements)]

    private Optional<IfNode> IfStatement() throws SyntaxErrorException {
        var ifNode = new IfNode();

        // "if"
        if (tokenManager.matchAndRemove(Token.TokenTypes.IF).isEmpty()) return Optional.empty();
        // BoolExp
        ifNode.condition = BoolExpTerm().orElseThrow(() -> new SyntaxErrorException("Boolean expression expected after \"if\"", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        // Optional Statement-Body
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            // Consume Newline
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            // Body (Statements)
            ifNode.statements = StatementBlock().orElse(null);
//                .orElseThrow(() -> new SyntaxErrorException("Body expected in if-statement", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        }

        // Else-Statement
        if (tokenManager.matchAndRemove(Token.TokenTypes.ELSE).isPresent()) {
            var elseNode = new ElseNode(); // Make elseNode
            // Newline
            requireNewLine();
            // Statement(s)
            elseNode.statements = StatementBlock().orElse(null);
//                    .orElseThrow(() -> new SyntaxErrorException("Body expected in else-statement", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            ifNode.elseStatement = Optional.of(elseNode); // Add elseNode to ifNode
        }

        return Optional.of(ifNode);
    }
    // StatementBlock (officially: Statements) = INDENT { Statement NEWLINE } DEDENT

    private Optional<List<StatementNode>> StatementBlock() throws SyntaxErrorException {
        List<StatementNode> statements = new ArrayList<>();
        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            return Optional.empty();

        // While DEDENT not found
        while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            // Add statement to list
            Statement().ifPresent(statements::add);
            // Newline
            requireNewLine();

            // If at end of tokens, and still no Dedent
            if (tokenManager.done())
                throw new SyntaxErrorException("Dedent Expected at end of Statement Block", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        return Optional.of(statements);
    }

    // BoolExpTerm = BoolExpFactor {("and"|"or") BoolExpTerm} | "not" BoolExpTerm
    private Optional<BooleanOpNode> BoolExpTerm() throws SyntaxErrorException {
        var boolOpNode = new BooleanOpNode();
        // Unary Operator Case//

        // "not"
        if (tokenManager.matchAndRemove(Token.TokenTypes.NOT).isPresent()) {
            // BoolExpTerm
            boolOpNode.right = BoolExpTerm().orElseThrow(
                    () -> new SyntaxErrorException("BoolTerm expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
            );
            return Optional.of(boolOpNode);
        }

        // Binary Operator Case //

        // L = BoolExpFactor()
        var boolFactor = BoolExpFactor();
        if (boolFactor.isEmpty()) return Optional.empty();
        boolOpNode.left = boolFactor.get();

        // While (next = "and" or "or")

        // get operator
        var op = tokenManager.matchAndRemove(Token.TokenTypes.AND)
                .or(() -> tokenManager.matchAndRemove(Token.TokenTypes.OR))
                .map(Token::getType);
        while (op.isPresent()) {
            // "and" | "or"
            if (op.get() == Token.TokenTypes.AND)
                boolOpNode.op = BooleanOpNode.BooleanOperations.and;
            else
                boolOpNode.op = BooleanOpNode.BooleanOperations.or;

            // R = BoolExpTerm()
            boolOpNode.right = BoolExpTerm().orElseThrow(
                    () -> new SyntaxErrorException("BoolTerm expected after operator", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber())
            );
            // Make new operation node, and copy over everything
            var boolOpCopy = new BooleanOpNode();
            boolOpCopy.left = boolOpNode.left;
            boolOpCopy.op = boolOpNode.op;
            boolOpCopy.right = boolOpNode.right;
            // L = this new node
            boolOpNode.left = boolOpCopy;
        }

        return Optional.of(boolOpNode);
    }

    // BoolExpFactor = MethodCallExpression | Compare | VariableReference
    private Optional<? extends ExpressionNode> BoolExpFactor() throws SyntaxErrorException {
        // Method Call
        var methodCall = MethodCall();
        if (methodCall.isPresent()) {return methodCall;}
        // Comparison
        var comparison = Comparison();
        if (comparison.isPresent()) {return comparison;}
        // Variable Reference
        var reference = VariableReference();
        if (reference.isPresent()) {return reference;}

        return Optional.empty();
    }

    // Comparison (Unofficial) = (Expression ( "==" | "!=" | "<=" | ">=" | ">" | "<" ) Expression)
    private Optional<CompareNode> Comparison() throws SyntaxErrorException {
        var comparison = new CompareNode();
        // Left Expression
        var lexp = Expression();
        if (lexp.isEmpty()) return Optional.empty(); // Tokens are not a Comparison

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
            return Optional.empty();
        }
        // Only after operator found, are we sure this is a Comparison
        comparison.left = lexp.get();

        // Right Expression
        var rexp = Expression();
        if (rexp.isEmpty())
            throw new SyntaxErrorException("Right hand side of Comparison expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        comparison.right = rexp.get();

        return Optional.of(comparison);
    }

    // Variable Reference = Identifier
    private Optional<VariableReferenceNode> VariableReference() {
        // Identifier == WORD
        var identifier = tokenManager.matchAndRemove(Token.TokenTypes.WORD).map(Token::getValue);
        if (identifier.isPresent()) {
            var variable = new VariableReferenceNode();
            variable.name = identifier.get();
            return Optional.of(variable);
        }
        return Optional.empty();
    }

    // Assignment = VariableReference "=" Expression
    private Optional<AssignmentNode> Assignment() {
        return Optional.empty();
    }

    // Expression = ???
    private Optional<? extends ExpressionNode> Expression() {
        return Optional.empty();
    }

    private Optional<MethodCallExpressionNode> MethodCall() {
        return Optional.empty();
    }

} // EOC
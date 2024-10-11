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
                parseClass().ifPresent(tranNode.Classes::add);
            // Interface
            else if (token.get().getType() == Token.TokenTypes.INTERFACE)
                parseInterface().ifPresent(tranNode.Interfaces::add);
            // Consume Empty-Space between classes & interfaces
            else
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
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

    // MethodHeader = WORD "(" [VariableDeclaration {"," VariableDeclaration}] ")" [":" VariableDeclaration {"," VariableDeclaration}]
    private Optional<MethodHeaderNode> parseMethodHeader() throws SyntaxErrorException {
        var methodHeaderNode = new MethodHeaderNode();
        // Name
        var nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isEmpty())
            return Optional.empty();
        methodHeaderNode.name = nameToken.get().getValue();

        // Left Paren
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty())
            throw new SyntaxErrorException("Lparen expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());


        // Add 1st parameter, if there
        var firstParameter = parseVariableDeclaration();
        firstParameter.ifPresent(methodHeaderNode.parameters::add);

        // 2 or more parameters, look for comma
        while (firstParameter.isPresent() && tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            var parameterDeclaration = parseVariableDeclaration();
            if (parameterDeclaration.isEmpty())
                throw new SyntaxErrorException("Parameter expected after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            methodHeaderNode.parameters.add(parameterDeclaration.get());
        }

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
        // Name
        tokenManager.matchAndRemove(Token.TokenTypes.WORD).ifPresent(nameToken -> varDeclareNode.name = nameToken.getValue());

        return Optional.of(varDeclareNode);
    }


    private void requireNewLine() throws SyntaxErrorException {
        // If at EOF, Dedents don't need to be preceded with Newlines
        if (tokenManager.isOnlyDedentsLeft()) return;

        boolean foundNewLine = false;
        while (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent())
            foundNewLine = true;

        if (!foundNewLine) throw new SyntaxErrorException("Newline Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    // Class  = "class" WORD ["implements" WORD {"," WORD}] NEWLINE INDENT { (Constructor|MethodDeclaration|Member) NEWLINE } DEDENT
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

        // TODO: Refactor parsing members
        Optional<? extends Node> maybeMember;
        // Parse members
        boolean memberFound = false;
        do {
            // Constructors
            if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.CONSTRUCT, Token.TokenTypes.LPAREN)) {
                parseConstructor().ifPresent(classNode.constructors::add);
                memberFound = true;
            // Fields
            } else if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
                parseField().ifPresent(classNode.members::add);
                memberFound = true;
            // Methods
            } else {
                var maybeMethod = parseMethodDeclaration();
                if (maybeMethod.isPresent()) {
                    classNode.methods.add(maybeMethod.get());
                    memberFound = true;
                } else
                    memberFound = false;
            }
            // If not at EOF, require Newline
            requireNewLine();
        } while (memberFound);

        // Dedent
        if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        return Optional.of(classNode);
    }

    // Field (officially: Member) = VariableDeclaration NEWLINE ["accessor" ":" Statements] ["mutator" ":" Statements]
    private Optional<MemberNode> parseField() throws SyntaxErrorException {
        var fieldNode = new MemberNode();

        // Declaration
        var maybeDeclaration = parseVariableDeclaration();
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
                fieldNode.accessor = parseStatementBlock();
            }

            // 0 or 1 Mutators
            if (tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isEmpty())
                    throw new SyntaxErrorException("Colon expected after Mutator keyword", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                // Newline
                requireNewLine();
                // Add statement block
                fieldNode.mutator = parseStatementBlock();
            }

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
        if ((methodNode.isShared || methodNode.isPrivate)) {
            if (maybeMethodHeader.isEmpty())
                throw new SyntaxErrorException("MethodHeader Expected after keyword Shared/Private", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            else
                return Optional.empty();
        }

        // Copy over all fields of methodHeader to methodNode
        if (maybeMethodHeader.isEmpty()) return Optional.empty();
        var methodHeader = maybeMethodHeader.get();
        methodNode.name = methodHeader.name;
        // TODO: Refactor the Declaration {"," Declaration} loop somehow
        methodNode.parameters = methodHeader.parameters;

        // Newline
        requireNewLine();
        // MethodBody
        parseMethodBody(methodNode.locals, methodNode.statements);

        return Optional.of(methodNode);
    }

    // Constructor = "construct" "(" [VariableDeclaration] | VariableDeclaration {"," VariableDeclaration} ")" NEWLINE MethodBody
    private Optional<ConstructorNode> parseConstructor() throws SyntaxErrorException {
        var constructorNode = new ConstructorNode();

        // Construct
        if (tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isEmpty())
            return Optional.empty();

        // Left paren
        tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).orElseThrow(() -> new SyntaxErrorException("Lparen Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        // 0 or 1 Parameter
        var firstParameter = parseVariableDeclaration();
        firstParameter.ifPresent(constructorNode.parameters::add);

        // > 1 Parameters, separated by Commas
        while (firstParameter.isPresent() && tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            parseVariableDeclaration().orElseThrow(() -> new SyntaxErrorException("Parameter expected after Comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        }

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
            throw new SyntaxErrorException("Indent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // { VariableDeclaration NEWLINE }
        var variableDeclaration = parseVariableDeclaration();
        while (variableDeclaration.isPresent()) {
            locals.add(variableDeclaration.get());
            requireNewLine();
            variableDeclaration = parseVariableDeclaration();
        }

        // {Statement NEWLINE}
        var statement = parseStatement();
        while (statement.isPresent()) {
            statements.add(statement.get());
            requireNewLine();
        }

        // Dedent
        if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    private Optional<? extends StatementNode> parseStatement() throws SyntaxErrorException {
        Optional<? extends StatementNode> retval;
        // If
        retval = parseIfStatement();
        if (retval.isPresent()) return retval;
        // Loop
        retval = parseLoopStatement();
        if (retval.isPresent()) return retval;
        // Assignment
        retval = parseAssignment();
        if (retval.isPresent()) return retval;
        // Empty statement
        if (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) return Optional.empty();

        throw new SyntaxErrorException("Unknown statement type", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    // Assignment = VariableReference "=" Expression
    private Optional<AssignmentNode> parseAssignment() {
        return Optional.empty();
    }

    // Loop = [VariableReference "=" ] "loop" BoolExpTerm NEWLINE Statements
    private Optional<LoopNode> parseLoopStatement() throws SyntaxErrorException {

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
        loopNode.expression = parseBoolExpTerm().orElseThrow(() -> new SyntaxErrorException("Boolean Expression Expected after \"loop\"", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        // Newline
        requireNewLine();
        // Body (Statements)
        loopNode.statements = parseStatementBlock().orElse(null);
//                .orElseThrow(() -> new SyntaxErrorException("Body expected in Loop-statement", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        return Optional.of(loopNode);
    }

    // "if" BoolExp NEWLINE Statements ["else" NEWLINE (Statement | Statements)]
    private Optional<IfNode> parseIfStatement() throws SyntaxErrorException {
        var ifNode = new IfNode();

        // "if"
        if (tokenManager.matchAndRemove(Token.TokenTypes.IF).isEmpty()) return Optional.empty();
        // BoolExp
        ifNode.condition = parseBoolExpTerm().orElseThrow(() -> new SyntaxErrorException("Boolean expression expected after \"if\"", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        // Newline
        requireNewLine();
        // Statements
        ifNode.statements = parseStatementBlock().orElse(null);
//                .orElseThrow(() -> new SyntaxErrorException("Body expected in if-statement", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        // Else-Statement
        if (tokenManager.matchAndRemove(Token.TokenTypes.ELSE).isPresent()) {
            var elseNode = new ElseNode(); // Make elseNode
            // Newline
            requireNewLine();
            // Statement(s)
            elseNode.statements = parseStatementBlock().orElse(null);
//                    .orElseThrow(() -> new SyntaxErrorException("Body expected in else-statement", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            ifNode.elseStatement = Optional.of(elseNode); // Add elseNode to ifNode
        }

        return Optional.of(ifNode);
    }

    // StatementBlock (officially: Statements) = INDENT {Statement NEWLINE } DEDENT
    private Optional<List<StatementNode>> parseStatementBlock() throws SyntaxErrorException {
        List<StatementNode> statements = new ArrayList<>();
        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            return Optional.empty();
//            throw new SyntaxErrorException("Indent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // While Statements are being found
        var maybeStatement = parseStatement();
        while (maybeStatement.isPresent()) {
            // Add statement to list
            statements.add(maybeStatement.get());
            // Newline
            requireNewLine();
            // Parse next statement
            maybeStatement = parseStatement();
        }

        // Dedent
        if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        return Optional.of(statements);
    }



    private Optional<BooleanOpNode> parseBoolExpTerm() {
        return Optional.of(new BooleanOpNode());
    }

} // EOC
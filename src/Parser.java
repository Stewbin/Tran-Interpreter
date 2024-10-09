import AST.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
            if (token.get().getType() == Token.TokenTypes.CLASS) {
                parseClass().ifPresent(tranNode.Classes::add);
            // Interface
            } else if (token.get().getType() == Token.TokenTypes.INTERFACE) {
                parseInterface().ifPresent(tranNode.Interfaces::add);
            }
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
            if (tokenManager.peek(1).isPresent())
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
            throw new SyntaxErrorException("LPAREN Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // Parameters
        // Handle 0 or 1 parameters
        parseVariableDeclaration().ifPresent(methodHeaderNode.parameters::add);
        // 2 or more parameters, look for comma
        while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            var parameterDeclaration = parseVariableDeclaration();
            if (parameterDeclaration.isEmpty())
                throw new SyntaxErrorException("Parameter Expected after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            methodHeaderNode.parameters.add(parameterDeclaration.get());
        }

        // Right Paren
        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty())
            throw new SyntaxErrorException("RPAREN Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // Colon
        if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent())  {
            // Return types
            do {
                var returnDeclaration = parseVariableDeclaration();
                if (returnDeclaration.isEmpty()) {
                    throw new SyntaxErrorException(
                            "Must specify at least one return type after Colon", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()
                    );
                } else {
                    // Add returnDeclaration to return types list
                    methodHeaderNode.returns.add(returnDeclaration.get());
                }
            } while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()); // In case of multiple returns, look for comma
        }

        return Optional.of(methodHeaderNode);
    }

    // VariableDeclaration = WORD WORD {"," WORD}
    private Optional<VariableDeclarationNode> parseVariableDeclaration() throws SyntaxErrorException {
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
        boolean foundNewLine = false;
        while (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent())
            foundNewLine = true;

        if (!foundNewLine) throw new SyntaxErrorException("Newline Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    // Class  = "class" WORD ["implements" WORD {"," WORD}] NEWLINE INDENT
    // {(Constructor|MethodDeclaration|Member) NEWLINE} DEDENT
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
        while (tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()) {
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
                classNode.interfaces.add(tokenManager.matchAndRemove(Token.TokenTypes.WORD)
                    .orElseThrow(
                        () -> new SyntaxErrorException("Interface name must be specified after COMMA",tokenManager.getCurrentLine(),tokenManager.getCurrentColumnNumber()))
                    .getValue());
            }
        }

        // Newline
        requireNewLine();

        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            throw new SyntaxErrorException("Indent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // TODO: Refactor parsing members
        // Parse members
        Optional<? extends Node> maybeMember;
        do {
            // Constructors
            maybeMember = parseConstructor();
            if (maybeMember.isPresent()) {
                classNode.constructors.add((ConstructorNode) maybeMember.get());
                if (tokenManager.peek(1).isPresent()) requireNewLine();
            } else {
                // Methods
                maybeMember = parseMethodDeclaration();
                if (maybeMember.isPresent()) {
                    classNode.methods.add((MethodDeclarationNode) maybeMember.get());
                    if (tokenManager.peek(1).isPresent()) requireNewLine();
                } else {
                    // Fields (MemberNodes)
                    maybeMember = parseField();
                    if (maybeMember.isPresent()) {
                        System.out.println("Field found!: " + maybeMember.get());
                        classNode.members.add((MemberNode) maybeMember.get());
                        if (tokenManager.peek(1).isPresent()) requireNewLine();
                    }
                }
            }
        } while (maybeMember.isPresent());

        // Dedent
        if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        return Optional.of(classNode);
    }

    // Field (officially: Member) = VariableDeclaration NEWLINE INDENT ["accessor" ":" Statements DEDENT] ["mutator" ":" Statements DEDENT]
    private Optional<MemberNode> parseField() throws SyntaxErrorException {
        var fieldNode = new MemberNode();

        // Declaration
        var maybeDeclaration = parseVariableDeclaration();
        if (maybeDeclaration.isEmpty())
            return Optional.empty();
        fieldNode.declaration = maybeDeclaration.get();

        // NewLine
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            // Consume Newline & Indent
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);

            // 0 or 1 Accessors
            if (tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
                    fieldNode.accessor = parseStatementBlock();
                } else {
                    throw new SyntaxErrorException("Colon expected after Accessor keyword", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
            }

            // 0 or 1 Mutators
            if (tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
                    fieldNode.mutator = parseStatementBlock();
                } else {
                    throw new SyntaxErrorException("Colon expected after Mutator keyword", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
            }
        }

        return Optional.of(fieldNode);
    }

    // MethodDeclaration = ["private"] ["shared"] MethodHeader NEWLINE MethodBody
    private Optional<MethodDeclarationNode> parseMethodDeclaration() {
        var methodNode = new MethodDeclarationNode();

        return Optional.empty();
//        return Optional.of(methodNode);
    }

    // Constructor = "construct" "(" VariableDeclarations ")" NEWLINE MethodBody
    private Optional<ConstructorNode> parseConstructor() throws SyntaxErrorException {
        var constructorNode = new ConstructorNode();

        return Optional.empty();
//        return Optional.of(constructorNode);
    }

    private Optional<StatementNode> parseStatement() throws SyntaxErrorException {
        return Optional.empty();
    }

    // StatementBlock (officially: Statements) = INDENT {Statement NEWLINE } DEDENT
    private Optional<List<StatementNode>> parseStatementBlock() throws SyntaxErrorException {
        var statements = new ArrayList<StatementNode>();

        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            return Optional.empty();

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

} // EOC
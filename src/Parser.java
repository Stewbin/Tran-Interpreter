import AST.*;

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
        var nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isEmpty())
            throw new SyntaxErrorException("Interfaces must have a name", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        interfaceNode.name = nameToken.get().getValue();

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

    // Class  = "class" WORD ["implements" WORD {, WORD}] NEWLINE INDENT {Statement NEWLINE} DEDENT
    private Optional<ClassNode> parseClass() throws SyntaxErrorException {
        var classNode = new ClassNode();
        // Class
        if (tokenManager.matchAndRemove(Token.TokenTypes.CLASS).isEmpty())
            return Optional.empty();

        // Name
        if (tokenManager.matchAndRemove(Token.TokenTypes.WORD).isEmpty())
            throw new SyntaxErrorException("Class must have name", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // "Implements"
        while (tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()) {
            // Interfaces

            // Check for at least one interface name
            var interfaceName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            // If none, then SyntaxError
            if (interfaceName.isEmpty()) {
                throw new SyntaxErrorException(
                        "At least one name of interface must be specified after \"implements\"",
                        tokenManager.getCurrentLine(),
                        tokenManager.getCurrentColumnNumber()
                );
            }
            // Add interface name to classNode
            classNode.interfaces.add(interfaceName.get().getValue());

            // > 1 interfaces specified must have comma
            while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                interfaceName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
                if (interfaceName.isEmpty()) {
                    throw new SyntaxErrorException(
                            "Interface name must be specified after COMMA",
                            tokenManager.getCurrentLine(),
                            tokenManager.getCurrentColumnNumber()
                    );
                }
                classNode.interfaces.add(interfaceName.get().getValue());
            }
            ;
        }

        // Newline
        requireNewLine();

        // Indent
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            throw new SyntaxErrorException("Indent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        // Parse members
//        do {
//            // Constructors
//            var maybeConstructor = parseConstructor();
//            maybeConstructor.ifPresent(constructorNode -> classNode.constructors.add(constructorNode));
//            // Methods
//
//            // Fields (MemberNodes)
//
//        } while ();

        // Dedent
        if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        return Optional.of(classNode);
    }

    private Optional<MemberNode> parseField() {
        return Optional.empty();
    }

    private Optional<MethodDeclarationNode> parseMethodDeclaration() {
        return Optional.empty();
    }

    private Optional<ConstructorNode> parseConstructor() throws SyntaxErrorException {
        return Optional.empty();
    }

} // EOC
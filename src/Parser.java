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
                // TODO: ClassNode parsing
            // Interface
            } else if (token.get().getType() == Token.TokenTypes.INTERFACE) {
                parseInterface().ifPresent(tranNode.Interfaces::add);
            }
        }
    }

    // Interface = "interface" Word { MethodHeader }
    private Optional<InterfaceNode> parseInterface() throws SyntaxErrorException {
        InterfaceNode interfaceNode = new InterfaceNode();
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
        } while (true);
        // Dedent
        if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Dedent Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());

        return Optional.of(interfaceNode);
    }

    private Optional<MethodHeaderNode> parseMethodHeader() throws SyntaxErrorException {
        var methodHeaderNode = new MethodHeaderNode();

        // Name
        var nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isEmpty()) {
            throw new SyntaxErrorException("Methods must have a name", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        methodHeaderNode.name = nameToken.get().getValue();
        // Left Paren
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty())
            throw new SyntaxErrorException("LPAREN Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        // Parameters
        while (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
            // Add VariableDeclarationNode to parameters list
            parseVariableDeclaration().ifPresent(methodHeaderNode.parameters::add);
        }
        // Right Paren
        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty())
            throw new SyntaxErrorException("RPAREN Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        // Newline
        requireNewLine();

        // Return types
        // If a colon is present
        if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent())  {
            // There must be at least one return declaration
            do {
                var returnDeclaration = parseVariableDeclaration();
                if (returnDeclaration.isPresent()) {
                    // Add returnDeclaration to return types list
                    methodHeaderNode.returns.add(returnDeclaration.get());
                } else {
                    throw new SyntaxErrorException(
                            "Must specify at least one return type after Colon", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()
                    );
                }
            } while (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD));
        }

        return Optional.of(methodHeaderNode);
    }

    private Optional<ConstructorNode> parseConstructor() throws SyntaxErrorException {
        return Optional.empty();
    }

    private Optional<VariableDeclarationNode> parseVariableDeclaration() throws SyntaxErrorException {
        // Must be two WORD tokens next to each other
        if (!tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD))
            return Optional.empty();

        var varDeclareNode = new VariableDeclarationNode();
        // Type
        tokenManager.matchAndRemove(Token.TokenTypes.WORD).ifPresent(typeToken -> varDeclareNode.type = typeToken.getValue());
        // Name
        tokenManager.matchAndRemove(Token.TokenTypes.WORD).ifPresent(nameToken -> varDeclareNode.type = nameToken.getValue());

        return Optional.of(varDeclareNode);
    }


    private void requireNewLine() throws SyntaxErrorException {
        boolean foundNewLine = false;
        while (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent())
            foundNewLine = true;

        if (!foundNewLine) throw new SyntaxErrorException("Newline Expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

} // EOC
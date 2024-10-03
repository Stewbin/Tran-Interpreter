import AST.*;

import java.util.ArrayList;
import java.util.LinkedList;
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
            if (tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE).isPresent()) {
                parseInterface().ifPresent(tranNode.Interfaces::add);
            } else if (tokenManager.matchAndRemove(Token.TokenTypes.CLASS).isPresent()) {
                // TODO: ClassNode parsing
            }
        }
    }

    // Interface = "interface" Word { MethodHeader }
    private Optional<InterfaceNode> parseInterface() throws SyntaxErrorException {
        // It is assumed that parseInterface has only been called after already detecting an INTERFACE Token

        InterfaceNode interfaceNode = new InterfaceNode();
        // Name
        var nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isEmpty())
            throw new SyntaxErrorException("Interfaces must have a name", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        interfaceNode.name = nameToken.get().getValue();
        // Newline
        RequireNewLine();
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


    private void RequireNewLine() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()) {
            throw new SyntaxErrorException("Missing NewLine character", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
    }

} // EOC
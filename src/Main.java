import java.io.IOException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
            return;
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        ToyCLexer sysYLexer = new ToyCLexer(input);

        sysYLexer.removeErrorListeners();
        ErrorListener myErrorListener = new ErrorListener();
        sysYLexer.addErrorListener(myErrorListener);

        List<? extends Token> myTokens = sysYLexer.getAllTokens();

        if (myErrorListener.hasError()) {
            myErrorListener.printLexerErrorInformation();
        } else {
            for (Token t : myTokens) {
                printSysYTokenInformation(t);
            }
        }
    }

    public static void printSysYTokenInformation(Token t) {
        String tokenName = ToyCLexer.VOCABULARY.getSymbolicName(t.getType());
        String tokenText = t.getText();
        int tokenLine = t.getLine();

        if ("INTEGER_CONST".equals(tokenName)) {
            int value = Integer.parseInt(tokenText);

            System.err.println(tokenName + " " + value + " at Line " + tokenLine + ".");
        } else {
            System.err.println(tokenName + " " + tokenText + " at Line " + tokenLine + ".");
        }
    }
}

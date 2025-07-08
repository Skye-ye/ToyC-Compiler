package toyc.frontend.util;

import org.antlr.v4.runtime.*;
import java.util.ArrayList;
import java.util.List;

public class LexerErrorListener extends BaseErrorListener {
    private boolean hasError = false;
    private final List<ErrorInfo> errorList = new ArrayList<>();

    private static class ErrorInfo {
        int line;
        String msg;

        ErrorInfo(int line, String msg) {
            this.line = line;
            this.msg = msg;
        }
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        hasError = true;
        errorList.add(new ErrorInfo(line, msg));
    }

    public boolean hasError() {
        return hasError;
    }

    public void printLexerErrorInformation() {
        for (ErrorInfo error : errorList) {
            System.err.println("Error type A at Line " + error.line + ": " + error.msg);
        }
    }
}
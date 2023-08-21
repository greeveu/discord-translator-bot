package eu.greev.translator.classes.exceptions;

public class TranslationLimitReachedException extends Exception {

    public TranslationLimitReachedException() {
        super();
    }

    public TranslationLimitReachedException(String message) {
        super(message);
    }

    public TranslationLimitReachedException(Throwable cause) {
        super(cause);
    }

    public TranslationLimitReachedException(String message, Throwable cause) {
        super(message, cause);
    }
}
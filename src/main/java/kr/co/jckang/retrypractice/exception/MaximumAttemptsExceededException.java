package kr.co.jckang.retrypractice.exception;

public class MaximumAttemptsExceededException extends RuntimeException {
    public MaximumAttemptsExceededException(String message) {
        super(message);
    }
}

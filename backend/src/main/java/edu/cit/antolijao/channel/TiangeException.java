package edu.cit.antolijao.channel;

class TiangeException extends RuntimeException {
    final String errorCode;
    final int httpStatus;

    TiangeException(String errorCode, int httpStatus, Throwable cause) {
        super("Tiangge error " + errorCode + " (HTTP " + httpStatus + ")", cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
package edu.cit.antolijao.supplier;

class LegacySupplyException extends RuntimeException {
    final String errorCode;
    final int httpStatus;

    LegacySupplyException(String errorCode, int httpStatus, Throwable cause) {
        super("LegacySupply error " + errorCode + " (HTTP " + httpStatus + ")", cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
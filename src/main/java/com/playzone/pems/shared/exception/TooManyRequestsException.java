package com.playzone.pems.shared.exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String mensaje) {
        super(mensaje);
    }
}

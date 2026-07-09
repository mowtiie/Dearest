package com.mowtiie.dearest.security;

public class KeyManagerException extends RuntimeException {
    public KeyManagerException(String message) {
        super(message);
    }

    public KeyManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}

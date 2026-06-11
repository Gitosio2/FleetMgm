package com.fleetmgm.shared.exception;

public class BadCredentialsException extends RuntimeException {

    public BadCredentialsException() {
        super("Invalid credentials");
    }
}

package com.agilesolutions.service_b.exception;

/**
 * Custom exception for database and service unavailability scenarios
 * 
 * Thrown when the database is unreachable or responds with unavailability status
 */
public class ServiceUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ServiceUnavailableException with a message
     * 
     * @param message the detail message
     */
    public ServiceUnavailableException(String message) {
        super(message);
    }

    /**
     * Constructs a ServiceUnavailableException with a message and cause
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a ServiceUnavailableException with a cause
     * 
     * @param cause the cause of the exception
     */
    public ServiceUnavailableException(Throwable cause) {
        super("Service temporarily unavailable", cause);
    }
}


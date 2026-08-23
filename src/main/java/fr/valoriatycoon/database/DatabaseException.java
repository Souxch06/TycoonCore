package fr.valoriatycoon.database;

/** Wraps storage failures crossing an asynchronous service boundary. */
public final class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

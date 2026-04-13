package ru.practicum.moviehub.exception;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> details;

    public ValidationException(String message, List<String> details) {
        super(message);
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}
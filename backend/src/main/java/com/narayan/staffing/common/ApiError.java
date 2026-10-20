package com.narayan.staffing.common;

import java.time.Instant;
import java.util.List;

public record ApiError(
        boolean success,
        String message,
        Instant timestamp,
        String path,
        List<FieldViolation> errors
) {
    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(String message, String path, List<FieldViolation> errors) {
        return new ApiError(false, message, Instant.now(), path, errors == null ? List.of() : errors);
    }
}

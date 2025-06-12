package com.hmdrinks.Config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleInvalidEnumValue(HttpMessageNotReadableException ex) {
        String message = ex.getMessage();

        if (message != null && message.contains("from String")) {
            String enumType = extractEnumType(message);
            String invalidValue = extractInvalidValue(message);
            String allowedValues = extractAllowedValues(message);

            String errorMessage = String.format(
                    "Giá trị '%s' không hợp lệ.  Các giá trị hợp lệ: %s",
                    invalidValue,  allowedValues
            );
            return ResponseEntity.badRequest().body(errorMessage);
        }

        return ResponseEntity.badRequest().body("Dữ liệu JSON không hợp lệ: " + message);
    }

    private String extractEnumType(String message) {
        int start = message.indexOf("`") + 1;
        int end = message.indexOf("`", start);
        return message.substring(start, end);
    }

    private String extractInvalidValue(String message) {
        int idx = message.indexOf("from String \"") + 13;
        return message.substring(idx, message.indexOf("\"", idx));
    }

    private String extractAllowedValues(String message) {
        int start = message.indexOf("[");
        int end = message.indexOf("]", start) + 1;
        return message.substring(start, end);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String error = String.format("Tham số '%s' nhận giá trị '%s' không hợp lệ. Kiểu dữ liệu hợp lệ là %s",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body("Loại nội dung không được hỗ trợ: " + ex.getContentType());
    }

}

package dev.onebrain.api;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> application(ApiException exception, HttpServletResponse response) {
        return error(exception.status(), exception.code(), exception.getMessage(), response);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalidInput(Exception exception, HttpServletResponse response) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Check required fields, supported values, and field lengths.", response);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletResponse response) {
        if (exception instanceof org.springframework.web.ErrorResponse httpError
                && httpError.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.valueOf(httpError.getStatusCode().value());
            return error(status, status == HttpStatus.NOT_FOUND ? "NOT_FOUND" : "INVALID_REQUEST",
                    status == HttpStatus.NOT_FOUND ? "Resource not found." : "The request is not supported.", response);
        }
        // Do not log exception messages: JDBC and validation errors can contain submitted content.
        log.error("Request failed: requestId={}, errorType={}",
                response.getHeader("X-Request-Id"), exception.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed.", response);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                           HttpServletResponse response) {
        return ResponseEntity.status(status)
                .body(new ApiError(code, message, response.getHeader("X-Request-Id")));
    }
}

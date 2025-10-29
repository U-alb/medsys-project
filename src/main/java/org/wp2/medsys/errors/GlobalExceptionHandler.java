package org.wp2.medsys.errors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.wp2.medsys.strategy.BookingConflictException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /* ------------------------ 401 ------------------------ */
    @ExceptionHandler(UnauthorizedException.class)
    public Object handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        log.warn("401 Unauthorized: {}", ex.getMessage());
        return negotiate(req, HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), ex, null);
    }

    /* ------------------------ 403 ------------------------ */
    @ExceptionHandler({ ForbiddenException.class, AccessDeniedException.class })
    public Object handleForbidden(RuntimeException ex, HttpServletRequest req) {
        log.warn("403 Forbidden: {}", ex.getMessage());
        return negotiate(req, HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), ex, null);
    }

    /* ------------------------ 404 ------------------------ */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNotFound(NoResourceFoundException ex, HttpServletRequest req) {
        log.info("404 Not Found: {}", ex.getMessage());
        return negotiate(req, HttpStatus.NOT_FOUND, "Not Found", "Resource not found.", ex, null);
    }

    /* ------------------------ 409 ------------------------ */
    @ExceptionHandler({ ConflictException.class, BookingConflictException.class })
    public Object handleConflict(RuntimeException ex, HttpServletRequest req) {
        log.info("409 Conflict: {}", ex.getMessage());
        return negotiate(req, HttpStatus.CONFLICT, "Conflict", ex.getMessage(), ex, null);
    }

    /* ------------------------ 400 family ------------------------ */
    @ExceptionHandler({
            BadRequestException.class,
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public Object handleBadRequest(Exception ex, HttpServletRequest req) {
        log.info("400 Bad Request: {}", ex.getMessage());
        return negotiate(req, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), ex, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value"),
                        "rejectedValue", maskIfSensitive(fe)))
                .collect(Collectors.toList());
        log.info("400 Validation failed: {}", summarizeFieldErrors(fieldErrors));
        return negotiate(req, HttpStatus.BAD_REQUEST, "Validation failed",
                "Some fields are invalid.", ex, fieldErrors);
    }

    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException ex, HttpServletRequest req) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value"),
                        "rejectedValue", maskIfSensitive(fe)))
                .collect(Collectors.toList());
        log.info("400 Binding failed: {}", summarizeFieldErrors(fieldErrors));
        return negotiate(req, HttpStatus.BAD_REQUEST, "Binding failed",
                "Some fields are invalid.", ex, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        var fieldErrors = ex.getConstraintViolations().stream()
                .map(cv -> Map.of(
                        "field", pathOf(cv),
                        "message", Optional.ofNullable(cv.getMessage()).orElse("Invalid value"),
                        "rejectedValue", safeValue(cv.getInvalidValue())))
                .collect(Collectors.toList());
        log.info("400 Constraint violation: {}", summarizeFieldErrors(fieldErrors));
        return negotiate(req, HttpStatus.BAD_REQUEST, "Validation failed",
                "Some parameters are invalid.", ex, fieldErrors);
    }

    /* ------------------------ 405 / 415 ------------------------ */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        log.info("405 Method Not Allowed: {}", ex.getMessage());
        var headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) headers.setAllow(ex.getSupportedHttpMethods());
        return negotiate(req, HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", ex.getMessage(), ex, null, headers);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        log.info("415 Unsupported Media Type: {}", ex.getMessage());
        return negotiate(req, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", ex.getMessage(), ex, null);
    }

    /* ------------------------ ResponseStatusException passthrough ------------------------ */
    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        var status = HttpStatus.valueOf(ex.getStatusCode().value());
        var reason = Optional.ofNullable(ex.getReason()).orElse(status.getReasonPhrase());
        if (status.is5xxServerError()) {
            log.error("{} {}", status.value(), reason, ex);
        } else {
            log.info("{} {}", status.value(), reason);
        }
        return negotiate(req, status, reason, reason, ex, null);
    }

    /* ------------------------ 500 (catch-all) ------------------------ */
    @ExceptionHandler(Throwable.class)
    public Object handleGeneric(Throwable ex, HttpServletRequest req) {
        if (ex instanceof NoResourceFoundException nrf) {
            return handleNotFound(nrf, req);
        }
        log.error("500 Internal Server Error", ex);
        return negotiate(req, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Something went wrong. Please try again.", ex, null);
    }

    /* ========================================================================== */
    /* Helpers                                                                    */
    /* ========================================================================== */

    private Object negotiate(HttpServletRequest req,
                             HttpStatus status,
                             String error,
                             String message,
                             Throwable ex,
                             List<Map<String, Object>> fieldErrors) {
        return negotiate(req, status, error, message, ex, fieldErrors, null);
    }

    private Object negotiate(HttpServletRequest req,
                             HttpStatus status,
                             String error,
                             String message,
                             Throwable ex,
                             List<Map<String, Object>> fieldErrors,
                             HttpHeaders extraHeaders) {

        boolean debug = isDebug(req);
        String path = req.getRequestURI();
        String requestId = firstNonBlank(
                req.getHeader("X-Request-Id"),
                MDC.get("requestId"),
                MDC.get("reqId"),
                UUID.randomUUID().toString() // last resort
        );
        String traceId = firstNonBlank(
                MDC.get("traceId"),            // Micrometer Tracing (Boot 3)
                MDC.get("X-B3-TraceId"),       // Brave/Zipkin
                MDC.get("trace_id"),
                req.getHeader("X-Trace-Id")
        );

        if (wantsJson(req)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", status.value());
            body.put("error", error != null ? error : status.getReasonPhrase());
            body.put("message", message);
            body.put("path", path);
            body.put("timestamp", OffsetDateTime.now());
            if (requestId != null) body.put("requestId", requestId);
            if (traceId != null) body.put("traceId", traceId);
            if (fieldErrors != null && !fieldErrors.isEmpty()) body.put("fieldErrors", fieldErrors);
            if (debug && ex != null) {
                body.put("exception", ex.getClass().getSimpleName());
                body.put("trace", stackTrace(ex));
            }

            ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
            if (extraHeaders != null) builder.headers(extraHeaders);
            return builder.body(body);
        }

        // HTML error page
        ModelAndView mv = new ModelAndView("error");
        mv.setStatus(status);
        mv.addObject("status", status.value());
        mv.addObject("error", error != null ? error : status.getReasonPhrase());
        mv.addObject("message", message);
        mv.addObject("path", path);
        mv.addObject("timestamp", OffsetDateTime.now());
        mv.addObject("requestId", requestId);
        mv.addObject("traceId", traceId);
        if (debug && ex != null) {
            mv.addObject("exception", ex.getClass().getName());
            mv.addObject("trace", stackTrace(ex));
        }
        return mv;
    }

    private boolean wantsJson(HttpServletRequest req) {
        String accept = Optional.ofNullable(req.getHeader("Accept")).orElse("");
        String xhr = req.getHeader("X-Requested-With");
        String contentType = Optional.ofNullable(req.getHeader("Content-Type")).orElse("");
        String path = Optional.ofNullable(req.getRequestURI()).orElse("");
        String format = req.getParameter("format");

        return (accept.contains("application/json"))
                || (contentType.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(xhr)
                || path.startsWith("/api/")
                || "json".equalsIgnoreCase(format);
    }

    private boolean isDebug(HttpServletRequest req) {
        // Show details if ?debug present (any value) — pairs well with:
        // server.error.include-stacktrace=on_param
        return req.getParameterMap().containsKey("debug");
    }

    private static String stackTrace(Throwable t) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String summarizeFieldErrors(List<Map<String, Object>> list) {
        return list.stream()
                .map(m -> m.get("field") + "=" + m.get("message"))
                .collect(Collectors.joining(", "));
    }

    private static String pathOf(ConstraintViolation<?> cv) {
        var path = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : "";
        return path.isBlank() ? "value" : path;
    }

    private static Object maskIfSensitive(FieldError fe) {
        String field = fe.getField().toLowerCase(Locale.ROOT);
        Object value = fe.getRejectedValue();
        if (field.contains("password") || field.contains("secret") || field.contains("token")) {
            return "***";
        }
        return safeValue(value);
    }

    private static Object safeValue(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v);
        return s.length() > 200 ? s.substring(0, 197) + "..." : s;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}

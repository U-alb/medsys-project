package org.wp2.medsys.errors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.wp2.medsys.strategy.BookingConflictException;

import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /* ------------------------ 401 ------------------------ */
    @ExceptionHandler(UnauthorizedException.class)
    public Object handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        log.warn("401 Unauthorized: {}", ex.getMessage());
        return negotiate(req, HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    /* ------------------------ 403 ------------------------ */
    @ExceptionHandler({ ForbiddenException.class, AccessDeniedException.class })
    public Object handleForbidden(RuntimeException ex, HttpServletRequest req) {
        log.warn("403 Forbidden: {}", ex.getMessage());
        return negotiate(req, HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    /* ------------------------ 404 (missing static/resources) ------------------------ */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNotFound(NoResourceFoundException ex, HttpServletRequest req) {
        log.warn("404 Not Found: {}", ex.getMessage());
        return negotiate(req, HttpStatus.NOT_FOUND, "Not Found", "Resource not found");
    }

    /* ------------------------ 409 ------------------------ */
    @ExceptionHandler({ ConflictException.class, BookingConflictException.class })
    public Object handleConflict(RuntimeException ex, HttpServletRequest req) {
        log.info("409 Conflict: {}", ex.getMessage());
        return negotiate(req, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    /* ------------------------ 400 ------------------------ */
    @ExceptionHandler({
            BadRequestException.class,
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class
    })
    public Object handleBadRequest(Exception ex, HttpServletRequest req) {
        log.info("400 Bad Request: {}", ex.getMessage());
        return negotiate(req, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /* ------------------------ 500 (catch-all) ------------------------ */
    @ExceptionHandler(Throwable.class)
    public Object handleGeneric(Throwable ex, HttpServletRequest req) {
        // Do NOT convert 404s to 500s
        if (ex instanceof NoResourceFoundException nrf) {
            return handleNotFound(nrf, req);
        }
        log.error("500 Internal Server Error", ex);
        return negotiate(req, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Something went wrong. Please try again.");
    }

    /* ========================================================================== */
    /* Helpers: decide HTML vs JSON and build the response                        */
    /* ========================================================================== */
    private Object negotiate(HttpServletRequest req, HttpStatus status, String error, String message) {
        if (wantsJson(req)) {
            return ResponseEntity.status(status)
                    .body(Map.of(
                            "status", status.value(),
                            "error", error,
                            "message", message
                    ));
        }
        return view(status, message);
    }

    private boolean wantsJson(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String xhr = req.getHeader("X-Requested-With");
        return (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(xhr);
    }

    private ModelAndView view(HttpStatus status, String message) {
        ModelAndView mv = new ModelAndView("error");
        mv.setStatus(status);
        mv.addObject("error", message); // templates/error.html should print ${error}
        return mv;
    }
}

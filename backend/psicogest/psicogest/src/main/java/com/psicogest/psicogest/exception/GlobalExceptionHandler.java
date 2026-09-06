package com.psicogest.psicogest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleNotFound(
                        ResourceNotFoundException exception) {

                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 404);
                response.put("error", "Not Found");
                response.put("message", exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(response);
        }

        @ExceptionHandler(EmailAlreadyExistsException.class)
        public ResponseEntity<Map<String, Object>> handleEmailExists(
                        EmailAlreadyExistsException exception) {

                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 409);
                response.put("error", "Conflict");
                response.put("message", exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(
                        MethodArgumentNotValidException exception) {

                Map<String, String> errors = new HashMap<>();

                exception.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> errors.put(
                                                error.getField(),
                                                error.getDefaultMessage()));

                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 400);
                response.put("error", "Validation Error");
                response.put("errors", errors);

                return ResponseEntity
                                .badRequest()
                                .body(response);
        }

        @ExceptionHandler(CnpjAlreadyExistsException.class)
        public ResponseEntity<Map<String, Object>> handleCnpjExists(
                        CnpjAlreadyExistsException exception) {
                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 409);
                response.put("error", "Conflict");
                response.put("message", exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler(MembershipAlreadyExistsException.class)
        public ResponseEntity<Map<String, Object>> handleMembershipExists(
                        MembershipAlreadyExistsException exception) {
                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 409);
                response.put("error", "Conflict");
                response.put("message", exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler(InvalidAvailabilityException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidAvailability(
                        InvalidAvailabilityException exception) {
                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 400);
                response.put("error", "Bad Request");
                response.put("message", exception.getMessage());

                return ResponseEntity
                                .badRequest()
                                .body(response);
        }

        @ExceptionHandler(ScheduleConflictException.class)
        public ResponseEntity<Map<String, Object>> handleScheduleConflict(
                        ScheduleConflictException exception) {

                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 409);
                response.put("error", "Conflict");
                response.put("message", exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler(InvalidAppointmentTransitionException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidAppointmentTransition(
                        InvalidAppointmentTransitionException exception) {

                Map<String, Object> response = new HashMap<>();

                response.put(
                                "timestamp",
                                LocalDateTime.now());

                response.put(
                                "status",
                                HttpStatus.CONFLICT.value());

                response.put(
                                "error",
                                "Conflict");

                response.put(
                                "message",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler(InvalidAppointmentSeriesException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidAppointmentSeries(
                        InvalidAppointmentSeriesException exception) {

                Map<String, Object> response = new HashMap<>();

                response.put(
                                "timestamp",
                                LocalDateTime.now());

                response.put(
                                "status",
                                400);

                response.put(
                                "error",
                                "Bad Request");

                response.put(
                                "message",
                                exception.getMessage());

                return ResponseEntity
                                .badRequest()
                                .body(response);
        }

        @ExceptionHandler({
                        InvalidTherapeuticRelationshipTransitionException.class,
                        TherapeuticRelationshipConflictException.class
        })
        public ResponseEntity<Map<String, Object>> handleTherapeuticRelationshipConflict(
                        RuntimeException exception) {

                Map<String, Object> response = new HashMap<>();

                response.put(
                                "timestamp",
                                LocalDateTime.now());

                response.put(
                                "status",
                                HttpStatus.CONFLICT.value());

                response.put(
                                "error",
                                "Conflict");

                response.put(
                                "message",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler({
                        InvalidClinicMembershipPeriodTransitionException.class,
                        ClinicMembershipPeriodConflictException.class
        })
        public ResponseEntity<Map<String, Object>> handleClinicMembershipPeriodConflict(
                        RuntimeException exception) {

                Map<String, Object> response = new HashMap<>();

                response.put(
                                "timestamp",
                                LocalDateTime.now());

                response.put(
                                "status",
                                HttpStatus.CONFLICT.value());

                response.put(
                                "error",
                                "Conflict");

                response.put(
                                "message",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler(EntityLifecycleException.class)
        public ResponseEntity<Map<String, Object>> handleEntityLifecycle(
                        EntityLifecycleException exception) {

                Map<String, Object> response = new HashMap<>();

                response.put(
                                "timestamp",
                                LocalDateTime.now());

                response.put(
                                "status",
                                HttpStatus.CONFLICT.value());

                response.put(
                                "error",
                                "Conflict");

                response.put(
                                "message",
                                exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }
}
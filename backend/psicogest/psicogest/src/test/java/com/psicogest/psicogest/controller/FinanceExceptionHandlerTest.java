package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.exception.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.stream.Stream;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FinanceExceptionHandlerTest {
    static Stream<Arguments> failures() {
        return Stream.of(Arguments.of(new IdempotencyConflictException("Synthetic replay conflict"),409),
                Arguments.of(new FinanceConflictException("Synthetic balance conflict"),409),
                Arguments.of(new InvalidFinanceTransitionException("Synthetic invalid transition"),409),
                Arguments.of(new FinanceValidationException("Synthetic validation failure"),422));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void returnsAHandledDomainError(RuntimeException exception, int expectedStatus) throws Exception {
        MockMvcBuilders.standaloneSetup(new Endpoint(exception)).setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/synthetic-finance-failure"))
                .andExpect(status().is(expectedStatus)).andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));
    }

    @RestController
    static class Endpoint {
        private final RuntimeException exception;
        Endpoint(RuntimeException exception) { this.exception=exception; }
        @GetMapping("/synthetic-finance-failure") String fail() { throw exception; }
    }
}

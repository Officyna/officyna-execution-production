package br.com.officyna_execution_production.administrative.labor.api.handler;

import br.com.officyna_execution_production.administrative.labor.domain.exception.LaborBusinessException;
import br.com.officyna_execution_production.administrative.labor.domain.exception.LaborNotFoundException;
import br.com.officyna_execution_production.infrastructure.exception.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LaborExceptionHandlerTest {

    private final LaborExceptionHandler handler = new LaborExceptionHandler();

    @Test
    @DisplayName("Deve tratar LaborNotFoundException retornando 404")
    void shouldHandleNotFound() {
        LaborNotFoundException ex = new LaborNotFoundException("Labor not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Labor not found", response.getBody().message());
        assertEquals(404, response.getBody().status());
    }

    @Test
    @DisplayName("Deve tratar LaborBusinessException retornando 400")
    void shouldHandleBusinessException() {
        LaborBusinessException ex = new LaborBusinessException("Invalid labor state");

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid labor state", response.getBody().message());
        assertEquals(400, response.getBody().status());
    }
}

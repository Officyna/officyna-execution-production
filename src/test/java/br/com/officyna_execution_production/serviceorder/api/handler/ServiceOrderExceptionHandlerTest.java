package br.com.officyna_execution_production.serviceorder.api.handler;

import br.com.officyna_execution_production.infrastructure.exception.ErrorResponse;
import br.com.officyna_execution_production.serviceorder.domain.exception.ServiceOrderBusinessException;
import br.com.officyna_execution_production.serviceorder.domain.exception.ServiceOrderNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServiceOrderExceptionHandlerTest {

    private final ServiceOrderExceptionHandler handler = new ServiceOrderExceptionHandler();

    @Test
    @DisplayName("Deve tratar ServiceOrderNotFoundException retornando 404")
    void shouldHandleNotFound() {
        ServiceOrderNotFoundException ex = new ServiceOrderNotFoundException("OS not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("OS not found", response.getBody().message());
        assertEquals(404, response.getBody().status());
    }

    @Test
    @DisplayName("Deve tratar ServiceOrderBusinessException retornando 400")
    void shouldHandleBusinessException() {
        ServiceOrderBusinessException ex = new ServiceOrderBusinessException("Invalid status");

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid status", response.getBody().message());
        assertEquals(400, response.getBody().status());
    }
}

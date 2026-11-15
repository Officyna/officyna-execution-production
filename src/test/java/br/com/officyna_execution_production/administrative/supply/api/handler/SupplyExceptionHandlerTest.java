package br.com.officyna_execution_production.administrative.supply.api.handler;

import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyBusinessException;
import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyNotFoundException;
import br.com.officyna_execution_production.infrastructure.exception.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SupplyExceptionHandlerTest {

    private final SupplyExceptionHandler handler = new SupplyExceptionHandler();

    @Test
    @DisplayName("Deve tratar SupplyNotFoundException retornando 404")
    void shouldHandleNotFound() {
        SupplyNotFoundException ex = new SupplyNotFoundException("Supply not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Supply not found", response.getBody().message());
        assertEquals(404, response.getBody().status());
    }

    @Test
    @DisplayName("Deve tratar SupplyBusinessException retornando 400")
    void shouldHandleBusinessException() {
        SupplyBusinessException ex = new SupplyBusinessException("Insufficient stock");

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Insufficient stock", response.getBody().message());
        assertEquals(400, response.getBody().status());
    }
}

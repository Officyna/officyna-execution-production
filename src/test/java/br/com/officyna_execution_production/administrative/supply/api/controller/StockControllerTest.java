package br.com.officyna_execution_production.administrative.supply.api.controller;

import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;
import br.com.officyna_execution_production.administrative.supply.api.resources.StockMovementRequest;
import br.com.officyna_execution_production.administrative.supply.domain.entity.StockMovement;
import br.com.officyna_execution_production.administrative.supply.domain.service.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockControllerTest {

    @Mock
    private StockService stock;

    @InjectMocks
    private StockController controller;

    @Test
    @DisplayName("Deve reservar suprimentos com status 204 NO_CONTENT")
    void shouldReserve() {
        StockMovementRequest request = new StockMovementRequest(List.of(new StockItemRequest("sup-1", 2)));

        ResponseEntity<Void> response = controller.reserve("os-1", request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(stock).reserveSupplies("os-1", request.supplies());
    }

    @Test
    @DisplayName("Deve consumir suprimentos com status 204 NO_CONTENT")
    void shouldConsume() {
        StockMovementRequest request = new StockMovementRequest(List.of(new StockItemRequest("sup-1", 2)));

        ResponseEntity<Void> response = controller.consume("os-1", request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(stock).consumeSupplies("os-1", request.supplies());
    }

    @Test
    @DisplayName("Deve liberar suprimentos com status 204 NO_CONTENT")
    void shouldRelease() {
        StockMovementRequest request = new StockMovementRequest(List.of(new StockItemRequest("sup-1", 2)));

        ResponseEntity<Void> response = controller.release("os-1", request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(stock).releaseSupplies("os-1", request.supplies());
    }

    @Test
    @DisplayName("Deve consultar histórico de movimentações com sucesso")
    void shouldGetHistory() {
        StockMovement movement = StockMovement.builder()
                .id("mov-1")
                .serviceOrderId("os-1")
                .supplyId("sup-1")
                .operation("RESERVE")
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .build();
        when(stock.history("os-1")).thenReturn(List.of(movement));

        ResponseEntity<List<StockMovement>> response = controller.history("os-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("mov-1", response.getBody().getFirst().getId());
        verify(stock).history("os-1");
    }
}

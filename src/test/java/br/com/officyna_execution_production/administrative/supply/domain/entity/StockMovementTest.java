package br.com.officyna_execution_production.administrative.supply.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StockMovementTest {

    @Test
    @DisplayName("Deve construir e manipular atributos de StockMovement")
    void shouldCreateAndMutateStockMovement() {
        LocalDateTime now = LocalDateTime.now();
        StockMovement movement = StockMovement.builder()
                .id("mov-1")
                .serviceOrderId("os-1")
                .supplyId("sup-1")
                .operation("RESERVE")
                .quantity(10)
                .createdAt(now)
                .build();

        assertEquals("mov-1", movement.getId());
        assertEquals("os-1", movement.getServiceOrderId());
        assertEquals("sup-1", movement.getSupplyId());
        assertEquals("RESERVE", movement.getOperation());
        assertEquals(10, movement.getQuantity());
        assertEquals(now, movement.getCreatedAt());

        movement.setQuantity(5);
        assertEquals(5, movement.getQuantity());

        StockMovement empty = new StockMovement();
        assertNotNull(empty);
    }
}

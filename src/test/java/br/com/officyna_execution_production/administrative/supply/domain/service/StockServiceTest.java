package br.com.officyna_execution_production.administrative.supply.domain.service;

import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;
import br.com.officyna_execution_production.administrative.supply.domain.entity.StockMovement;
import br.com.officyna_execution_production.administrative.supply.domain.entity.Supply;
import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyBusinessException;
import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyNotFoundException;
import br.com.officyna_execution_production.administrative.supply.domain.repository.StockMovementRepository;
import br.com.officyna_execution_production.administrative.supply.domain.repository.SupplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockServiceTest {
    private SupplyRepository supplies;
    private StockMovementRepository movements;
    private StockService service;

    @BeforeEach
    void setUp() {
        supplies = mock(SupplyRepository.class);
        movements = mock(StockMovementRepository.class);
        service = new StockService(supplies, movements);
    }

    @Test
    void reservesSupplyAndRecordsMovement() {
        Supply supply = supply(10, 2);
        when(supplies.findById("s-1")).thenReturn(Optional.of(supply));

        service.reserveSupplies("os-1", List.of(new StockItemRequest("s-1", 3)));

        assertEquals(5, supply.getReservedQuantity());
        verify(supplies).save(supply);
        assertMovement("RESERVA", "os-1", "s-1", 3);
    }

    @Test
    void consumesSupplyAndReleasesReservedQuantity() {
        Supply supply = supply(10, 4);
        when(supplies.findById("s-1")).thenReturn(Optional.of(supply));

        service.consumeSupplies("os-1", List.of(new StockItemRequest("s-1", 3)));

        assertEquals(7, supply.getStockQuantity());
        assertEquals(1, supply.getReservedQuantity());
        assertMovement("CONSUMO", "os-1", "s-1", 3);
    }

    @Test
    void releasesReservationWithoutAllowingNegativeBalance() {
        Supply supply = supply(10, 2);
        when(supplies.findById("s-1")).thenReturn(Optional.of(supply));

        service.releaseSupplies("os-1", List.of(new StockItemRequest("s-1", 5)));

        assertEquals(0, supply.getReservedQuantity());
        assertMovement("LIBERACAO", "os-1", "s-1", 5);
    }

    @Test
    void returnsHistoryFromRepository() {
        List<StockMovement> history = List.of(StockMovement.builder().serviceOrderId("os-1").build());
        when(movements.findByServiceOrderIdOrderByCreatedAtDesc("os-1")).thenReturn(history);

        assertSame(history, service.history("os-1"));
    }

    @Test
    void validatesMovementInput() {
        assertThrows(SupplyBusinessException.class, () -> service.reserveSupplies(" ", List.of()));
        assertDoesNotThrow(() -> service.reserveSupplies("os-1", List.of()));
        assertThrows(SupplyBusinessException.class, () -> service.reserveSupplies("os-1", List.of(new StockItemRequest("", 1))));
        assertThrows(SupplyBusinessException.class, () -> service.reserveSupplies("os-1", List.of(new StockItemRequest("s-1", 0))));
    }

    @Test
    void throwsWhenSupplyDoesNotExist() {
        when(supplies.findById("missing")).thenReturn(Optional.empty());

        assertThrows(SupplyNotFoundException.class,
                () -> service.reserveSupplies("os-1", List.of(new StockItemRequest("missing", 1))));
    }

    @Test
    void rejectsUnknownOperationsAndHandlesLowStockConsumption() {
        Supply supply = supply(3, 1);
        supply.setMinimumQuantity(2);
        when(supplies.findById("s-1")).thenReturn(Optional.of(supply));

        assertThrows(SupplyBusinessException.class,
                () -> service.move("os-1", List.of(new StockItemRequest("s-1", 1)), "AJUSTE"));

        service.consumeSupplies("os-1", List.of(new StockItemRequest("s-1", 2)));
        assertEquals(1, supply.getStockQuantity());
    }

    private Supply supply(int stock, int reserved) {
        return Supply.builder().id("s-1").name("Oil").stockQuantity(stock).reservedQuantity(reserved).build();
    }

    private void assertMovement(String operation, String serviceOrderId, String supplyId, int quantity) {
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movements).save(captor.capture());
        StockMovement movement = captor.getValue();
        assertEquals(operation, movement.getOperation());
        assertEquals(serviceOrderId, movement.getServiceOrderId());
        assertEquals(supplyId, movement.getSupplyId());
        assertEquals(quantity, movement.getQuantity());
        assertNotNull(movement.getCreatedAt());
    }
}

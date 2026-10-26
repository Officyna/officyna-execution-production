package br.com.officyna_execution_production.administrative.supply.domain.service;

import br.com.officyna_execution_production.administrative.supply.domain.entity.Supply;
import br.com.officyna_execution_production.administrative.supply.domain.entity.SupplyType;
import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyBusinessException;
import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyNotFoundException;
import br.com.officyna_execution_production.administrative.supply.domain.repository.SupplyRepository;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupplyServiceTest {
    private SupplyRepository repository;
    private SupplyService service;

    @BeforeEach void setUp() { repository = mock(SupplyRepository.class); service = new SupplyService(repository); }

    @Test void findsActiveSuppliesAndTypes() {
        var supply = supply("s-1", "Oil", 10, 1);
        when(repository.findByActiveTrue()).thenReturn(List.of(supply));
        when(repository.findByActiveTrueAndType(SupplyType.SUPPLY)).thenReturn(List.of(supply));
        assertEquals(List.of(supply), service.findAll());
        assertEquals(List.of(supply), service.findByType(SupplyType.SUPPLY));
    }

    @Test void createsValidSupplyAndRejectsDuplicateOrInvalidStock() {
        var supply = supply("s-1", "Oil", 10, 1);
        when(repository.save(supply)).thenReturn(supply);
        assertSame(supply, service.create(supply));
        when(repository.existsByName("Oil")).thenReturn(true);
        assertThrows(SupplyBusinessException.class, () -> service.create(supply));
        assertThrows(SupplyBusinessException.class, () -> service.create(supply("s-2", "Bad", 1, 2)));
    }

    @Test void updatesDeletesAndFindsSupply() {
        var existing = supply("s-1", "Oil", 10, 1);
        var changes = supply("s-1", "New Oil", 20, 1);
        when(repository.findById("s-1")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        assertSame(existing, service.findById("s-1"));
        assertSame(existing, service.update("s-1", changes));
        assertEquals("New Oil", existing.getName());
        service.delete("s-1");
        assertFalse(existing.getActive());
    }

    @Test void rejectsChangingReservedQuantityOrDuplicateUpdatedNameAndMissingSupply() {
        var existing = supply("s-1", "Oil", 10, 1);
        when(repository.findById("s-1")).thenReturn(Optional.of(existing));
        when(repository.existsByName("Other")).thenReturn(true);
        assertThrows(SupplyBusinessException.class, () -> service.update("s-1", supply("s-1", "Other", 10, 1)));
        when(repository.existsByName("Other")).thenReturn(false);
        assertThrows(SupplyBusinessException.class, () -> service.update("s-1", supply("s-1", "Other", 10, 2)));
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(SupplyNotFoundException.class, () -> service.findEntityById("missing"));
    }

    private Supply supply(String id, String name, int stock, int reserved) {
        return Supply.builder().id(id).name(name).description("d").type(SupplyType.SUPPLY)
                .stockQuantity(stock).minimumQuantity(1).reservedQuantity(reserved).active(true).build();
    }
}

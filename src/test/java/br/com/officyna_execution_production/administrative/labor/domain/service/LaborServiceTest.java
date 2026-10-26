package br.com.officyna_execution_production.administrative.labor.domain.service;

import br.com.officyna_execution_production.administrative.labor.domain.entity.Labor;
import br.com.officyna_execution_production.administrative.labor.domain.exception.LaborBusinessException;
import br.com.officyna_execution_production.administrative.labor.domain.exception.LaborNotFoundException;
import br.com.officyna_execution_production.administrative.labor.domain.repository.LaborRepository;
import br.com.officyna_execution_production.monitoring.domain.service.LaborMonitoringService;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LaborServiceTest {
    private LaborRepository repository; private LaborMonitoringService monitoring; private LaborService service;
    @BeforeEach void setUp() { repository=mock(LaborRepository.class); monitoring=mock(LaborMonitoringService.class); service=new LaborService(repository, monitoring); }
    @Test void findsAndCreatesLaborWithMonitoring() {
        var labor=labor("l-1", "Repair"); when(repository.findByActiveTrue()).thenReturn(List.of(labor)); when(repository.save(labor)).thenReturn(labor);
        assertEquals(List.of(labor), service.findAll()); assertSame(labor, service.create(labor));
        verify(monitoring).initializeFromEstimate("l-1", "Repair", "d", 2);
    }
    @Test void rejectsDuplicateCreationAndUpdatesDeletes() {
        var labor=labor("l-1", "Repair"); when(repository.existsByName("Repair")).thenReturn(true);
        assertThrows(LaborBusinessException.class, () -> service.create(labor));
        when(repository.existsByName("Repair")).thenReturn(false); when(repository.findById("l-1")).thenReturn(Optional.of(labor)); when(repository.save(labor)).thenReturn(labor);
        var changes=labor("l-1", "New Repair"); assertSame(labor, service.update("l-1", changes)); assertEquals("New Repair", labor.getName());
        service.delete("l-1"); assertFalse(labor.getActive());
    }
    @Test void rejectsDuplicateNewNameAndMissingLabor() {
        var labor=labor("l-1", "Repair"); when(repository.findById("l-1")).thenReturn(Optional.of(labor)); when(repository.existsByName("Other")).thenReturn(true);
        assertThrows(LaborBusinessException.class, () -> service.update("l-1", labor("l-1", "Other")));
        when(repository.findById("missing")).thenReturn(Optional.empty()); assertThrows(LaborNotFoundException.class, () -> service.findEntityById("missing"));
    }
    @Test void findsLaborById() {
        var labor=labor("l-1", "Repair"); when(repository.findById("l-1")).thenReturn(Optional.of(labor));
        assertSame(labor, service.findById("l-1"));
    }
    private Labor labor(String id,String name) { return Labor.builder().id(id).name(name).description("d").price(BigDecimal.TEN).executionTimeInDays(2).active(true).build(); }
}

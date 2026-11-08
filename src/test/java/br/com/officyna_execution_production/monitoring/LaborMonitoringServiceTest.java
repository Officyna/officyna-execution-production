package br.com.officyna_execution_production.monitoring;

import br.com.officyna_execution_production.monitoring.domain.service.LaborMonitoringService;
import br.com.officyna_execution_production.monitoring.domain.repository.*;
import br.com.officyna_execution_production.monitoring.domain.entity.*;
import br.com.officyna_execution_production.administrative.labor.domain.repository.LaborRepository;
import br.com.officyna_execution_production.administrative.labor.domain.entity.Labor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LaborMonitoringServiceTest {
    @Test
    void eightHoursIsOneWorkingDayAndDuplicateDoesNotCountTwice() {
        var records = mock(LaborMonitoringRepository.class);
        var catalog = mock(LaborRepository.class);
        var history = mock(LaborExecutionRepository.class);
        var s = new LaborMonitoringService(records, catalog, history);
        when(catalog.findById("l1")).thenReturn(Optional.of(Labor.builder().id("l1").name("Troca").build()));
        when(history.findById(anyString())).thenReturn(Optional.empty(), Optional.of(new LaborExecution()));
        var start = LocalDateTime.of(2026, 10, 12, 8, 0);
        s.registerExecution("os1", "l1", start, start.plusHours(8));
        s.registerExecution("os1", "l1", start, start.plusHours(8));
        var c = org.mockito.ArgumentCaptor.forClass(LaborMonitoring.class);
        verify(records).save(c.capture());
        assertEquals(1.0, c.getValue().getAverageExecutionTimeInDays());
        assertEquals(1, c.getValue().getTotalExecutions());
        verify(history).save(any());
    }

    @Test
    void recalcUsesCompletedHistoryAndIgnoresInvalidDurations() {
        var records = mock(LaborMonitoringRepository.class);
        var catalog = mock(LaborRepository.class);
        var history = mock(LaborExecutionRepository.class);
        var s = new LaborMonitoringService(records, catalog, history);
        when(catalog.findByActiveTrue()).thenReturn(List.of(Labor.builder().id("l1").name("Troca").build()));
        var start = LocalDateTime.of(2026, 10, 12, 8, 0);
        when(history.findAll()).thenReturn(List.of(LaborExecution.builder().laborId("l1").startDate(start).endDate(start.plusHours(8)).build(), LaborExecution.builder().laborId("l1").startDate(start).endDate(start.plusHours(16)).build(), LaborExecution.builder().laborId("l1").startDate(start).endDate(start.minusHours(1)).build()));
        assertEquals(1, s.forceRecalc());
        var c = org.mockito.ArgumentCaptor.forClass(LaborMonitoring.class);
        verify(records).save(c.capture());
        assertEquals(1.5, c.getValue().getAverageExecutionTimeInDays());
        assertEquals(2, c.getValue().getTotalExecutions());
    }

    @Test
    void findsRecordsAndIgnoresIncompleteOrInvalidExecutions() {
        var records = mock(LaborMonitoringRepository.class);
        var catalog = mock(LaborRepository.class);
        var history = mock(LaborExecutionRepository.class);
        var s = new LaborMonitoringService(records, catalog, history);
        var monitoring = LaborMonitoring.builder().laborId("l1").build();
        when(records.findAll()).thenReturn(List.of(monitoring));
        assertEquals(List.of(monitoring), s.findAll());
        s.registerExecution("os", "l1", null, LocalDateTime.now());
        s.registerExecution("os", "l1", LocalDateTime.now(), LocalDateTime.now().minusHours(1));
        verifyNoInteractions(history);
    }

    @Test
    void updatesExistingMonitoringAndInitializesFromEstimate() {
        var records = mock(LaborMonitoringRepository.class);
        var catalog = mock(LaborRepository.class);
        var history = mock(LaborExecutionRepository.class);
        var s = new LaborMonitoringService(records, catalog, history);
        var existing = LaborMonitoring.builder().laborId("l1").averageExecutionTimeInDays(1.0).totalExecutions(2).build();
        when(history.findById(anyString())).thenReturn(Optional.empty());
        when(records.findByLaborId("l1")).thenReturn(Optional.of(existing));
        var start = LocalDateTime.of(2026, 1, 1, 8, 0);
        s.registerExecution("os", "l1", start, start.plusHours(8));
        assertEquals(1.0, existing.getAverageExecutionTimeInDays());
        assertEquals(3, existing.getTotalExecutions());
        s.initializeFromEstimate("l1", "Novo", "desc", 3);
        assertEquals("Novo", existing.getLaborName());
        s.initializeFromEstimate("l2", "Outro", "d", 2);
        verify(records, atLeast(3)).save(any());
    }

    @Test
    void estimateWithNullIsIgnoredAndMissingLaborDoesNotCreateMonitoring() {
        var records = mock(LaborMonitoringRepository.class);
        var catalog = mock(LaborRepository.class);
        var history = mock(LaborExecutionRepository.class);
        var s = new LaborMonitoringService(records, catalog, history);
        s.initializeFromEstimate("l1", "name", "desc", null);
        when(history.findById(anyString())).thenReturn(Optional.empty());
        when(records.findByLaborId("missing")).thenReturn(Optional.empty());
        when(catalog.findById("missing")).thenReturn(Optional.empty());
        var now = LocalDateTime.now();
        s.registerExecution("os", "missing", now, now.plusHours(1));
        verify(records, never()).save(any());
    }

    @Test
    void recalcSkipsLaborsWithoutExecutionsOrWithOnlyInvalidDurations() {
        var records = mock(LaborMonitoringRepository.class);
        var catalog = mock(LaborRepository.class);
        var history = mock(LaborExecutionRepository.class);
        var service = new LaborMonitoringService(records, catalog, history);
        var empty = Labor.builder().id("empty").name("Empty").build();
        var invalid = Labor.builder().id("invalid").name("Invalid").build();
        var start = LocalDateTime.of(2026, 1, 1, 8, 0);
        when(catalog.findByActiveTrue()).thenReturn(List.of(empty, invalid));
        when(history.findAll()).thenReturn(List.of(LaborExecution.builder().laborId("invalid")
                .startDate(start).endDate(start.minusHours(1)).build()));

        assertEquals(0, service.forceRecalc());
        verify(records, never()).save(any());
    }
}

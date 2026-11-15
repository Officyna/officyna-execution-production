package br.com.officyna_execution_production.monitoring.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LaborMonitoringTest {

    @Test
    @DisplayName("Deve construir e manipular atributos de LaborMonitoring e LaborExecution")
    void shouldCreateAndMutateEntities() {
        LaborMonitoring monitoring = LaborMonitoring.builder()
                .id("mon-1")
                .laborId("lab-1")
                .laborName("Freio")
                .laborDescription("Pastilhas")
                .averageExecutionTimeInDays(2.0)
                .totalExecutions(4)
                .build();

        assertEquals("mon-1", monitoring.getId());
        assertEquals("lab-1", monitoring.getLaborId());
        assertEquals("Freio", monitoring.getLaborName());
        assertEquals("Pastilhas", monitoring.getLaborDescription());
        assertEquals(2.0, monitoring.getAverageExecutionTimeInDays());
        assertEquals(4, monitoring.getTotalExecutions());

        monitoring.setTotalExecutions(5);
        assertEquals(5, monitoring.getTotalExecutions());

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);
        LaborExecution execution = LaborExecution.builder()
                .id("exec-1")
                .serviceOrderId("os-1")
                .laborId("lab-1")
                .startDate(start)
                .endDate(end)
                .build();

        assertEquals("exec-1", execution.getId());
        assertEquals("os-1", execution.getServiceOrderId());
        assertEquals("lab-1", execution.getLaborId());
        assertEquals(start, execution.getStartDate());
        assertEquals(end, execution.getEndDate());

        assertNotNull(new LaborMonitoring());
        assertNotNull(new LaborExecution());
    }
}

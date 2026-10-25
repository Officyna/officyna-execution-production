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
 @Test void eightHoursIsOneWorkingDayAndDuplicateDoesNotCountTwice(){
  var records=mock(LaborMonitoringRepository.class);var catalog=mock(LaborRepository.class);var history=mock(LaborExecutionRepository.class);
  var s=new LaborMonitoringService(records,catalog,history);
  when(catalog.findById("l1")).thenReturn(Optional.of(Labor.builder().id("l1").name("Troca").build()));
  when(history.findById(anyString())).thenReturn(Optional.empty(),Optional.of(new LaborExecution()));
  var start=LocalDateTime.of(2026,10,12,8,0);s.registerExecution("os1","l1",start,start.plusHours(8));s.registerExecution("os1","l1",start,start.plusHours(8));
  var c=org.mockito.ArgumentCaptor.forClass(LaborMonitoring.class);verify(records).save(c.capture());assertEquals(1.0,c.getValue().getAverageExecutionTimeInDays());assertEquals(1,c.getValue().getTotalExecutions());verify(history).save(any());
 }
 @Test void recalcUsesCompletedHistoryAndIgnoresInvalidDurations(){
  var records=mock(LaborMonitoringRepository.class);var catalog=mock(LaborRepository.class);var history=mock(LaborExecutionRepository.class);var s=new LaborMonitoringService(records,catalog,history);
  when(catalog.findByActiveTrue()).thenReturn(List.of(Labor.builder().id("l1").name("Troca").build()));
  var start=LocalDateTime.of(2026,10,12,8,0);when(history.findAll()).thenReturn(List.of(LaborExecution.builder().laborId("l1").startDate(start).endDate(start.plusHours(8)).build(),LaborExecution.builder().laborId("l1").startDate(start).endDate(start.plusHours(16)).build(),LaborExecution.builder().laborId("l1").startDate(start).endDate(start.minusHours(1)).build()));
  assertEquals(1,s.forceRecalc());var c=org.mockito.ArgumentCaptor.forClass(LaborMonitoring.class);verify(records).save(c.capture());assertEquals(1.5,c.getValue().getAverageExecutionTimeInDays());assertEquals(2,c.getValue().getTotalExecutions());
 }
}

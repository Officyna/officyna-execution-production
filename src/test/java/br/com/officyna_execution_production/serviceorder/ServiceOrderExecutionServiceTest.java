package br.com.officyna_execution_production.serviceorder;

import br.com.officyna_execution_production.administrative.supply.domain.service.StockService;
import br.com.officyna_execution_production.monitoring.domain.service.LaborMonitoringService;
import br.com.officyna_execution_production.infrastructure.security.AuthenticatedUserDetails;
import br.com.officyna_execution_production.serviceorder.domain.entity.ExecutionWork;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import br.com.officyna_execution_production.serviceorder.domain.exception.ServiceOrderNotFoundException;
import br.com.officyna_execution_production.serviceorder.domain.repository.ExecutionWorkRepository;
import br.com.officyna_execution_production.serviceorder.event.DiagnosisStartedEvent;
import br.com.officyna_execution_production.serviceorder.event.OsSnapshotEvent;
import br.com.officyna_execution_production.serviceorder.event.ServiceOrderIdEvent;
import br.com.officyna_execution_production.serviceorder.service.ExecutionPublisher;
import br.com.officyna_execution_production.serviceorder.service.OsServiceClient;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServiceOrderExecutionServiceTest {
    private ExecutionWorkRepository repository;
    private StockService stockService;
    private ExecutionPublisher publisher;
    private OsServiceClient osServiceClient;
    private ServiceOrderExecutionService service;

    @BeforeEach
    void setUp() {
        repository = mock(ExecutionWorkRepository.class);
        stockService = mock(StockService.class);
        publisher = mock(ExecutionPublisher.class);
        osServiceClient = mock(OsServiceClient.class);
        service = new ServiceOrderExecutionService(repository, stockService, publisher, osServiceClient,
                mock(LaborMonitoringService.class));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void receivesOnlySnapshotFieldsAndPersistsCorrelationId() {
        OsSnapshotEvent event = new OsSnapshotEvent("os-1", "corr-1", ServiceOrderStatus.RECEBIDA);
        service.receiveServiceOrderForDiagnosis(event);
        var captor = org.mockito.ArgumentCaptor.forClass(ExecutionWork.class);
        verify(repository).save(captor.capture());
        assertEquals("corr-1", captor.getValue().getCorrelationId());
        assertEquals(ServiceOrderStatus.RECEBIDA, captor.getValue().getStatus());
    }

    @Test
    void diagnosisEventUsesOnlyItsQueueContractAndStoredCorrelationId() {
        ExecutionWork order = ExecutionWork.builder().id("os-1").serviceOrderId("os-1")
                .status(ServiceOrderStatus.RECEBIDA).correlationId("corr-1").createdAt(LocalDateTime.now()).build();
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(order));

        authenticateMechanic();
        service.startDiagnosis("os-1");

        verify(publisher).publish(eq("eventDiagnosis"),
                eq(new DiagnosisStartedEvent("os-1", ServiceOrderStatus.EM_DIAGNOSTICO, "mechanic-1", "Ana Silva")),
                eq("corr-1"));
    }

    @Test
    void approvalReservesOsServiceSuppliesAndPublishesOnlyServiceOrderId() {
        ExecutionWork order = ExecutionWork.builder().id("os-1").serviceOrderId("os-1")
                .status(ServiceOrderStatus.EM_DIAGNOSTICO).correlationId("corr-1").createdAt(LocalDateTime.now()).build();
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(order));
        when(osServiceClient.getSupplies("os-1", "Bearer token")).thenReturn(java.util.List.of());

        service.submitForApproval("os-1", "Bearer token");

        verify(stockService).reserveSupplies("os-1", java.util.List.of());
        verify(publisher).publish(eq("eventDiagnosisCompleted"), eq(new ServiceOrderIdEvent("os-1")), eq("corr-1"));
    }

    @Test
    void unknownOrderCannotStartDiagnosis() {
        authenticateMechanic();
        assertThrows(ServiceOrderNotFoundException.class,
                () -> service.startDiagnosis("missing"));
        verify(repository, never()).save(any());
    }

    @Test
    void diagnosisRequiresAnAuthenticatedMechanicWithAnId() {
        assertThrows(Exception.class, () -> service.startDiagnosis("os-1"));

        var principal = new AuthenticatedUserDetails("ana@example.com",
                List.of(new SimpleGrantedAuthority("ROLE_MECHANIC")), "", "Ana Silva");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(order("os-1", ServiceOrderStatus.RECEBIDA)));
        assertThrows(Exception.class, () -> service.startDiagnosis("os-1"));
    }

    @Test
    void findAllReturnsOnlyActiveOrdersSortedByStatusPriorityThenCreationDate() {
        var later = order("later", ServiceOrderStatus.EM_EXECUCAO);
        later.setCreatedAt(LocalDateTime.now().plusMinutes(1));
        var earlier = order("earlier", ServiceOrderStatus.EM_EXECUCAO);
        earlier.setCreatedAt(LocalDateTime.now());
        when(repository.findAll()).thenReturn(List.of(order("done", ServiceOrderStatus.FINALIZADA), later, earlier,
                order("approval", ServiceOrderStatus.AGUARDANDO_APROVACAO)));

        assertEquals(List.of("earlier", "later", "approval"), service.findAll().stream()
                .map(ExecutionWork::getServiceOrderId).toList());
    }

    @Test
    void invalidOrDuplicateSnapshotIsNotSaved() {
        assertThrows(Exception.class, () -> service.receiveServiceOrderForDiagnosis(null));
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(order("os-1", ServiceOrderStatus.RECEBIDA)));
        service.receiveServiceOrderForDiagnosis(new OsSnapshotEvent("os-1", "corr", ServiceOrderStatus.RECEBIDA));
        verify(repository, never()).save(any());
    }

    @Test
    void delegatesOrderEditingOperationsToOsService() throws Exception {
        var work = order("os-1", ServiceOrderStatus.EM_DIAGNOSTICO);
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));
        var json = new ObjectMapper().readTree("{\"ok\":true}");
        when(osServiceClient.addLabor(anyString(), anyString(), anyString())).thenReturn(json);
        when(osServiceClient.removeLabor(anyString(), anyString(), anyString())).thenReturn(json);
        when(osServiceClient.addSupply(anyString(), anyString(), anyInt(), anyString())).thenReturn(json);
        when(osServiceClient.removeSupply(anyString(), anyString(), anyString())).thenReturn(json);
        when(osServiceClient.updateInformation(anyString(), anyString(), anyString())).thenReturn(json);

        assertSame(json, service.addLabor("os-1", new br.com.officyna_execution_production.serviceorder.api.request.AddLaborRequest("labor-1"), "token"));
        assertSame(json, service.removeLabor("os-1", "labor-1", "token"));
        assertSame(json, service.addSupply("os-1", new br.com.officyna_execution_production.serviceorder.api.request.AddSupplyRequest("supply-1", 2), "token"));
        assertSame(json, service.removeSupply("os-1", "supply-1", "token"));
        assertSame(json, service.updateInformation("os-1", "note", "token"));
    }

    @Test
    void approvedBillingDecisionChangesStatusAndSavesCorrelation() {
        var work = order("os-1", ServiceOrderStatus.AGUARDANDO_APROVACAO);
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));

        service.receiveBillingDecision(new br.com.officyna_execution_production.serviceorder.event.BillingDecisionEvent("os-1", "APROVADA"), "corr-new");

        assertEquals(ServiceOrderStatus.APROVADA, work.getStatus());
        assertEquals("corr-new", work.getCorrelationId());
        verify(repository).save(work);
    }

    @Test
    void refusedBillingDecisionReleasesReservedStockAndIsIdempotent() {
        var work = order("os-1", ServiceOrderStatus.AGUARDANDO_APROVACAO);
        work.setStockReserved(true);
        work.setStockReservationItems(List.of(new br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest("supply", 1)));
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));

        service.receiveBillingDecision(new br.com.officyna_execution_production.serviceorder.event.BillingDecisionEvent("os-1", "RECUSADA"), "corr");
        service.receiveBillingDecision(new br.com.officyna_execution_production.serviceorder.event.BillingDecisionEvent("os-1", "RECUSADA"), "corr");

        verify(stockService).releaseSupplies("os-1", work.getStockReservationItems());
        assertFalse(work.isStockReserved());
        assertEquals(ServiceOrderStatus.RECUSADA, work.getStatus());
    }

    @Test
    void invalidBillingDecisionsAndInvalidExecutionStatusAreRejected() {
        assertThrows(Exception.class, () -> service.receiveBillingDecision(null, "corr"));
        var work = order("os-1", ServiceOrderStatus.EM_DIAGNOSTICO);
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));
        assertThrows(Exception.class, () -> service.receiveBillingDecision(
                new br.com.officyna_execution_production.serviceorder.event.BillingDecisionEvent("os-1", "PENDENTE"), "corr"));
        assertThrows(Exception.class, () -> service.startServiceOrderExecution("os-1", "token"));
    }

    @Test
    void rejectsBillingDecisionWhenTheOrderIsNotAwaitingApproval() {
        var work = order("os-1", ServiceOrderStatus.EM_DIAGNOSTICO);
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));

        assertThrows(Exception.class, () -> service.receiveBillingDecision(
                new br.com.officyna_execution_production.serviceorder.event.BillingDecisionEvent("os-1", "APROVADA"), "corr"));
    }

    @Test
    void diagnosisRecordsTheDurationOfTheReceivedStatus() {
        var work = order("os-1", ServiceOrderStatus.RECEBIDA);
        work.setRegistrationDate(LocalDateTime.now().minusMinutes(5));
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));
        authenticateMechanic();

        service.startDiagnosis("os-1");

        assertNotNull(work.getDiagnosisStartDate());
    }

    @Test
    void deliveryStatusHasNoLocalStartDate() throws Exception {
        var method = ServiceOrderExecutionService.class.getDeclaredMethod("getStatusStartDate",
                ExecutionWork.class, ServiceOrderStatus.class);
        method.setAccessible(true);

        assertNull(method.invoke(service, order("os-1", ServiceOrderStatus.FINALIZADA), ServiceOrderStatus.ENTREGUE));
    }

    @Test
    void executionConsumesReservedStockAndPublishesStatus() {
        var work = order("os-1", ServiceOrderStatus.APROVADA);
        work.setStockReserved(true);
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));

        service.startServiceOrderExecution("os-1", "token");

        assertEquals(ServiceOrderStatus.EM_EXECUCAO, work.getStatus());
        assertTrue(work.isStockConsumed());
        assertFalse(work.isStockReserved());
        verify(stockService).consumeSupplies(eq("os-1"), anyList());
        verify(publisher).publish(eq("eventExecutionStarted"), any(), eq("corr-1"));
    }

    @Test
    void startLaborStartsOrderOnlyOnFirstLabor() {
        var work = order("os-1", ServiceOrderStatus.APROVADA);
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));
        service.startLabor("os-1", "labor-1", "token");
        assertEquals(ServiceOrderStatus.EM_EXECUCAO, work.getStatus());

        service.startLabor("os-1", "labor-2", "token");
        verify(osServiceClient).startLabor("os-1", "labor-1", "token");
        verify(osServiceClient).startLabor("os-1", "labor-2", "token");
        verify(publisher, times(1)).publish(eq("eventExecutionStarted"), any(), anyString());
    }

    @Test
    void cannotFinishOrderWithOpenLaborAndFinishesWhenAllAreClosed() throws Exception {
        var work = order("os-1", ServiceOrderStatus.EM_EXECUCAO);
        work.setStockReserved(true);
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));
        var mapper = new ObjectMapper();
        when(osServiceClient.findServiceOrder("os-1", "token")).thenReturn(mapper.readTree("{\"labors\":{\"laborsDetails\":[{\"situation\":\"ATIVO\",\"endDate\":\"\"}]}}"));
        assertThrows(Exception.class, () -> service.finishServiceOrderExecution("os-1", "token"));

        when(osServiceClient.findServiceOrder("os-1", "token")).thenReturn(mapper.readTree("{\"labors\":{\"laborsDetails\":[{\"situation\":\"REJEITADO\"}]}}"));
        service.finishServiceOrderExecution("os-1", "token");
        assertEquals(ServiceOrderStatus.FINALIZADA, work.getStatus());
        verify(stockService).releaseSupplies(eq("os-1"), anyList());
    }

    @Test
    void finishingLaborRegistersMonitoringWhenTheOsReturnsItsTimes() throws Exception {
        var work = order("os-1", ServiceOrderStatus.EM_EXECUCAO);
        var monitoring = mock(LaborMonitoringService.class);
        service = new ServiceOrderExecutionService(repository, stockService, publisher, osServiceClient, monitoring);
        when(repository.findByServiceOrderId("os-1")).thenReturn(Optional.of(work));
        var response = new ObjectMapper().readTree("{\"ok\":true}");
        var start = LocalDateTime.of(2026, 1, 1, 8, 0);
        when(osServiceClient.finishLabor("os-1", "labor-1", "token")).thenReturn(response);
        when(osServiceClient.registerLaborMonitoring(response, "labor-1"))
                .thenReturn(Optional.of(new OsServiceClient.LaborTimes(start, start.plusHours(2))));

        service.finishLabor("os-1", "labor-1", "token");

        verify(monitoring).registerExecution("os-1", "labor-1", start, start.plusHours(2));
    }

    private ExecutionWork order(String id, ServiceOrderStatus status) {
        return ExecutionWork.builder().id(id).serviceOrderId(id).status(status).correlationId("corr-1")
                .createdAt(LocalDateTime.now()).build();
    }

    private void authenticateMechanic() {
        var principal = new AuthenticatedUserDetails("ana@example.com",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_MECHANIC")), "mechanic-1", "Ana Silva");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

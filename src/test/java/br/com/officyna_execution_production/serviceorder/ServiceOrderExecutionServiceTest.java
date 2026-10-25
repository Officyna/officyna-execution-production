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
import java.util.Optional;

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

    private void authenticateMechanic() {
        var principal = new AuthenticatedUserDetails("ana@example.com",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_MECHANIC")), "mechanic-1", "Ana Silva");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.event.DiagnosisStartedEvent;
import br.com.officyna_execution_production.serviceorder.event.ServiceOrderIdEvent;
import br.com.officyna_execution_production.serviceorder.event.ServiceOrderRecusedEvent;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SagaCompensationConsumerTest {

    @Mock
    private ServiceOrderExecutionService service;

    @InjectMocks
    private SagaCompensationConsumer consumer;

    @Test
    @DisplayName("Deve compensar falha no início de diagnóstico (eventDiagnosisDlq)")
    void shouldCompensateDiagnosisDlq() {
        DiagnosisStartedEvent event = new DiagnosisStartedEvent("os-1", "mech-1", "João");

        consumer.consumeDiagnosisDlq(event, "corr-diag", null, "Bearer token");

        verify(service).compensateDiagnosisFailure(event, "corr-diag");
        assertNull(MDC.get(CorrelationConstants.CORRELATION_ID_KEY));
        assertNull(MDC.get(CorrelationConstants.AUTHORIZATION_KEY));
    }

    @Test
    @DisplayName("Deve compensar falha no envio para aprovação (eventDiagnosisCompletedDlq)")
    void shouldCompensateDiagnosisCompletedDlq() {
        ServiceOrderIdEvent event = new ServiceOrderIdEvent("os-2");

        consumer.consumeDiagnosisCompletedDlq(event, null, "legacy-completed", null);

        verify(service).compensateDiagnosisCompletedFailure(event, "legacy-completed");
        assertNull(MDC.get(CorrelationConstants.CORRELATION_ID_KEY));
    }

    @Test
    @DisplayName("Deve compensar recusa de ordem de serviço gerando correlationId quando não fornecido")
    void shouldCompensateServiceOrderRecused() {
        ServiceOrderRecusedEvent event = ServiceOrderRecusedEvent.builder()
                .serviceOrderId("os-3")
                .status("RECUSADA")
                .build();

        consumer.consumeServiceOrderRecused(event, null, null, null);

        verify(service).compensateServiceOrderRecused(eq(event), anyString());
        assertNull(MDC.get(CorrelationConstants.CORRELATION_ID_KEY));
    }
}

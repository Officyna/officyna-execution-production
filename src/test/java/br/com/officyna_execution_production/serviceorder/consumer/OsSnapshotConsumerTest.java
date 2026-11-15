package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import br.com.officyna_execution_production.serviceorder.event.OsSnapshotEvent;
import br.com.officyna_execution_production.serviceorder.service.ExecutionPublisher;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OsSnapshotConsumerTest {

    @Mock
    private ServiceOrderExecutionService service;

    @Mock
    private ExecutionPublisher executionPublisher;

    @InjectMocks
    private OsSnapshotConsumer consumer;

    @Test
    @DisplayName("Deve consumir snapshot com sucesso propagando correlationId")
    void shouldConsumeSnapshotSuccessfully() {
        OsSnapshotEvent event = new OsSnapshotEvent("os-1", null, ServiceOrderStatus.RECEBIDA);

        consumer.consume(event, "corr-100", null, "Bearer token");

        verify(service).receiveServiceOrderForDiagnosis(event);
        assertEquals("corr-100", event.getCorrelationId());
        assertNull(MDC.get(CorrelationConstants.CORRELATION_ID_KEY));
        assertNull(MDC.get(CorrelationConstants.AUTHORIZATION_KEY));
    }

    @Test
    @DisplayName("Deve usar legacyCorrelationId quando correlationId estiver vazio")
    void shouldUseLegacyCorrelationId() {
        OsSnapshotEvent event = new OsSnapshotEvent("os-2", null, ServiceOrderStatus.RECEBIDA);

        consumer.consume(event, null, "legacy-200", null);

        verify(service).receiveServiceOrderForDiagnosis(event);
        assertEquals("legacy-200", event.getCorrelationId());
    }

    @Test
    @DisplayName("Deve publicar na DLQ quando ocorrer erro no processamento do evento")
    void shouldPublishToDlqWhenExceptionOccurs() {
        OsSnapshotEvent event = new OsSnapshotEvent("os-3", "corr-300", ServiceOrderStatus.RECEBIDA);
        doThrow(new RuntimeException("Simulated error")).when(service).receiveServiceOrderForDiagnosis(event);

        consumer.consume(event, "corr-300", null, null);

        verify(executionPublisher).publish(eq("eventForDiagnosticsDlq"), eq(event), eq("corr-300"));
        assertNull(MDC.get(CorrelationConstants.CORRELATION_ID_KEY));
    }

    @Test
    @DisplayName("Deve publicar mapa vazio na DLQ quando evento for nulo e ocorrer erro")
    void shouldPublishToDlqWhenEventIsNull() {
        doThrow(new RuntimeException("Null error")).when(service).receiveServiceOrderForDiagnosis(null);

        consumer.consume(null, "corr-400", null, null);

        verify(executionPublisher).publish(eq("eventForDiagnosticsDlq"), any(), eq("corr-400"));
    }
}

package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.event.OsSnapshotEvent;
import br.com.officyna_execution_production.serviceorder.service.ExecutionPublisher;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Header;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OsSnapshotConsumer {
    private final ServiceOrderExecutionService service;
    private final ExecutionPublisher executionPublisher;

    @com.newrelic.api.agent.Trace(dispatcher = true)
    @SqsListener("${aws.sqs.event-for-diagnostics}")
    public void consume(
            OsSnapshotEvent event,
            @Header(name = "correlationId", required = false) String correlationId,
            @Header(name = CorrelationConstants.LEGACY_CORRELATION_ID_HEADER, required = false) String legacyCorrelationId,
            @Header(name = CorrelationConstants.AUTHORIZATION_HEADER, required = false) String authorization) {
        String resolvedCorrelationId = (correlationId != null && !correlationId.isBlank())
                ? correlationId
                : legacyCorrelationId;
        if (event != null && (event.getCorrelationId() == null || event.getCorrelationId().isBlank())) {
            event.setCorrelationId(resolvedCorrelationId);
        }
        String dlqCorrelationId = (resolvedCorrelationId != null && !resolvedCorrelationId.isBlank())
                ? resolvedCorrelationId
                : UUID.randomUUID().toString();
        try {
            if (resolvedCorrelationId != null && !resolvedCorrelationId.isBlank()) {
                MDC.put(CorrelationConstants.CORRELATION_ID_KEY, resolvedCorrelationId);
            }
            if (authorization != null && !authorization.isBlank()) {
                MDC.put(CorrelationConstants.AUTHORIZATION_KEY, authorization);
            }
            service.receiveServiceOrderForDiagnosis(event);
        } catch (Exception ex) {
            log.error("Erro ao processar eventForDiagnostics. Enviando para eventForDiagnosticsDlq: {}", ex.getMessage(), ex);
            executionPublisher.publish("eventForDiagnosticsDlq", event != null ? event : Map.of(), dlqCorrelationId);
        } finally {
            MDC.remove(CorrelationConstants.CORRELATION_ID_KEY);
            MDC.remove(CorrelationConstants.AUTHORIZATION_KEY);
        }
    }
}

package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.event.OsSnapshotEvent;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Header;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OsSnapshotConsumer {
    private final ServiceOrderExecutionService service;

    @com.newrelic.api.agent.Trace(dispatcher = true)
    @SqsListener("${aws.sqs.event-for-diagnostics}")
    public void consume(
            OsSnapshotEvent event,
            @Header("correlationId") String correlationId,
            @Header(name = CorrelationConstants.AUTHORIZATION_HEADER, required = false) String authorization) {
        event.setCorrelationId(correlationId);
        try {
            MDC.put(CorrelationConstants.CORRELATION_ID_KEY, correlationId);
            if (authorization != null && !authorization.isBlank()) {
                MDC.put(CorrelationConstants.AUTHORIZATION_KEY, authorization);
            }
            service.receiveServiceOrderForDiagnosis(event);
        } finally {
            MDC.remove(CorrelationConstants.CORRELATION_ID_KEY);
            MDC.remove(CorrelationConstants.AUTHORIZATION_KEY);
        }
    }
}

package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.serviceorder.event.OsSnapshotEvent;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OsSnapshotConsumer {
    private final ServiceOrderExecutionService service;

    @com.newrelic.api.agent.Trace(dispatcher = true)
    @SqsListener("${aws.sqs.event-for-diagnostics}")
    public void consume(OsSnapshotEvent event, @Header("correlationId") String correlationId) {
        event.setCorrelationId(correlationId);
        try (var ignored = org.slf4j.MDC.putCloseable("correlationId", correlationId)) {
            service.receiveServiceOrderForDiagnosis(event);
        }
    }
}

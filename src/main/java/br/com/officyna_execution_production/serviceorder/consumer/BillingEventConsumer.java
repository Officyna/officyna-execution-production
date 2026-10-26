package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.serviceorder.event.BillingDecisionEvent;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingEventConsumer {
    private final ServiceOrderExecutionService service;

    @com.newrelic.api.agent.Trace(dispatcher = true)
    @SqsListener("${aws.sqs.approval-queue}")
    public void consume(BillingDecisionEvent event, @Header(name = "correlationId", required = false) String correlationId) {
        try (var ignored = org.slf4j.MDC.putCloseable("correlationId", correlationId == null ? java.util.UUID.randomUUID().toString() : correlationId)) {
            service.receiveBillingDecision(event, correlationId);
        }
    }
}

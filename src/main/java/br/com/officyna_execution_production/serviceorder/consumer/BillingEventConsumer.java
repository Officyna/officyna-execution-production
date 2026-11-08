package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.event.BillingDecisionEvent;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Header;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BillingEventConsumer {
    private final ServiceOrderExecutionService service;

    @com.newrelic.api.agent.Trace(dispatcher = true)
    @SqsListener("${aws.sqs.approval-queue}")
    public void consume(
            BillingDecisionEvent event,
            @Header(name = "correlationId", required = false) String correlationId,
            @Header(name = CorrelationConstants.AUTHORIZATION_HEADER, required = false) String authorization) {
        String resolvedCorrelationId = (correlationId == null || correlationId.isBlank())
                ? UUID.randomUUID().toString()
                : correlationId;
        try {
            MDC.put(CorrelationConstants.CORRELATION_ID_KEY, resolvedCorrelationId);
            if (authorization != null && !authorization.isBlank()) {
                MDC.put(CorrelationConstants.AUTHORIZATION_KEY, authorization);
            }
            service.receiveBillingDecision(event, resolvedCorrelationId);
        } finally {
            MDC.remove(CorrelationConstants.CORRELATION_ID_KEY);
            MDC.remove(CorrelationConstants.AUTHORIZATION_KEY);
        }
    }
}

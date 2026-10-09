package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.serviceorder.event.BillingDecisionEvent;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingEventConsumer {
    private final ServiceOrderExecutionService service;

    @SqsListener("${aws.sqs.billing-response-queue}")
    public void consume(BillingDecisionEvent event) {
        service.onBilling(event);
    }
}

package br.com.officyna_execution_production.serviceorder.service;

import br.com.officyna_execution_production.serviceorder.event.ExecutionEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionPublisher {
    private final SqsTemplate sqs;
    @Value("${aws.sqs.os-command-queue}")
    private String osQueue;
    @Value("${aws.sqs.billing-request-queue}")
    private String billingQueue;

    public void toOs(ExecutionEvent e) {
        sqs.send(osQueue, e);
    }

    public void toBilling(ExecutionEvent e) {
        sqs.send(billingQueue, e);
    }
}

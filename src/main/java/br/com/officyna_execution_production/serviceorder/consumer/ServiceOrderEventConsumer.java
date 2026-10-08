package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.serviceorder.event.ServiceOrderEvent;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceOrderEventConsumer {

    private final ServiceOrderExecutionService service;

    @SqsListener("${aws.sqs.service-order-queue}")
    public void consume(ServiceOrderEvent event) {

        log.info(
                "Service order event received. serviceOrderId={}",
                event.getServiceOrderId()
        );

        service.process(event);

        log.info(
                "Service order event processed. serviceOrderId={}",
                event.getServiceOrderId()
        );
    }
}
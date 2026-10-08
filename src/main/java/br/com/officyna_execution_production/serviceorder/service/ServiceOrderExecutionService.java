package br.com.officyna_execution_production.serviceorder.service;

import br.com.officyna_execution_production.serviceorder.domain.entity.ServiceOrderExecution;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import br.com.officyna_execution_production.serviceorder.event.ServiceOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderExecutionService {

    private final ServiceOrderExecutionRepository repository;

    public void process(ServiceOrderEvent event) {

        log.info(
                "Processing received service order. serviceOrderId={}",
                event.getServiceOrderId()
        );

        validateEvent(event);

        var existingServiceOrder =
                repository.findByServiceOrderId(event.getServiceOrderId());

        if (existingServiceOrder.isPresent()) {

            log.warn(
                    "Service order already registered. serviceOrderId={}",
                    event.getServiceOrderId()
            );

            return;
        }

        LocalDateTime now = LocalDateTime.now();

        ServiceOrderExecution serviceOrderExecution =
                ServiceOrderExecution.builder()
                        .serviceOrderId(event.getServiceOrderId())
                        .status(ServiceOrderStatus.RECEBIDA)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        repository.save(serviceOrderExecution);

        log.info(
                "Service order registered for execution. serviceOrderId={}, status={}",
                event.getServiceOrderId(),
                ServiceOrderStatus.RECEBIDA
        );
    }

    public ServiceOrderExecution updateStatus(
            String serviceOrderId,
            ServiceOrderStatus newStatus
    ) {

        ServiceOrderExecution serviceOrderExecution =
                repository.findByServiceOrderId(serviceOrderId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Ordem de Serviço não encontrada: "
                                                + serviceOrderId
                                )
                        );

        serviceOrderExecution.setStatus(newStatus);

        ServiceOrderExecution updated =
                repository.save(serviceOrderExecution);

        log.info(
                "Service order status updated. serviceOrderId={}, status={}",
                serviceOrderId,
                newStatus
        );

        return updated;
    }

    private void validateEvent(ServiceOrderEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Service order event cannot be null."
            );
        }

        if (event.getServiceOrderId() == null
                || event.getServiceOrderId().isBlank()) {

            throw new IllegalArgumentException(
                    "Service order id cannot be null or empty."
            );
        }
    }
}
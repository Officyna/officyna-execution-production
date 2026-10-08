package br.com.officyna_execution_production.serviceorder.domain.repository;

import br.com.officyna_execution_production.serviceorder.domain.entity.ServiceOrderExecution;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;

import java.util.List;
import java.util.Optional;

public interface ServiceOrderExecutionRepository {

    ServiceOrderExecution save(
            ServiceOrderExecution execution
    );

    Optional<ServiceOrderExecution> findByServiceOrderId(
            String serviceOrderId
    );

    List<ServiceOrderExecution> findAll();

    List<ServiceOrderExecution> findByStatus(
            ServiceOrderStatus status
    );
}
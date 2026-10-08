package br.com.officyna_execution_production.monitoring.domain.repository;

import br.com.officyna_execution_production.monitoring.domain.entity.LaborExecution;

import java.util.List;
import java.util.Optional;

public interface LaborExecutionRepository {

    LaborExecution save(LaborExecution entity);

    List<LaborExecution> findAll();

    Optional<LaborExecution> findByServiceOrderIdAndLaborId(
            String serviceOrderId,
            String laborId
    );

    List<LaborExecution> findByLaborId(String laborId);
}
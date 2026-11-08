package br.com.officyna_execution_production.serviceorder.domain.repository;

import br.com.officyna_execution_production.serviceorder.domain.entity.ExecutionWork;

import java.util.List;
import java.util.Optional;

public interface ExecutionWorkRepository {
    Optional<ExecutionWork> findByServiceOrderId(String serviceOrderId);
    List<ExecutionWork> findAll();
    ExecutionWork save(ExecutionWork serviceOrder);
}

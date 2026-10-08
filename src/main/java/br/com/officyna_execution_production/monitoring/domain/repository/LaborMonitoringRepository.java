package br.com.officyna_execution_production.monitoring.domain.repository;

import br.com.officyna_execution_production.monitoring.domain.entity.LaborMonitoring;

import java.util.List;
import java.util.Optional;

/**
 * Interface de repositório pura para LaborMonitoringEntity.
 * Sem qualquer dependência de Spring Data ou MongoDB.
 */
public interface LaborMonitoringRepository{
    Optional<LaborMonitoring> findByLaborId(String laborId);
    LaborMonitoring save(LaborMonitoring entity);
    Optional<LaborMonitoring> findById(String id);
    List<LaborMonitoring> findAll();
    void deleteById(String id);
    boolean existsById(String id);
}


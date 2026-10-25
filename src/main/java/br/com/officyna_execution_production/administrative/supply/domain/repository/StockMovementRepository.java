package br.com.officyna_execution_production.administrative.supply.domain.repository;

import br.com.officyna_execution_production.administrative.supply.domain.entity.StockMovement;

import java.util.List;

public interface StockMovementRepository {
    StockMovement save(StockMovement movement);
    List<StockMovement> findByServiceOrderIdOrderByCreatedAtDesc(String serviceOrderId);
}

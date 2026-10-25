package br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository;

import br.com.officyna_execution_production.infrastructure.persistence.mongodb.model.StockMovementDocument;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StockMovementMongoRepository extends MongoRepository<StockMovementDocument, String> {
    List<StockMovementDocument> findByServiceOrderIdOrderByCreatedAtDesc(String serviceOrderId);
}

package br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository;

import br.com.officyna_execution_production.infrastructure.persistence.mongodb.model.SupplyDocument;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SupplyMongoRepository extends MongoRepository<SupplyDocument, String> {
    boolean existsByName(String name);

    List<SupplyDocument> findByActiveTrue();

    List<SupplyDocument> findByActiveTrueAndType(String type);
}

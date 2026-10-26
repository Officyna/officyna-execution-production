package br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.repository;

import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.document.ExecutionWorkDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ExecutionWorkMongoRepository extends MongoRepository<ExecutionWorkDocument, String> {
    Optional<ExecutionWorkDocument> findByServiceOrderId(String serviceOrderId);
}

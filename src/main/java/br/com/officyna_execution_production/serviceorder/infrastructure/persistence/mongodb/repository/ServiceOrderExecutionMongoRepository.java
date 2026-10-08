package br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.repository;

import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.document.ServiceOrderExecutionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ServiceOrderExecutionMongoRepository
        extends MongoRepository<ServiceOrderExecutionDocument, String> {

    Optional<ServiceOrderExecutionDocument> findByServiceOrderId(
            String serviceOrderId
    );
}
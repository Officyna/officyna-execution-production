package br.com.officyna_execution_production.monitoring.infrastructure.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.*;

public interface LaborExecutionMongoRepository extends MongoRepository<LaborExecutionDocument, String> {
    Optional<LaborExecutionDocument> findByServiceOrderIdAndLaborId(String serviceOrderId, String laborId);

    List<LaborExecutionDocument> findByLaborId(String laborId);
}

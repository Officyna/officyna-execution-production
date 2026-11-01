package br.com.officyna_execution_production.monitoring.infrastructure.persistence;

import br.com.officyna_execution_production.monitoring.domain.entity.LaborExecution;
import br.com.officyna_execution_production.monitoring.domain.repository.LaborExecutionRepository;
import br.com.officyna_execution_production.infrastructure.persistence.mongodb.gateway.AbstractMongoRepositoryGateway;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LaborExecutionRepositoryGateway extends AbstractMongoRepositoryGateway<LaborExecution, LaborExecutionDocument, LaborExecutionMongoRepository> implements LaborExecutionRepository {
    public LaborExecutionRepositoryGateway(LaborExecutionMongoRepository repo) {
        super(repo, e -> LaborExecutionDocument.builder().id(e.getId()).serviceOrderId(e.getServiceOrderId()).laborId(e.getLaborId()).startDate(e.getStartDate()).endDate(e.getEndDate()).build(),
                d -> LaborExecution.builder().id(d.getId()).serviceOrderId(d.getServiceOrderId()).laborId(d.getLaborId()).startDate(d.getStartDate()).endDate(d.getEndDate()).build());
    }

    public Optional<LaborExecution> findByServiceOrderIdAndLaborId(String os, String labor) {
        return mongoRepository.findByServiceOrderIdAndLaborId(os, labor).map(toEntity);
    }

    public List<LaborExecution> findByLaborId(String id) {
        return mongoRepository.findByLaborId(id).stream().map(toEntity).toList();
    }
}

package br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.gateway;

import br.com.officyna_execution_production.serviceorder.domain.entity.ExecutionWork;
import br.com.officyna_execution_production.serviceorder.domain.repository.ExecutionWorkRepository;
import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.mapper.ExecutionWorkDocumentMapper;
import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.repository.ExecutionWorkMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExecutionWorkRepositoryGateway implements ExecutionWorkRepository {
    private final ExecutionWorkMongoRepository mongoRepository;

    @Override
    public Optional<ExecutionWork> findByServiceOrderId(String serviceOrderId) {
        return mongoRepository.findByServiceOrderId(serviceOrderId).map(ExecutionWorkDocumentMapper::toEntity);
    }

    @Override
    public List<ExecutionWork> findAll() {
        return mongoRepository.findAll().stream().map(ExecutionWorkDocumentMapper::toEntity).toList();
    }

    @Override
    public ExecutionWork save(ExecutionWork serviceOrder) {
        return ExecutionWorkDocumentMapper.toEntity(mongoRepository.save(ExecutionWorkDocumentMapper.toDocument(serviceOrder)));
    }
}

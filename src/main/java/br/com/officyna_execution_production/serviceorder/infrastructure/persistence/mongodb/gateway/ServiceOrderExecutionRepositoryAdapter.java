package br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.gateway;

import br.com.officyna_execution_production.serviceorder.domain.entity.ServiceOrderExecution;
import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mapper.ServiceOrderExecutionMapper;
import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.repository.ServiceOrderExecutionMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServiceOrderExecutionRepositoryAdapter
        implements ServiceOrderExecutionRepository {

    private final ServiceOrderExecutionMongoRepository mongoRepository;
    private final ServiceOrderExecutionMapper mapper;

    @Override
    public ServiceOrderExecution save(
            ServiceOrderExecution serviceOrderExecution
    ) {

        var document = mapper.toDocument(serviceOrderExecution);

        var savedDocument = mongoRepository.save(document);

        return mapper.toDomain(savedDocument);
    }

    @Override
    public Optional<ServiceOrderExecution> findByServiceOrderId(
            String serviceOrderId
    ) {

        return mongoRepository
                .findByServiceOrderId(serviceOrderId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ServiceOrderExecution> findAll() {

        return mongoRepository
                .findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
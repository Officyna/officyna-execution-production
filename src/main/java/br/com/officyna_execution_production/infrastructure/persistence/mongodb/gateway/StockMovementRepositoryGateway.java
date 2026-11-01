package br.com.officyna_execution_production.infrastructure.persistence.mongodb.gateway;

import br.com.officyna_execution_production.administrative.supply.domain.entity.StockMovement;
import br.com.officyna_execution_production.administrative.supply.domain.repository.StockMovementRepository;
import br.com.officyna_execution_production.infrastructure.persistence.mapper.StockMovementEntityDocumentMapper;
import br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository.StockMovementMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockMovementRepositoryGateway implements StockMovementRepository {
    private final StockMovementMongoRepository mongoRepository;
    private final StockMovementEntityDocumentMapper mapper;

    @Override
    public StockMovement save(StockMovement movement) {
        return mapper.toEntity(mongoRepository.save(mapper.toDocument(movement)));
    }

    @Override
    public List<StockMovement> findByServiceOrderIdOrderByCreatedAtDesc(String serviceOrderId) {
        return mongoRepository.findByServiceOrderIdOrderByCreatedAtDesc(serviceOrderId)
                .stream().map(mapper::toEntity).toList();
    }
}

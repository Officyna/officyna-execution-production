package br.com.officyna_execution_production.infrastructure.persistence.mongodb.gateway;

import br.com.officyna_execution_production.infrastructure.persistence.mapper.SupplyEntityDocumentMapper;

import br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository.SupplyMongoRepository;

import br.com.officyna_execution_production.administrative.supply.domain.entity.Supply;
import br.com.officyna_execution_production.administrative.supply.domain.entity.SupplyType;
import br.com.officyna_execution_production.administrative.supply.domain.repository.SupplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SupplyRepositoryGateway implements SupplyRepository {
    private final SupplyMongoRepository mongo;
    private final SupplyEntityDocumentMapper mapper;

    @Override
    public boolean existsByName(String name) {
        return mongo.existsByName(name);
    }

    @Override
    public List<Supply> findByActiveTrue() {
        return mongo.findByActiveTrue().stream().map(mapper::toEntity).toList();
    }

    @Override
    public List<Supply> findByActiveTrueAndType(SupplyType type) {
        return mongo.findByActiveTrueAndType(type.name()).stream().map(mapper::toEntity).toList();
    }

    @Override
    public Supply save(Supply entity) {
        return mapper.toEntity(mongo.save(mapper.toDocument(entity)));
    }

    @Override
    public Optional<Supply> findById(String id) {
        return mongo.findById(id).map(mapper::toEntity);
    }

    @Override
    public List<Supply> findAll() {
        return mongo.findAll().stream().map(mapper::toEntity).toList();
    }

    @Override
    public void deleteById(String id) {
        mongo.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return mongo.existsById(id);
    }
}

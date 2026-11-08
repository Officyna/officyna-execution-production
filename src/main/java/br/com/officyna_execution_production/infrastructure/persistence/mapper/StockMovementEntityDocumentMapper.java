package br.com.officyna_execution_production.infrastructure.persistence.mapper;

import br.com.officyna_execution_production.administrative.supply.domain.entity.StockMovement;
import br.com.officyna_execution_production.infrastructure.persistence.mongodb.model.StockMovementDocument;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class StockMovementEntityDocumentMapper {
    public StockMovementDocument toDocument(StockMovement movement) {
        StockMovementDocument document = new StockMovementDocument();
        BeanUtils.copyProperties(movement, document);
        return document;
    }

    public StockMovement toEntity(StockMovementDocument document) {
        StockMovement movement = new StockMovement();
        BeanUtils.copyProperties(document, movement);
        return movement;
    }
}

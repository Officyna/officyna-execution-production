package br.com.officyna_execution_production.infrastructure.persistence.mapper;

import br.com.officyna_execution_production.infrastructure.persistence.mongodb.model.SupplyDocument;

import br.com.officyna_execution_production.administrative.supply.domain.entity.Supply;
import br.com.officyna_execution_production.administrative.supply.domain.entity.SupplyType;
import org.springframework.stereotype.Component;

@Component
public class SupplyEntityDocumentMapper {
    public SupplyDocument toDocument(Supply entity) {
        return SupplyDocument.builder()
                .id(entity.getId()).name(entity.getName()).description(entity.getDescription())
                .type(entity.getType() == null ? null : entity.getType().name())
                .purchasePrice(entity.getPurchasePrice()).salePrice(entity.getSalePrice())
                .stockQuantity(entity.getStockQuantity()).minimumQuantity(entity.getMinimumQuantity())
                .reservedQuantity(entity.getReservedQuantity()).active(entity.getActive())
                .createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }

    public Supply toEntity(SupplyDocument document) {
        return Supply.builder()
                .id(document.getId()).name(document.getName()).description(document.getDescription())
                .type(document.getType() == null ? null : SupplyType.valueOf(document.getType()))
                .purchasePrice(document.getPurchasePrice()).salePrice(document.getSalePrice())
                .stockQuantity(document.getStockQuantity()).minimumQuantity(document.getMinimumQuantity())
                .reservedQuantity(document.getReservedQuantity()).active(document.getActive())
                .createdAt(document.getCreatedAt()).updatedAt(document.getUpdatedAt()).build();
    }
}

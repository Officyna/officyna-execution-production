package br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mapper;

import br.com.officyna_execution_production.serviceorder.domain.entity.ServiceOrderExecution;
import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.document.ServiceOrderExecutionDocument;
import org.springframework.stereotype.Component;

@Component
public class ServiceOrderExecutionMapper {

    public ServiceOrderExecutionDocument toDocument(
            ServiceOrderExecution entity
    ) {

        return ServiceOrderExecutionDocument.builder()
                .id(entity.getId())
                .serviceOrderId(entity.getServiceOrderId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ServiceOrderExecution toDomain(
            ServiceOrderExecutionDocument document
    ) {

        return ServiceOrderExecution.builder()
                .id(document.getId())
                .serviceOrderId(document.getServiceOrderId())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
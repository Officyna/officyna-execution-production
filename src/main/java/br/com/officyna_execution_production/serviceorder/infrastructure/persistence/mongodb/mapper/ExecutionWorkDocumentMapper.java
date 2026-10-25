package br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.mapper;

import br.com.officyna_execution_production.serviceorder.domain.entity.ExecutionWork;
import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.document.ExecutionWorkDocument;
import org.springframework.beans.BeanUtils;

public final class ExecutionWorkDocumentMapper {
    private ExecutionWorkDocumentMapper() {}

    public static ExecutionWork toEntity(ExecutionWorkDocument document) {
        // Restore saved state without executing another status transition.
        ExecutionWork serviceOrder = ExecutionWork.builder().status(document.getStatus()).build();
        BeanUtils.copyProperties(document, serviceOrder, "status");
        serviceOrder.initializeStockReservationItems();
        return serviceOrder;
    }

    public static ExecutionWorkDocument toDocument(ExecutionWork serviceOrder) {
        ExecutionWorkDocument document = new ExecutionWorkDocument();
        BeanUtils.copyProperties(serviceOrder, document);
        return document;
    }
}

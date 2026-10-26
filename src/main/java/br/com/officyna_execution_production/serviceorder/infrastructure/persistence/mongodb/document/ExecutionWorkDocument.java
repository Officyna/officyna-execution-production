package br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.document;

import br.com.officyna_execution_production.serviceorder.domain.entity.*;
import lombok.*;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("execution_work")
public class ExecutionWorkDocument {
    @Id
    private String id;
    @Indexed(unique = true)
    private String serviceOrderId;
    private String mechanicId;
    private String mechanicName;
    private String correlationId;
    private ServiceOrderStatus status;
    private LocalDateTime registrationDate, diagnosisStartDate, clientSendDate, approvalDate,
            executionStartDate, finalizationDate, refuseDate;
    private boolean stockReserved;
    private boolean stockConsumed;
    private List<StockItemRequest> stockReservationItems;
    private LocalDateTime createdAt, updatedAt;
}

package br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.document;

import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_order_execution")
public class ServiceOrderExecutionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String serviceOrderId;

    private ServiceOrderStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
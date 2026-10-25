package br.com.officyna_execution_production.infrastructure.persistence.mongodb.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stock_movements")
public class StockMovementDocument {
    @Id
    private String id;
    private String serviceOrderId;
    private String supplyId;
    private String operation;
    private Integer quantity;
    private LocalDateTime createdAt;
}

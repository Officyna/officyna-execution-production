package br.com.officyna_execution_production.administrative.supply.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {
    private String id;
    private String serviceOrderId;
    private String supplyId;
    private String operation;
    private Integer quantity;
    private LocalDateTime createdAt;
}

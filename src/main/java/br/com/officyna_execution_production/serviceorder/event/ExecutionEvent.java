package br.com.officyna_execution_production.serviceorder.event;

import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import lombok.*;

import java.util.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEvent {
    private String eventId;
    private String correlationId;
    private String serviceOrderId;
    private String type;
    private ServiceOrderStatus expectedStatus, newStatus;
    private String mechanicId;
    private List<LaborEvent> labors;
    private List<SupplyItem> supplies;
    private BigDecimal totalAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplyItem {
        private String supplyId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}

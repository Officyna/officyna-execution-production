package br.com.officyna_execution_production.serviceorder.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderEvent {

    private String serviceOrderId;

    private String customerId;

    private String vehicleId;

    private List<LaborEvent> labors;

    private LocalDateTime createdAt;
}
package br.com.officyna_execution_production.serviceorder.event;

import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OsSnapshotEvent {
    private String correlationId;
    private String serviceOrderId;
    private ServiceOrderStatus status;
    private String mechanicId;
}

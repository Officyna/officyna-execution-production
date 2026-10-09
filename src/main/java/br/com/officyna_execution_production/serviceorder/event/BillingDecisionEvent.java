package br.com.officyna_execution_production.serviceorder.event;

import br.com.officyna_execution_production.serviceorder.domain.enums.*;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingDecisionEvent {
    private String eventId;
    private String serviceOrderId;
    private ServiceOrderStatus status;
    private Map<String, LaborSituation> laborSituations;
}

package br.com.officyna_execution_production.serviceorder.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingDecisionEvent {
    private String serviceOrderId;
    private String statusPayment;
}

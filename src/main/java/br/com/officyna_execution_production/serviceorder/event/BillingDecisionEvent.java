package br.com.officyna_execution_production.serviceorder.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingDecisionEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonAlias({"serviceOrderOd", "service_order_id"})
    private String serviceOrderId;

    private PaymentInfo payment;

    @JsonAlias({"status_payment", "paymentStatus", "status"})
    private String statusPayment;

    @JsonAlias({"paymentDescription", "reason"})
    private String description;

    public BillingDecisionEvent(String serviceOrderId, String statusPayment) {
        this.serviceOrderId = serviceOrderId;
        this.statusPayment = statusPayment;
    }

    public String getStatusPayment() {
        if (payment != null && payment.getStatus() != null && !payment.getStatus().isBlank()) {
            return payment.getStatus();
        }
        return statusPayment;
    }

    @JsonProperty("Message")
    private void unpackSnsMessage(String messageJson) {
        if (messageJson == null || messageJson.isBlank()) {
            return;
        }
        try {
            BillingDecisionEvent nested = MAPPER.readValue(messageJson, BillingDecisionEvent.class);
            if (this.serviceOrderId == null || this.serviceOrderId.isBlank()) {
                this.serviceOrderId = nested.getServiceOrderId();
            }
            if (this.payment == null) {
                this.payment = nested.getPayment();
            }
            if (this.statusPayment == null || this.statusPayment.isBlank()) {
                this.statusPayment = nested.statusPayment;
            }
            if (this.description == null || this.description.isBlank()) {
                this.description = nested.getDescription();
            }
        } catch (Exception ignored) {
            // Ignora caso não seja um envelope SNS válido
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentInfo {

        private String paymentId;

        private String status;

        private String paymentMethod;

        private BigDecimal totalPaidAmount;

        private String paidAt;
    }
}

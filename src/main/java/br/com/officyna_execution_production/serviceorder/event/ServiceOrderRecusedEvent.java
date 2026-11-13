package br.com.officyna_execution_production.serviceorder.event;

import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceOrderRecusedEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonAlias({"serviceOrderOd", "service_order_id"})
    private String serviceOrderId;

    private String status;

    @JsonAlias({"sypplyes", "suppliesDetails"})
    private List<RecusedSupplyItem> supplies;

    @JsonProperty("Message")
    private void unpackSnsMessage(String messageJson) {
        if (messageJson == null || messageJson.isBlank()) {
            return;
        }
        try {
            ServiceOrderRecusedEvent nested = MAPPER.readValue(messageJson, ServiceOrderRecusedEvent.class);
            if (this.serviceOrderId == null || this.serviceOrderId.isBlank()) {
                this.serviceOrderId = nested.getServiceOrderId();
            }
            if (this.status == null || this.status.isBlank()) {
                this.status = nested.getStatus();
            }
            if (this.supplies == null || this.supplies.isEmpty()) {
                this.supplies = nested.getSupplies();
            }
        } catch (Exception ignored) {
            // Ignora caso não seja um envelope SNS válido
        }
    }

    public List<StockItemRequest> toStockItemRequests() {
        if (supplies == null || supplies.isEmpty()) {
            return List.of();
        }
        List<StockItemRequest> items = new ArrayList<>();
        for (RecusedSupplyItem item : supplies) {
            if (item == null) {
                continue;
            }
            String resolvedId = item.resolveSupplyId();
            if (resolvedId != null && !resolvedId.isBlank() && item.getQuantity() != null && item.getQuantity() > 0) {
                items.add(new StockItemRequest(resolvedId, item.getQuantity()));
            }
        }
        return List.copyOf(items);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecusedSupplyItem {
        private String id;
        private String supplyId;
        private String supplieId;
        private Integer quantity;
        private String name;
        private BigDecimal unitPrice;

        public String resolveSupplyId() {
            if (supplyId != null && !supplyId.isBlank()) {
                return supplyId;
            }
            if (supplieId != null && !supplieId.isBlank()) {
                return supplieId;
            }
            return id;
        }
    }
}


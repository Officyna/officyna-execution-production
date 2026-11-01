package br.com.officyna_execution_production.serviceorder.service;

import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OsServiceClient {
    public record LaborTimes(LocalDateTime start, LocalDateTime end) {}
    private final RestClient.Builder restClientBuilder;

    @Value("${os-service.base-url}")
    private String baseUrl;

    private RestClient client() {
        return restClientBuilder.baseUrl(baseUrl).build();
    }

    private String bearer(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("JWT obrigatório para chamar o os-service");
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }

    public JsonNode findServiceOrder(String serviceOrderId, String token) {
        return client().get().uri("/api/serviceorder/{id}", serviceOrderId)
                .header("Authorization", bearer(token)).retrieve().body(JsonNode.class);
    }

    public JsonNode addLabor(String serviceOrderId, String laborId, String token) {
        return addItemsWithPatchCompatibility("/api/serviceorder/{id}/add-labors", serviceOrderId,
                List.of(Map.of("id", laborId)), token);
    }

    private RestClient.RequestBodySpec request(String token, String path, String serviceOrderId) {
        return client().put().uri(path, serviceOrderId).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer(token));
    }

    public JsonNode removeLabor(String serviceOrderId, String laborId, String token) {
        return client().put().uri("/api/serviceorder/{id}/remove-labors/{laborId}", serviceOrderId, laborId)
                .header("Authorization", bearer(token)).retrieve().body(JsonNode.class);
    }

    public JsonNode addSupply(String serviceOrderId, String supplyId, int quantity, String token) {
        return addItemsWithPatchCompatibility("/api/serviceorder/{id}/add-supply", serviceOrderId,
                List.of(Map.of("id", supplyId, "quantity", quantity)), token);
    }

    private JsonNode addItemsWithPatchCompatibility(String path, String serviceOrderId, Object body, String token) {
        try {
            return client().patch().uri(path, serviceOrderId).contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", bearer(token)).body(body).retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.MethodNotAllowed e) {
            // The checked-in os-service currently exposes these mutations as PUT; newer deployments may expose PATCH.
            return request(token, path, serviceOrderId).body(body).retrieve().body(JsonNode.class);
        }
    }

    public JsonNode removeSupply(String serviceOrderId, String supplyId, String token) {
        return client().put().uri("/api/serviceorder/{id}/remove-supply/{supplyId}", serviceOrderId, supplyId)
                .header("Authorization", bearer(token)).retrieve().body(JsonNode.class);
    }

    public JsonNode updateInformation(String serviceOrderId, String informationText, String token) {
        JsonNode currentOrder = findServiceOrder(serviceOrderId, token);
        String mechanicId = currentOrder == null ? null : currentOrder.path("mechanic").path("id").asText(null);
        Map<String, Object> changes = new java.util.LinkedHashMap<>();
        changes.put("informationText", informationText == null ? "" : informationText);
        if (mechanicId != null && !mechanicId.isBlank()) changes.put("mechanicId", mechanicId);
        return client().patch().uri("/api/serviceorder/{id}", serviceOrderId)
                .contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token))
                .body(changes)
                .retrieve().body(JsonNode.class);
    }

    public JsonNode startLabor(String serviceOrderId, String laborId, String token) {
        return client().put().uri("/api/serviceorder/{id}/start-labor/{laborId}", serviceOrderId, laborId)
                .header("Authorization", bearer(token)).retrieve().body(JsonNode.class);
    }

    public JsonNode finishLabor(String serviceOrderId, String laborId, String token) {
        return client().put().uri("/api/serviceorder/{id}/finish-labor/{laborId}", serviceOrderId, laborId)
                .header("Authorization", bearer(token)).retrieve().body(JsonNode.class);
    }

    public JsonNode updateStatus(String serviceOrderId, ServiceOrderStatus status, String token) {
        return client().patch().uri(uriBuilder -> uriBuilder
                        .path("/api/serviceorder/{id}/update-status/")
                        .queryParam("status", status.name())
                        .build(serviceOrderId))
                .header("Authorization", bearer(token)).retrieve().body(JsonNode.class);
    }

    public Optional<LaborTimes> registerLaborMonitoring(JsonNode order, String laborId) {
        JsonNode labors = order == null ? null : order.path("labors").path("laborsDetails");
        if (labors == null || !labors.isArray()) return Optional.empty();
        for (JsonNode labor : labors) {
            // os-service serializes LaborDetailDTO.laborId (not "id").
            if (!laborId.equals(labor.path("laborId").asText())) continue;
            String start = labor.path("startDate").asText(null);
            String end = labor.path("endDate").asText(null);
            if (start == null || end == null) return Optional.empty();
            try {
                return Optional.of(new LaborTimes(LocalDateTime.parse(start), LocalDateTime.parse(end)));
            } catch (DateTimeParseException exception) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public List<StockItemRequest> getSupplies(String serviceOrderId, String token) {
        JsonNode response = findServiceOrder(serviceOrderId, token);
        JsonNode details = response == null ? null : response.path("supplys").path("supplysDetails");
        if (details == null || !details.isArray()) return List.of();
        java.util.ArrayList<StockItemRequest> items = new java.util.ArrayList<>();
        details.forEach(item -> {
            String id = item.path("id").asText(null);
            int quantity = item.path("quantity").asInt(0);
            if (id != null && !id.isBlank() && quantity > 0) items.add(new StockItemRequest(id, quantity));
        });
        return List.copyOf(items);
    }
}

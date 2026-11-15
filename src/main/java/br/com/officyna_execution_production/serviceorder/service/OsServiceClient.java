package br.com.officyna_execution_production.serviceorder.service;

import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;
import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OsServiceClient {
    public record LaborTimes(LocalDateTime start, LocalDateTime end) {}
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${os-service.base-url}")
    private String baseUrl;

    private RestClient client() {
        return restClientBuilder.baseUrl(baseUrl).build();
    }

    private JsonNode parseJson(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            log.error("Erro ao converter resposta do os-service para JsonNode: {}", e.getMessage());
            throw new RuntimeException("Erro ao converter resposta do os-service", e);
        }
    }

    private String resolveToken(String explicitToken) {
        if (explicitToken != null && !explicitToken.isBlank()) {
            return explicitToken.startsWith("Bearer ") ? explicitToken : "Bearer " + explicitToken;
        }

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null && !auth.isBlank()) {
                    return auth.startsWith("Bearer ") ? auth : "Bearer " + auth;
                }
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() instanceof String credentials && !credentials.isBlank()) {
                return credentials.startsWith("Bearer ") ? credentials : "Bearer " + credentials;
            }

            String mdcAuth = MDC.get(CorrelationConstants.AUTHORIZATION_KEY);
            if (mdcAuth != null && !mdcAuth.isBlank()) {
                return mdcAuth.startsWith("Bearer ") ? mdcAuth : "Bearer " + mdcAuth;
            }
        } catch (Exception e) {
            log.debug("Could not resolve authorization token: {}", e.getMessage());
        }

        throw new IllegalArgumentException("JWT obrigatório para chamar o os-service");
    }

    private void propagateHeaders(HttpHeaders headers, String explicitToken) {
        try {
            String token = resolveToken(explicitToken);
            headers.set(HttpHeaders.AUTHORIZATION, token);

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String correlationId = MDC.get(CorrelationConstants.CORRELATION_ID_KEY);
            if ((correlationId == null || correlationId.isBlank()) && attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                correlationId = request.getHeader(CorrelationConstants.CORRELATION_ID_HEADER);
                if (correlationId == null || correlationId.isBlank()) {
                    correlationId = request.getHeader(CorrelationConstants.LEGACY_CORRELATION_ID_HEADER);
                }
            }

            if (correlationId != null && !correlationId.isBlank()) {
                headers.set(CorrelationConstants.CORRELATION_ID_HEADER, correlationId);
                headers.set(CorrelationConstants.LEGACY_CORRELATION_ID_HEADER, correlationId);
            }
        } catch (Exception e) {
            log.debug("Could not propagate request headers: {}", e.getMessage());
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
        }
    }

    public JsonNode findServiceOrder(String serviceOrderId, String token) {
        String response = client().get().uri("/api/serviceorder/{id}", serviceOrderId)
                .headers(h -> propagateHeaders(h, token))
                .retrieve().body(String.class);
        return parseJson(response);
    }

    public JsonNode addLabor(String serviceOrderId, String laborId, String token) {
        return addItemsWithPatchCompatibility("/api/serviceorder/{id}/add-labors", serviceOrderId,
                List.of(Map.of("id", laborId)), token);
    }

    private RestClient.RequestBodySpec request(String token, String path, String serviceOrderId) {
        return client().put().uri(path, serviceOrderId).contentType(MediaType.APPLICATION_JSON)
                .headers(h -> propagateHeaders(h, token));
    }

    public JsonNode removeLabor(String serviceOrderId, String laborId, String token) {
        String response = client().put().uri("/api/serviceorder/{id}/remove-labors/{laborId}", serviceOrderId, laborId)
                .headers(h -> propagateHeaders(h, token))
                .retrieve().body(String.class);
        return parseJson(response);
    }

    public JsonNode addSupply(String serviceOrderId, String supplyId, int quantity, String name, String description, BigDecimal unitPrice, String token) {
        Map<String, Object> supplyPayload = new LinkedHashMap<>();
        supplyPayload.put("id", supplyId);
        supplyPayload.put("quantity", quantity);
        supplyPayload.put("name", name);
        supplyPayload.put("description", description);
        supplyPayload.put("unitPrice", unitPrice);
        return addItemsWithPatchCompatibility("/api/serviceorder/{id}/add-supply", serviceOrderId,
                List.of(supplyPayload), token);
    }

    private JsonNode addItemsWithPatchCompatibility(String path, String serviceOrderId, Object body, String token) {
        try {
            String response = client().patch().uri(path, serviceOrderId).contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> propagateHeaders(h, token))
                    .body(body).retrieve().body(String.class);
            return parseJson(response);
        } catch (HttpClientErrorException.MethodNotAllowed e) {
            // The checked-in os-service currently exposes these mutations as PUT; newer deployments may expose PATCH.
            String response = request(token, path, serviceOrderId).body(body).retrieve().body(String.class);
            return parseJson(response);
        }
    }

    public JsonNode removeSupply(String serviceOrderId, String supplyId, String token) {
        String response = client().put().uri("/api/serviceorder/{id}/remove-supply/{supplyId}", serviceOrderId, supplyId)
                .headers(h -> propagateHeaders(h, token))
                .retrieve().body(String.class);
        return parseJson(response);
    }

    public JsonNode updateInformation(String serviceOrderId, String informationText, String token) {
        JsonNode currentOrder = findServiceOrder(serviceOrderId, token);
        String mechanicId = currentOrder == null ? null : currentOrder.path("mechanic").path("id").asText(null);
        Map<String, Object> changes = new java.util.LinkedHashMap<>();
        changes.put("informationText", informationText == null ? "" : informationText);
        if (mechanicId != null && !mechanicId.isBlank()) changes.put("mechanicId", mechanicId);
        String response = client().patch().uri("/api/serviceorder/{id}", serviceOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> propagateHeaders(h, token))
                .body(changes)
                .retrieve().body(String.class);
        return parseJson(response);
    }

    public JsonNode startLabor(String serviceOrderId, String laborId, String token) {
        String response = client().put().uri("/api/serviceorder/{id}/start-labor/{laborId}", serviceOrderId, laborId)
                .headers(h -> propagateHeaders(h, token))
                .retrieve().body(String.class);
        return parseJson(response);
    }

    public JsonNode finishLabor(String serviceOrderId, String laborId, String token) {
        String response = client().put().uri("/api/serviceorder/{id}/finish-labor/{laborId}", serviceOrderId, laborId)
                .headers(h -> propagateHeaders(h, token))
                .retrieve().body(String.class);
        return parseJson(response);
    }

    public JsonNode updateStatus(String serviceOrderId, ServiceOrderStatus status, String token) {
        String response = client().patch().uri(uriBuilder -> uriBuilder
                        .path("/api/serviceorder/{id}/update-status/")
                        .queryParam("status", status.name())
                        .build(serviceOrderId))
                .headers(h -> propagateHeaders(h, token))
                .retrieve().body(String.class);
        return parseJson(response);
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

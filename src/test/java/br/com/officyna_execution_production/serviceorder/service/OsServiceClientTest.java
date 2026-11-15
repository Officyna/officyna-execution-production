package br.com.officyna_execution_production.serviceorder.service;

import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;
import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OsServiceClientTest {

    private MockRestServiceServer mockServer;
    private OsServiceClient osServiceClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        objectMapper = JsonMapper.builder().findAndAddModules().build();
        RestClient.Builder builder = RestClient.builder();

        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        this.osServiceClient = new OsServiceClient(builder, objectMapper);
        ReflectionTestUtils.setField(osServiceClient, "baseUrl", "http://localhost:8080");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Deve usar token explicito quando fornecido")
    void findServiceOrder_WithExplicitToken() {
        String json = "{\"id\":\"os-100\"}";
        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer explicit-token"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.findServiceOrder("os-100", "explicit-token");

        assertNotNull(result);
        assertEquals("os-100", result.path("id").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve resolver token a partir do RequestContextHolder quando nao fornecido explicitamente")
    void findServiceOrder_ResolvesTokenFromRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer req-token");
        request.addHeader(CorrelationConstants.CORRELATION_ID_HEADER, "corr-req-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String json = "{\"id\":\"os-200\"}";
        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-200"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer req-token"))
                .andExpect(header(CorrelationConstants.CORRELATION_ID_HEADER, "corr-req-123"))
                .andExpect(header(CorrelationConstants.LEGACY_CORRELATION_ID_HEADER, "corr-req-123"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.findServiceOrder("os-200", null);

        assertNotNull(result);
        assertEquals("os-200", result.path("id").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve resolver token a partir do SecurityContextHolder quando RequestContext nao possuir header")
    void findServiceOrder_ResolvesTokenFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("mechanic", "sec-token-123", List.of())
        );

        String json = "{\"id\":\"os-300\"}";
        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-300"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sec-token-123"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.findServiceOrder("os-300", "");

        assertNotNull(result);
        assertEquals("os-300", result.path("id").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve resolver token a partir do MDC quando os outros contextos forem vazios")
    void findServiceOrder_ResolvesTokenFromMdc() {
        MDC.put(CorrelationConstants.AUTHORIZATION_KEY, "Bearer mdc-token-777");
        MDC.put(CorrelationConstants.CORRELATION_ID_KEY, "corr-mdc-999");

        String json = "{\"id\":\"os-400\"}";
        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-400"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer mdc-token-777"))
                .andExpect(header(CorrelationConstants.CORRELATION_ID_HEADER, "corr-mdc-999"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.findServiceOrder("os-400", null);

        assertNotNull(result);
        assertEquals("os-400", result.path("id").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve lancar IllegalArgumentException se nenhum token puder ser resolvido")
    void findServiceOrder_ThrowsWhenNoToken() {
        assertThrows(IllegalArgumentException.class, () -> osServiceClient.findServiceOrder("os-500", null));
    }

    @Test
    @DisplayName("Deve adicionar labor com PATCH")
    void addLabor_SendsPatchRequest() {
        String responseJson = "{\"id\":\"os-100\"}";

        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100/add-labors"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer explicit-token"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.addLabor("os-100", "labor-1", "explicit-token");

        assertNotNull(result);
        assertEquals("os-100", result.path("id").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve remover labor com PUT")
    void removeLabor_SendsPutRequest() {
        String responseJson = "{\"id\":\"os-100\"}";

        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100/remove-labors/labor-1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer explicit-token"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.removeLabor("os-100", "labor-1", "explicit-token");

        assertNotNull(result);
        assertEquals("os-100", result.path("id").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve enviar id, quantity, name, description e unitPrice ao adicionar suprimento")
    void addSupply_SendsCompleteSupplyPayload() {
        String expectedRequest = "[{\"id\":\"supply-1\",\"quantity\":2,\"name\":\"Filtro de Óleo\",\"description\":\"Filtro sintético\",\"unitPrice\":100.00}]";
        String responseJson = "{\"id\":\"os-100\"}";

        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100/add-supply"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer explicit-token"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().json(expectedRequest))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.addSupply(
                "os-100",
                "supply-1",
                2,
                "Filtro de Óleo",
                "Filtro sintético",
                new BigDecimal("100.00"),
                "explicit-token"
        );

        assertNotNull(result);
        assertEquals("os-100", result.path("id").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve remover supply com PUT")
    void removeSupply_SendsPutRequest() {
        String responseJson = "{\"id\":\"os-100\"}";

        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100/remove-supply/sup-1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer explicit-token"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.removeSupply("os-100", "sup-1", "explicit-token");

        assertNotNull(result);
        assertEquals("os-100", result.path("id").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve atualizar informacoes de diagnostico")
    void updateInformation_SendsPatch() {
        String findJson = "{\"id\":\"os-100\",\"mechanic\":{\"id\":\"mech-1\"}}";
        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(findJson, MediaType.APPLICATION_JSON));

        String updateResponse = "{\"id\":\"os-100\",\"informationText\":\"Texto diagnóstico\"}";
        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess(updateResponse, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.updateInformation("os-100", "Texto diagnóstico", "explicit-token");

        assertNotNull(result);
        assertEquals("Texto diagnóstico", result.path("informationText").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve iniciar e finalizar labor com PUT")
    void startAndFinishLabor_SendsPutRequests() {
        String responseJson = "{\"id\":\"os-100\"}";

        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100/start-labor/l-1"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100/finish-labor/l-1"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        JsonNode startResult = osServiceClient.startLabor("os-100", "l-1", "explicit-token");
        JsonNode finishResult = osServiceClient.finishLabor("os-100", "l-1", "explicit-token");

        assertNotNull(startResult);
        assertNotNull(finishResult);
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve atualizar status da OS")
    void updateStatus_SendsPatchWithQueryParam() {
        String responseJson = "{\"id\":\"os-100\",\"status\":\"EM_EXECUCAO\"}";

        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100/update-status/?status=EM_EXECUCAO"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        JsonNode result = osServiceClient.updateStatus("os-100", ServiceOrderStatus.EM_EXECUCAO, "explicit-token");

        assertNotNull(result);
        assertEquals("EM_EXECUCAO", result.path("status").asText());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve registrar monitoramento de labor extraindo datas de início e fim")
    void registerLaborMonitoring_ParsesValidDates() throws Exception {
        String json = """
                {
                  "labors": {
                    "laborsDetails": [
                      {
                        "laborId": "l-1",
                        "startDate": "2026-11-15T10:00:00",
                        "endDate": "2026-11-15T12:30:00"
                      }
                    ]
                  }
                }
                """;
        JsonNode orderNode = objectMapper.readTree(json);

        Optional<OsServiceClient.LaborTimes> times = osServiceClient.registerLaborMonitoring(orderNode, "l-1");

        assertTrue(times.isPresent());
        assertEquals(LocalDateTime.parse("2026-11-15T10:00:00"), times.get().start());
        assertEquals(LocalDateTime.parse("2026-11-15T12:30:00"), times.get().end());
    }

    @Test
    @DisplayName("Deve obter lista de suprimentos da OS")
    void getSupplies_ExtractsItems() {
        String json = """
                {
                  "id": "os-100",
                  "supplys": {
                    "supplysDetails": [
                      {"id": "sup-1", "quantity": 3},
                      {"id": "sup-2", "quantity": 0},
                      {"id": "", "quantity": 5}
                    ]
                  }
                }
                """;
        mockServer.expect(requestTo("http://localhost:8080/api/serviceorder/os-100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<StockItemRequest> supplies = osServiceClient.getSupplies("os-100", "explicit-token");

        assertEquals(1, supplies.size());
        assertEquals("sup-1", supplies.getFirst().supplyId());
        assertEquals(3, supplies.getFirst().quantity());
        mockServer.verify();
    }
}

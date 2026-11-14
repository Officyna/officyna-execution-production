package br.com.officyna_execution_production.serviceorder.service;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OsServiceClientTest {

    private MockRestServiceServer mockServer;
    private OsServiceClient osServiceClient;

    @BeforeEach
    void setUp() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
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
                new java.math.BigDecimal("100.00"),
                "explicit-token"
        );

        assertNotNull(result);
        assertEquals("os-100", result.path("id").asText());
        mockServer.verify();
    }
}

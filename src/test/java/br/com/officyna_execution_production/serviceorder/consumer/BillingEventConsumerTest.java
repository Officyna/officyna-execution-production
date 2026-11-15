package br.com.officyna_execution_production.serviceorder.consumer;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.event.BillingDecisionEvent;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BillingEventConsumerTest {

    @Mock
    private ServiceOrderExecutionService service;

    @InjectMocks
    private BillingEventConsumer consumer;

    @Test
    @DisplayName("Deve consumir evento de faturamento com correlationId explícito")
    void shouldConsumeWithExplicitCorrelationId() {
        BillingDecisionEvent event = new BillingDecisionEvent("os-1", "APROVADO");

        consumer.consume(event, "corr-123", null, "Bearer token");

        verify(service).receiveBillingDecision(event, "corr-123");
        assertNull(MDC.get(CorrelationConstants.CORRELATION_ID_KEY));
        assertNull(MDC.get(CorrelationConstants.AUTHORIZATION_KEY));
    }

    @Test
    @DisplayName("Deve consumir evento usando legacyCorrelationId quando correlationId for nulo")
    void shouldConsumeWithLegacyCorrelationId() {
        BillingDecisionEvent event = new BillingDecisionEvent("os-1", "RECUSADO");

        consumer.consume(event, null, "legacy-corr", null);

        verify(service).receiveBillingDecision(event, "legacy-corr");
        assertNull(MDC.get(CorrelationConstants.CORRELATION_ID_KEY));
    }

    @Test
    @DisplayName("Deve gerar correlationId aleatório quando nenhum header for informado")
    void shouldConsumeWithGeneratedCorrelationId() {
        BillingDecisionEvent event = new BillingDecisionEvent("os-1", "APROVADO");

        consumer.consume(event, null, null, null);

        verify(service).receiveBillingDecision(eq(event), org.mockito.ArgumentMatchers.anyString());
        assertNull(MDC.get(CorrelationConstants.CORRELATION_ID_KEY));
    }
}

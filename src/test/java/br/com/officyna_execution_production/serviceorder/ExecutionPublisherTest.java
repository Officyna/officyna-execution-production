package br.com.officyna_execution_production.serviceorder;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.event.ServiceOrderIdEvent;
import br.com.officyna_execution_production.serviceorder.service.ExecutionPublisher;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExecutionPublisherTest {

    private SqsTemplate sqs;
    private ExecutionPublisher publisher;

    @BeforeEach
    void setUp() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }

        sqs = mock(SqsTemplate.class);
        publisher = new ExecutionPublisher(sqs);
        ReflectionTestUtils.setField(publisher, "diagnosisQueue", "eventDiagnosis");
        ReflectionTestUtils.setField(publisher, "diagnosisCompletedQueue", "eventDiagnosisCompleted");
        ReflectionTestUtils.setField(publisher, "executionStartedQueue", "eventExecutionStarted");
        ReflectionTestUtils.setField(publisher, "completedQueue", "eventForOsCompleted");
        ReflectionTestUtils.setField(publisher, "diagnosticsDlqQueue", "eventForDiagnosticsDlq");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void completedDiagnosisSendsOnlyIdWithCorrelationHeader() {
        publisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent("os-1"), "corr-1");

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(sqs).send(eq("eventDiagnosisCompleted"), captor.capture());
        Message<?> message = captor.getValue();
        assertEquals(new ServiceOrderIdEvent("os-1"), message.getPayload());
        assertEquals("corr-1", message.getHeaders().get("correlationId"));
    }

    @Test
    void publishesDiagnosisEvent() {
        publisher.publish("eventDiagnosis", new ServiceOrderIdEvent("os-diag"), "corr-diag");

        verify(sqs).send(eq("eventDiagnosis"), any(Message.class));
    }

    @Test
    void publishesExecutionStartedEvent() {
        publisher.publish("eventExecutionStarted", new ServiceOrderIdEvent("os-start"), "corr-start");

        verify(sqs).send(eq("eventExecutionStarted"), any(Message.class));
    }

    @Test
    void publishesForOsCompletedEvent() {
        publisher.publish("eventForOsCompleted", new ServiceOrderIdEvent("os-finish"), "corr-finish");

        verify(sqs).send(eq("eventForOsCompleted"), any(Message.class));
    }

    @Test
    void throwsExceptionOnUnknownEventType() {
        assertThrows(IllegalArgumentException.class, () ->
                publisher.publish("unknownEvent", new ServiceOrderIdEvent("os-x"), "corr-x"));
    }

    @Test
    void publishesWithAuthorizationHeaderFromRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer request-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        publisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent("os-1"), "corr-1");

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(sqs).send(eq("eventDiagnosisCompleted"), captor.capture());
        assertEquals("Bearer request-token", captor.getValue().getHeaders().get(CorrelationConstants.AUTHORIZATION_HEADER));
    }

    @Test
    void publishesWithAuthorizationHeaderFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "sec-token", List.of())
        );

        publisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent("os-1"), "corr-1");

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(sqs).send(eq("eventDiagnosisCompleted"), captor.capture());
        assertEquals("Bearer sec-token", captor.getValue().getHeaders().get(CorrelationConstants.AUTHORIZATION_HEADER));
    }

    @Test
    void publishesWithAuthorizationHeaderWhenPresentInContext() {
        MDC.put(CorrelationConstants.AUTHORIZATION_KEY, "Bearer test-token-123");

        publisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent("os-1"), "corr-1");

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(sqs).send(eq("eventDiagnosisCompleted"), captor.capture());
        Message<?> message = captor.getValue();
        assertEquals("Bearer test-token-123", message.getHeaders().get(CorrelationConstants.AUTHORIZATION_HEADER));
        assertEquals("corr-1", message.getHeaders().get("correlationId"));
    }

    @Test
    void refusesToPublishWithoutCorrelationId() {
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent("os-1"), null));
    }

    @Test
    void publishesDiagnosticsDlqEventToConfiguredQueue() {
        publisher.publish("eventForDiagnosticsDlq", new ServiceOrderIdEvent("os-dlq"), "corr-dlq");

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(sqs).send(eq("eventForDiagnosticsDlq"), captor.capture());
        assertEquals(new ServiceOrderIdEvent("os-dlq"), captor.getValue().getPayload());
        assertEquals("corr-dlq", captor.getValue().getHeaders().get("correlationId"));
    }

    @Test
    void registersTransactionSynchronizationWhenTransactionIsActive() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            TransactionSynchronizationManager.setActualTransactionActive(true);

            publisher.publish("eventDiagnosis", new ServiceOrderIdEvent("os-tx"), "corr-tx");

            verify(sqs, never()).send(anyString(), any(Message.class));

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            verify(sqs).send(eq("eventDiagnosis"), any(Message.class));
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}

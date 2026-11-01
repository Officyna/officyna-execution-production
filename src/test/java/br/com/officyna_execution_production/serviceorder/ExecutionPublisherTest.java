package br.com.officyna_execution_production.serviceorder;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import br.com.officyna_execution_production.serviceorder.event.ServiceOrderIdEvent;
import br.com.officyna_execution_production.serviceorder.service.ExecutionPublisher;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExecutionPublisherTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void completedDiagnosisSendsOnlyIdWithCorrelationHeader() {
        SqsTemplate sqs = mock(SqsTemplate.class);
        ExecutionPublisher publisher = new ExecutionPublisher(sqs);
        ReflectionTestUtils.setField(publisher, "diagnosisCompletedQueue", "eventDiagnosisCompleted");

        publisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent("os-1"), "corr-1");

        var captor = org.mockito.ArgumentCaptor.forClass(Message.class);
        verify(sqs).send(eq("eventDiagnosisCompleted"), captor.capture());
        Message<?> message = captor.getValue();
        assertEquals(new ServiceOrderIdEvent("os-1"), message.getPayload());
        assertEquals("corr-1", message.getHeaders().get("correlationId"));
        verifyNoMoreInteractions(sqs);
    }

    @Test
    void publishesWithAuthorizationHeaderWhenPresentInContext() {
        MDC.put(CorrelationConstants.AUTHORIZATION_KEY, "Bearer test-token-123");
        SqsTemplate sqs = mock(SqsTemplate.class);
        ExecutionPublisher publisher = new ExecutionPublisher(sqs);
        ReflectionTestUtils.setField(publisher, "diagnosisCompletedQueue", "eventDiagnosisCompleted");

        publisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent("os-1"), "corr-1");

        var captor = org.mockito.ArgumentCaptor.forClass(Message.class);
        verify(sqs).send(eq("eventDiagnosisCompleted"), captor.capture());
        Message<?> message = captor.getValue();
        assertEquals("Bearer test-token-123", message.getHeaders().get(CorrelationConstants.AUTHORIZATION_HEADER));
        assertEquals("corr-1", message.getHeaders().get("correlationId"));
    }

    @Test
    void refusesToPublishWithoutCorrelationId() {
        ExecutionPublisher publisher = new ExecutionPublisher(mock(SqsTemplate.class));
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent("os-1"), null));
    }
}

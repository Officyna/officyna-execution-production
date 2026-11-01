package br.com.officyna_execution_production.serviceorder;

import br.com.officyna_execution_production.serviceorder.event.ServiceOrderIdEvent;
import br.com.officyna_execution_production.serviceorder.service.ExecutionPublisher;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExecutionPublisherTest {
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
    void refusesToPublishWithoutCorrelationId() {
        ExecutionPublisher publisher = new ExecutionPublisher(mock(SqsTemplate.class));
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent("os-1"), null));
    }
}

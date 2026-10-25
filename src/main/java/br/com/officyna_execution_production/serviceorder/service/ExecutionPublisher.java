package br.com.officyna_execution_production.serviceorder.service;

import org.springframework.messaging.support.MessageBuilder;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class ExecutionPublisher {
    private final SqsTemplate sqsTemplate;
    @Value("${aws.sqs.event-diagnosis}")
    private String diagnosisQueue;
    @Value("${aws.sqs.event-for-os-completed}")
    private String completedQueue;
    @Value("${aws.sqs.event-execution-started}")
    private String executionStartedQueue;
    @Value("${aws.sqs.event-diagnosis-completed}")
    private String diagnosisCompletedQueue;

    public void publish(String type, Object payload, String correlationId) {
        if (correlationId == null || correlationId.isBlank())
            throw new IllegalArgumentException("correlationId obrigatório para publicar eventos de execução");
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendToQueue(type, payload, correlationId);
                }
            });
            return;
        }
        sendToQueue(type, payload, correlationId);
    }

    private void sendToQueue(String type, Object payload, String correlationId) {
        var message = MessageBuilder.withPayload(payload).setHeader("correlationId", correlationId).build();
        switch (type) {
            case "eventDiagnosis" -> sqsTemplate.send(diagnosisQueue, message);
            case "eventDiagnosisCompleted" -> sqsTemplate.send(diagnosisCompletedQueue, message);
            case "eventExecutionStarted" -> sqsTemplate.send(executionStartedQueue, message);
            case "eventForOsCompleted" -> sqsTemplate.send(completedQueue, message);
            default -> throw new IllegalArgumentException("Evento desconhecido: " + type);
        }
    }
}

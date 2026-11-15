package br.com.officyna_execution_production.serviceorder.service;

import br.com.officyna_execution_production.infrastructure.logging.CorrelationConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.support.MessageBuilder;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
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
    @Value("${aws.sqs.event-for-diagnostics-dlq:eventForDiagnosticsDlq}")
    private String diagnosticsDlqQueue;

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
        var messageBuilder = MessageBuilder.withPayload(payload)
                .setHeader("correlationId", correlationId);

        String authorization = resolveAuthorizationHeader();
        if (authorization != null && !authorization.isBlank()) {
            messageBuilder.setHeader(CorrelationConstants.AUTHORIZATION_HEADER, authorization);
        }

        var message = messageBuilder.build();
        switch (type) {
            case "eventDiagnosis" -> sqsTemplate.send(diagnosisQueue, message);
            case "eventDiagnosisCompleted" -> sqsTemplate.send(diagnosisCompletedQueue, message);
            case "eventExecutionStarted" -> sqsTemplate.send(executionStartedQueue, message);
            case "eventForOsCompleted" -> sqsTemplate.send(completedQueue, message);
            case "eventForDiagnosticsDlq" -> sqsTemplate.send(diagnosticsDlqQueue, message);
            default -> throw new IllegalArgumentException("Evento desconhecido: " + type);
        }
    }

    private String resolveAuthorizationHeader() {
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
            log.debug("Could not resolve Authorization header for SQS publication: {}", e.getMessage());
        }
        return null;
    }
}

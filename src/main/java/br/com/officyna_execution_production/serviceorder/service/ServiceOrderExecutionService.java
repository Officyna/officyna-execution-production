package br.com.officyna_execution_production.serviceorder.service;

import br.com.officyna_execution_production.serviceorder.api.request.*;
import br.com.officyna_execution_production.serviceorder.domain.entity.*;
import br.com.officyna_execution_production.serviceorder.domain.enums.*;
import br.com.officyna_execution_production.serviceorder.domain.exception.*;
import br.com.officyna_execution_production.serviceorder.event.*;
import br.com.officyna_execution_production.serviceorder.domain.repository.ExecutionWorkRepository;
import br.com.officyna_execution_production.administrative.supply.domain.service.StockService;
import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;
import com.fasterxml.jackson.databind.JsonNode;
import br.com.officyna_execution_production.monitoring.domain.service.LaborMonitoringService;
import br.com.officyna_execution_production.infrastructure.security.AuthenticatedUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceOrderExecutionService {
    private final ExecutionWorkRepository executionRepository;
    private final StockService stockService;
    private final ExecutionPublisher executionPublisher;
    private final OsServiceClient osServiceClient;
    private final LaborMonitoringService laborMonitoringService;

    public ExecutionWork findByServiceOrderId(String serviceOrderId) {
        log.info("Finding service order by serviceOrderId: {}", serviceOrderId);
        return executionRepository.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> ServiceOrderNotFoundException.of(serviceOrderId));
    }

    public List<ExecutionWork> findAll() {
        return executionRepository.findAll().stream()
                .filter(serviceOrder -> Set.of(ServiceOrderStatus.RECEBIDA, ServiceOrderStatus.EM_DIAGNOSTICO,
                        ServiceOrderStatus.AGUARDANDO_APROVACAO, ServiceOrderStatus.APROVADA, ServiceOrderStatus.EM_EXECUCAO).contains(serviceOrder.getStatus()))
                .sorted(Comparator.comparingInt((ExecutionWork serviceOrder) -> serviceOrder.getStatus().getPriority()).thenComparing(ExecutionWork::getCreatedAt)).toList();
    }

    private ExecutionWork save(ExecutionWork serviceOrder) {
        serviceOrder.initializeStockReservationItems();
        serviceOrder.setUpdatedAt(LocalDateTime.now());
        return executionRepository.save(serviceOrder);
    }

    private void validateCurrentStatus(ExecutionWork serviceOrder, ServiceOrderStatus status) {
        if (serviceOrder.getStatus() != status)
            throw new ServiceOrderBusinessException("Transição inválida: esperado " + status + ", atual " + serviceOrder.getStatus());
    }

    private void changeStatus(ExecutionWork serviceOrder, ServiceOrderStatus next) {
        ServiceOrderStatus previous = serviceOrder.getStatus();
        LocalDateTime start = getStatusStartDate(serviceOrder, previous);
        serviceOrder.setStatus(next); // Same state-machine validations as the monolith.
        log.info("Service order status changed. event=SERVICE_ORDER_STATUS_CHANGED, serviceOrderId={}, previousStatus={}, status={}", serviceOrder.getServiceOrderId(), previous, next);
        LocalDateTime end = getStatusStartDate(serviceOrder, next);
        if (start != null && end != null) {
            long seconds = Duration.between(start, end).getSeconds();
            log.info("Service order status duration. event=SERVICE_ORDER_STATUS_DURATION, serviceOrderId={}, status={}, startDate={}, endDate={}, durationInSeconds={}, durationInMinutes={}", serviceOrder.getServiceOrderId(), previous, start, end, seconds, seconds / 60.0);
        }
    }

    private LocalDateTime getStatusStartDate(ExecutionWork serviceOrder, ServiceOrderStatus status) {
        if (status == null) return null;
        return switch (status) {
            case RECEBIDA -> serviceOrder.getRegistrationDate();
            case EM_DIAGNOSTICO -> serviceOrder.getDiagnosisStartDate();
            case AGUARDANDO_APROVACAO -> serviceOrder.getClientSendDate();
            case APROVADA -> serviceOrder.getApprovalDate();
            case EM_EXECUCAO -> serviceOrder.getExecutionStartDate();
            case FINALIZADA -> serviceOrder.getFinalizationDate();
            case RECUSADA -> serviceOrder.getRefuseDate();
            case ENTREGUE -> null;
        };
    }

    public void receiveServiceOrderForDiagnosis(OsSnapshotEvent event) {
        if (event == null || event.getServiceOrderId() == null || event.getServiceOrderId().isBlank()
                || event.getCorrelationId() == null || event.getCorrelationId().isBlank()
                || event.getStatus() != ServiceOrderStatus.RECEBIDA)
            throw new ServiceOrderBusinessException("eventForDiagnostics requer serviceOrderId, correlationId e status RECEBIDA");
        // OS ID is also Mongo _id, so concurrent deliveries cannot insert two snapshots.
        if (executionRepository.findByServiceOrderId(event.getServiceOrderId()).isPresent()) {
            log.info("Ignoring duplicate eventForDiagnostics. serviceOrderId={}", event.getServiceOrderId());
            return;
        }
        ExecutionWork serviceOrder = ExecutionWork.builder().id(event.getServiceOrderId()).serviceOrderId(event.getServiceOrderId())
                .createdAt(LocalDateTime.now()).build();
        serviceOrder.setCorrelationId(event.getCorrelationId());

        changeStatus(serviceOrder, ServiceOrderStatus.RECEBIDA);

        save(serviceOrder);
        log.info("Service order snapshot received. serviceOrderId={}, status=RECEBIDA", event.getServiceOrderId());
    }

    public void startDiagnosis(String serviceOrderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUserDetails mechanic))
            throw new ServiceOrderBusinessException("Usuário autenticado sem dados de mecânico");

        ExecutionWork serviceOrder = findByServiceOrderId(serviceOrderId);
        if (mechanic.getMechanicId() == null || mechanic.getMechanicId().isBlank())
            throw new ServiceOrderBusinessException("Mecânico obrigatório");
        changeStatus(serviceOrder, ServiceOrderStatus.EM_DIAGNOSTICO);
        serviceOrder.setMechanicId(mechanic.getMechanicId());
        serviceOrder.setMechanicName(mechanic.getMechanicName());
        save(serviceOrder);
        executionPublisher.publish("eventDiagnosis", new DiagnosisStartedEvent(serviceOrderId, ServiceOrderStatus.EM_DIAGNOSTICO,
                mechanic.getMechanicId(), mechanic.getMechanicName()), serviceOrder.getCorrelationId());
    }

    public JsonNode addLabor(String serviceOrderId, AddLaborRequest request, String token) {
        findByServiceOrderId(serviceOrderId);
        return osServiceClient.addLabor(serviceOrderId, request.laborId(), token);
    }

    public JsonNode removeLabor(String serviceOrderId, String laborId, String token) {
        findByServiceOrderId(serviceOrderId);
        return osServiceClient.removeLabor(serviceOrderId, laborId, token);
    }

    public JsonNode addSupply(String serviceOrderId, AddSupplyRequest request, String token) {
        findByServiceOrderId(serviceOrderId);
        return osServiceClient.addSupply(serviceOrderId, request.supplyId(), request.quantity(), token);
    }

    public JsonNode removeSupply(String serviceOrderId, String supplyId, String token) {
        findByServiceOrderId(serviceOrderId);
        return osServiceClient.removeSupply(serviceOrderId, supplyId, token);
    }

    private List<StockItemRequest> getStockItems(ExecutionWork serviceOrder) {
        return serviceOrder.getStockReservationItems() == null ? List.of() : serviceOrder.getStockReservationItems();
    }

    public JsonNode updateInformation(String serviceOrderId, String informationText, String token) {
        findByServiceOrderId(serviceOrderId);
        return osServiceClient.updateInformation(serviceOrderId, informationText, token);
    }

    public void submitForApproval(String serviceOrderId, String token) {
        ExecutionWork serviceOrder = findByServiceOrderId(serviceOrderId);
        changeStatus(serviceOrder, ServiceOrderStatus.AGUARDANDO_APROVACAO);
        List<StockItemRequest> supplies = osServiceClient.getSupplies(serviceOrderId, token);
        stockService.reserveSupplies(serviceOrderId, supplies);
        serviceOrder.setStockReservationItems(supplies);
        serviceOrder.setStockReserved(true);
        save(serviceOrder);
        executionPublisher.publish("eventDiagnosisCompleted", new ServiceOrderIdEvent(serviceOrderId), serviceOrder.getCorrelationId());
    }

    public void receiveBillingDecision(BillingDecisionEvent event, String correlationId) {
        ServiceOrderStatus paymentDecision = parsePaymentDecision(event == null ? null : event.getStatusPayment());
        if (event == null || event.getServiceOrderId() == null || event.getServiceOrderId().isBlank()
                || paymentDecision == null || correlationId == null || correlationId.isBlank())
            throw new ServiceOrderBusinessException("Evento de aprovação inválido");
        ExecutionWork serviceOrder = findByServiceOrderId(event.getServiceOrderId());
        if (hasAlreadyProcessedBillingDecision(serviceOrder, paymentDecision)) {
            log.info("Ignoring duplicate approval. serviceOrderId={}", event.getServiceOrderId());
            return;
        }
        validateCurrentStatus(serviceOrder, ServiceOrderStatus.AGUARDANDO_APROVACAO);
        changeStatus(serviceOrder, paymentDecision);
        serviceOrder.setCorrelationId(correlationId);
        if (paymentDecision == ServiceOrderStatus.RECUSADA && serviceOrder.isStockReserved()) {
            stockService.releaseSupplies(event.getServiceOrderId(), getStockItems(serviceOrder));
            serviceOrder.setStockReserved(false);
        }
        // Supplies remain order-level, exactly as in the monolith. No labor-to-supply association exists.
        save(serviceOrder);
    }

    private ServiceOrderStatus parsePaymentDecision(String statusPayment) {
        if (statusPayment == null) return null;
        try {
            ServiceOrderStatus decision = ServiceOrderStatus.valueOf(statusPayment.trim().toUpperCase(Locale.ROOT));
            return decision == ServiceOrderStatus.APROVADA || decision == ServiceOrderStatus.RECUSADA ? decision : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean hasAlreadyProcessedBillingDecision(ExecutionWork serviceOrder, ServiceOrderStatus decision) {
        boolean sameStatus = serviceOrder.getStatus() == decision;
        boolean executionAlreadyStarted = serviceOrder.getStatus() == ServiceOrderStatus.EM_EXECUCAO
                || serviceOrder.getStatus() == ServiceOrderStatus.FINALIZADA;
        boolean approvalAlreadyApplied = decision == ServiceOrderStatus.APROVADA
                && serviceOrder.getApprovalDate() != null && executionAlreadyStarted;
        return sameStatus || approvalAlreadyApplied;
    }

    private void validateStatusForLaborExecution(ExecutionWork serviceOrder) {
        if (serviceOrder.getStatus() != ServiceOrderStatus.APROVADA && serviceOrder.getStatus() != ServiceOrderStatus.EM_EXECUCAO)
            throw new ServiceOrderBusinessException("Um serviço só pode ser iniciado ou finalizado se o status da ordem de serviço for APROVADA ou EM EXECUÇÃO.");
    }

    private void startExecutionAndConsumeStock(ExecutionWork serviceOrder) {
        changeStatus(serviceOrder, ServiceOrderStatus.EM_EXECUCAO);
        if (serviceOrder.isStockReserved()) {
            stockService.consumeSupplies(serviceOrder.getServiceOrderId(), getStockItems(serviceOrder));
            serviceOrder.setStockConsumed(true);
            serviceOrder.setStockReserved(false);
        }
    }

    public void startServiceOrderExecution(String serviceOrderId, String token) {
        ExecutionWork serviceOrder = findByServiceOrderId(serviceOrderId);
        validateStatusForLaborExecution(serviceOrder);
        osServiceClient.updateStatus(serviceOrderId, ServiceOrderStatus.EM_EXECUCAO, token);
        startExecutionAndConsumeStock(serviceOrder);
        save(serviceOrder);
        executionPublisher.publish("eventExecutionStarted", new ExecutionStatusEvent(serviceOrderId, serviceOrder.getStatus()), serviceOrder.getCorrelationId());
    }

    public void startLabor(String serviceOrderId, String laborId, String token) {
        ExecutionWork serviceOrder = findByServiceOrderId(serviceOrderId);
        validateStatusForLaborExecution(serviceOrder);
        boolean startsServiceOrder = serviceOrder.getStatus() == ServiceOrderStatus.APROVADA;
        // os-service owns the status transition and starts the labor in one operation.
        osServiceClient.startLabor(serviceOrderId, laborId, token);
        if (startsServiceOrder) {
            startExecutionAndConsumeStock(serviceOrder);
            save(serviceOrder);
            executionPublisher.publish("eventExecutionStarted", new ExecutionStatusEvent(serviceOrderId, serviceOrder.getStatus()), serviceOrder.getCorrelationId());
        }
        log.info("Labor execution started. serviceOrderId={}, laborId={}", serviceOrderId, laborId);
    }

    public void finishLabor(String serviceOrderId, String laborId, String token) {
        ExecutionWork serviceOrder = findByServiceOrderId(serviceOrderId);
        validateStatusForLaborExecution(serviceOrder);
        JsonNode updatedOrder = osServiceClient.finishLabor(serviceOrderId, laborId, token);
        osServiceClient.registerLaborMonitoring(updatedOrder, laborId).ifPresent(times ->
                laborMonitoringService.registerExecution(serviceOrderId, laborId, times.start(), times.end()));
        log.info("Labor execution finished. serviceOrderId={}, laborId={}", serviceOrderId, laborId);
    }

    public void finishServiceOrderExecution(String serviceOrderId, String token) {
        ExecutionWork serviceOrder = findByServiceOrderId(serviceOrderId);
        validateCurrentStatus(serviceOrder, ServiceOrderStatus.EM_EXECUCAO);
        JsonNode osSnapshot = osServiceClient.findServiceOrder(serviceOrderId, token);
        JsonNode laborDetails = osSnapshot == null ? null : osSnapshot.path("labors").path("laborsDetails");
        if (laborDetails != null && laborDetails.isArray()) {
            for (JsonNode labor : laborDetails) {
                String endDate = labor.path("endDate").asText(null);
                if (!"REJEITADO".equals(labor.path("situation").asText())
                        && (endDate == null || endDate.isBlank()))
                    throw new ServiceOrderBusinessException("Não é possível finalizar ordem com serviços em aberto");
            }
        }
        osServiceClient.updateStatus(serviceOrderId, ServiceOrderStatus.FINALIZADA, token);
        changeStatus(serviceOrder, ServiceOrderStatus.FINALIZADA);
        if (serviceOrder.isStockReserved()) {
            stockService.releaseSupplies(serviceOrderId, getStockItems(serviceOrder));
            serviceOrder.setStockReserved(false);
        }
        save(serviceOrder);
        executionPublisher.publish("eventForOsCompleted", new ExecutionStatusEvent(serviceOrderId, serviceOrder.getStatus()), serviceOrder.getCorrelationId());
    }
}

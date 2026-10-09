package br.com.officyna_execution_production.serviceorder.service;

import br.com.officyna_execution_production.serviceorder.api.request.*;
import br.com.officyna_execution_production.serviceorder.domain.entity.*;
import br.com.officyna_execution_production.serviceorder.domain.enums.*;
import br.com.officyna_execution_production.serviceorder.domain.exception.ServiceOrderBusinessException;
import br.com.officyna_execution_production.serviceorder.event.*;
import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.document.ExecutionWorkDocument;
import br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.repository.ExecutionWorkMongoRepository;
import br.com.officyna_execution_production.monitoring.domain.service.LaborMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ServiceOrderExecutionService {
    private final ExecutionWorkMongoRepository repo;
    private final ExecutionPublisher publisher;
    private final LaborMonitoringService monitoring;
    private final Map<String, OsSnapshotEvent> snapshots = new java.util.concurrent.ConcurrentHashMap<>();

    private ExecutionWork from(ExecutionWorkDocument d) {
        return ExecutionWork.builder().id(d.getId()).serviceOrderId(d.getServiceOrderId()).mechanicId(d.getMechanicId()).labors(d.getLabors()).supplies(d.getSupplies()).createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).build();
    }

    private ExecutionWorkDocument doc(ExecutionWork w) {
        return ExecutionWorkDocument.builder().id(w.getId()).serviceOrderId(w.getServiceOrderId()).mechanicId(w.getMechanicId()).labors(w.getLabors()).supplies(w.getSupplies()).createdAt(w.getCreatedAt()).updatedAt(w.getUpdatedAt()).build();
    }

    private ExecutionWork get(String id) {
        return repo.findByServiceOrderId(id).map(this::from).orElseGet(() -> ExecutionWork.builder().serviceOrderId(id).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    private ExecutionWork save(ExecutionWork w) {
        return from(repo.save(doc(w)));
    }

    private void require(OsSnapshotEvent s, ServiceOrderStatus status) {
        if (s == null || s.getStatus() != status)
            throw new ServiceOrderBusinessException("Estado da OS não confirmado ou transição inválida: esperado " + status);
    }

    private ExecutionEvent event(String id, String type, ServiceOrderStatus expected, ServiceOrderStatus next) {
        return ExecutionEvent.builder().eventId(UUID.randomUUID().toString()).correlationId(UUID.randomUUID().toString()).serviceOrderId(id).type(type).expectedStatus(expected).newStatus(next).build();
    }

    public String requestDiagnosis(String id) {
        ExecutionEvent e = event(id, "FETCH_OS_FOR_DIAGNOSIS", ServiceOrderStatus.RECEBIDA, ServiceOrderStatus.EM_DIAGNOSTICO);
        publisher.toOs(e);
        return e.getCorrelationId();
    }

    public void onOsSnapshot(OsSnapshotEvent s) {
        if (s == null || s.getCorrelationId() == null) throw new ServiceOrderBusinessException("Resposta OS inválida");
        snapshots.put(s.getCorrelationId(), s);
    }

    public void confirmDiagnosis(String correlationId, String mechanicId) {
        OsSnapshotEvent s = snapshots.remove(correlationId);
        require(s, ServiceOrderStatus.RECEBIDA);
        if (mechanicId == null || mechanicId.isBlank()) throw new ServiceOrderBusinessException("Mecânico obrigatório");
        ExecutionWork w = get(s.getServiceOrderId());
        w.setMechanicId(mechanicId);
        save(w);
        ExecutionEvent e = event(s.getServiceOrderId(), "CHANGE_STATUS", ServiceOrderStatus.RECEBIDA, ServiceOrderStatus.EM_DIAGNOSTICO);
        e.setMechanicId(mechanicId);
        publisher.toOs(e);
    }

    public ExecutionWork addLabor(String id, AddLaborRequest r) {
        ExecutionWork w = get(id);
        w.addLabor(LaborExecution.builder().laborId(r.laborId()).name(r.name()).description(r.description()).price(r.price()).build());
        return save(w);
    }

    public ExecutionWork removeLabor(String id, String labor) {
        ExecutionWork w = get(id);
        w.removeLabor(labor);
        return save(w);
    }

    public ExecutionWork addSupply(String id, AddSupplyRequest r) {
        ExecutionWork w = get(id);
        w.addSupply(SupplyExecution.builder().supplyId(r.supplyId()).name(r.name()).description(r.description()).quantity(r.quantity()).unitPrice(r.unitPrice()).build());
        return save(w);
    }

    public ExecutionWork removeSupply(String id, String supply) {
        ExecutionWork w = get(id);
        w.removeSupply(supply);
        return save(w);
    }

    public void submitForApproval(String id) {
        ExecutionWork w = get(id);
        w.editable();
        if (w.getLabors().isEmpty()) throw new ServiceOrderBusinessException("Inclua pelo menos um serviço");
        ExecutionEvent e = event(id, "DIAGNOSIS_COMPLETED", ServiceOrderStatus.EM_DIAGNOSTICO, ServiceOrderStatus.AGUARDANDO_APROVACAO);
        e.setMechanicId(w.getMechanicId());
        e.setTotalAmount(w.total());
        e.setLabors(w.getLabors().stream().map(l -> LaborEvent.builder().laborId(l.getLaborId()).laborName(l.getName()).laborDescription(l.getDescription()).build()).toList());
        e.setSupplies(w.getSupplies().stream().map(s -> ExecutionEvent.SupplyItem.builder().supplyId(s.getSupplyId()).quantity(s.getQuantity()).unitPrice(s.getUnitPrice()).build()).toList());
        publisher.toOs(e);
        publisher.toBilling(e);
    }

    public void onBilling(BillingDecisionEvent e) {
        if (e == null || e.getServiceOrderId() == null || e.getStatus() == null)
            throw new ServiceOrderBusinessException("Evento Billing inválido");
        if (e.getStatus() != ServiceOrderStatus.APROVADA && e.getStatus() != ServiceOrderStatus.RECUSADA)
            throw new ServiceOrderBusinessException("Decisão inválida");
        ExecutionWork w = get(e.getServiceOrderId());
        w.editable();
        if (e.getLaborSituations() != null) for (LaborExecution l : w.getLabors()) {
            LaborSituation decision = e.getLaborSituations().get(l.getLaborId());
            if (decision != null) {
                l.setSituation(decision);
                l.setSituationDate(LocalDateTime.now());
            }
        }
        if (e.getStatus() == ServiceOrderStatus.APROVADA && w.getLabors().stream().noneMatch(l -> l.getSituation() == LaborSituation.APROVADO))
            throw new ServiceOrderBusinessException("Nenhum serviço aprovado");
        save(w);
        publisher.toOs(event(e.getServiceOrderId(), "BILLING_DECISION", ServiceOrderStatus.AGUARDANDO_APROVACAO, e.getStatus()));
    }

    public void startLabor(String id, String laborId) {
        ExecutionWork w = get(id);
        LaborExecution l = w.labor(laborId);
        if (l.getSituation() != LaborSituation.APROVADO || l.getStartDate() != null)
            throw new ServiceOrderBusinessException("Serviço não aprovado ou já iniciado");
        l.setStartDate(LocalDateTime.now());
        save(w);
        publisher.toOs(event(id, "START_LABOR", ServiceOrderStatus.APROVADA, ServiceOrderStatus.EM_EXECUCAO));
    }

    public void finishLabor(String id, String laborId) {
        ExecutionWork w = get(id);
        LaborExecution l = w.labor(laborId);
        if (l.getStartDate() == null || l.getEndDate() != null)
            throw new ServiceOrderBusinessException("Serviço não iniciado ou já finalizado");
        l.setEndDate(LocalDateTime.now());
        save(w);
        monitoring.registerExecution(id, laborId, l.getStartDate(), l.getEndDate());
    }

    public void finish(String id) {
        ExecutionWork w = get(id);
        w.editable();
        List<LaborExecution> approved = w.getLabors().stream().filter(l -> l.getSituation() == LaborSituation.APROVADO).toList();
        if (approved.isEmpty() || approved.stream().anyMatch(l -> !l.isFinished()))
            throw new ServiceOrderBusinessException("Todos os serviços aprovados devem ser finalizados");
        publisher.toOs(event(id, "FINISH_EXECUTION", ServiceOrderStatus.EM_EXECUCAO, ServiceOrderStatus.FINALIZADA));
    }
}

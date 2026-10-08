package br.com.officyna_execution_production.serviceorder.domain.entity;

import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import br.com.officyna_execution_production.serviceorder.domain.exception.ServiceOrderBusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderExecution {

    private String id;

    private String serviceOrderId;

    private Long serviceOrderNumber;

    private String customerId;

    private String vehicleId;

    private String mechanicId;

    private String mechanicName;

    @Builder.Default
    private List<LaborExecution> labors = new ArrayList<>();

    @Builder.Default
    private List<SupplyExecution> supplies = new ArrayList<>();

    private ServiceOrderStatus status;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime diagnosisStartDate;

    private LocalDateTime sentToApprovalAt;

    private LocalDateTime approvalDate;

    private LocalDateTime refuseDate;

    private LocalDateTime executionStartDate;

    private LocalDateTime finalizationDate;

    public void assignMechanic(
            String mechanicId,
            String mechanicName
    ) {

        if (mechanicId == null || mechanicId.isBlank()) {
            throw new ServiceOrderBusinessException(
                    "O mecânico deve ser informado."
            );
        }

        if (!ServiceOrderStatus.RECEBIDA.equals(status)
                && !ServiceOrderStatus.EM_DIAGNOSTICO.equals(status)) {

            throw new ServiceOrderBusinessException(
                    "O mecânico só pode ser atribuído antes ou durante o diagnóstico."
            );
        }

        this.mechanicId = mechanicId;
        this.mechanicName = mechanicName;
        touch();
    }

    public void startDiagnosis() {

        if (mechanicId == null || mechanicId.isBlank()) {
            throw new ServiceOrderBusinessException(
                    "É necessário atribuir um mecânico antes de iniciar o diagnóstico."
            );
        }

        setStatus(ServiceOrderStatus.EM_DIAGNOSTICO);
    }

    public void addLabor(LaborExecution labor) {

        validateCanEditDiagnosis();

        if (labor == null
                || labor.getLaborId() == null
                || labor.getLaborId().isBlank()) {

            throw new ServiceOrderBusinessException(
                    "O serviço informado é inválido."
            );
        }

        boolean alreadyExists = labors.stream()
                .anyMatch(item ->
                        item.getLaborId().equals(labor.getLaborId())
                );

        if (alreadyExists) {
            throw new ServiceOrderBusinessException(
                    "Este serviço já está cadastrado na Ordem de Serviço."
            );
        }

        labor.setSituation(
                br.com.officyna_execution_production.serviceorder.domain.enums.LaborSituation.PENDENTE
        );

        labor.setSituationDate(LocalDateTime.now());

        labors.add(labor);

        calculateBudget();
        touch();
    }

    public void removeLabor(String laborId) {

        validateCanEditDiagnosis();

        boolean removed = labors.removeIf(
                labor -> labor.getLaborId().equals(laborId)
        );

        if (!removed) {
            throw new ServiceOrderBusinessException(
                    "A O.S não possui este serviço."
            );
        }

        calculateBudget();
        touch();
    }

    public void addSupply(SupplyExecution supply) {

        validateCanEditDiagnosis();

        if (supply == null
                || supply.getSupplyId() == null
                || supply.getSupplyId().isBlank()) {

            throw new ServiceOrderBusinessException(
                    "O suprimento informado é inválido."
            );
        }

        if (supply.getQuantity() == null
                || supply.getQuantity() <= 0) {

            throw new ServiceOrderBusinessException(
                    "A quantidade do suprimento deve ser maior que zero."
            );
        }

        boolean alreadyExists = supplies.stream()
                .anyMatch(item ->
                        item.getSupplyId().equals(supply.getSupplyId())
                );

        if (alreadyExists) {
            throw new ServiceOrderBusinessException(
                    "Este suprimento já está cadastrado na Ordem de Serviço."
            );
        }

        supply.setTotalPrice(
                supply.getUnitPrice()
                        .multiply(BigDecimal.valueOf(supply.getQuantity()))
        );

        supplies.add(supply);

        calculateBudget();
        touch();
    }

    public void removeSupply(String supplyId) {

        validateCanEditDiagnosis();

        boolean removed = supplies.removeIf(
                supply -> supply.getSupplyId().equals(supplyId)
        );

        if (!removed) {
            throw new ServiceOrderBusinessException(
                    "A O.S não possui este suprimento."
            );
        }

        calculateBudget();
        touch();
    }

    public void submitForApproval() {

        if (!ServiceOrderStatus.EM_DIAGNOSTICO.equals(status)) {
            throw new ServiceOrderBusinessException(
                    "Somente uma O.S EM DIAGNÓSTICO pode ser enviada para aprovação."
            );
        }

        if (labors == null || labors.isEmpty()) {
            throw new ServiceOrderBusinessException(
                    "A Ordem de Serviço deve possuir pelo menos um serviço."
            );
        }

        calculateBudget();

        setStatus(ServiceOrderStatus.AGUARDANDO_APROVACAO);
    }

    public void approve() {

        setStatus(ServiceOrderStatus.APROVADA);
    }

    public void refuse() {

        setStatus(ServiceOrderStatus.RECUSADA);
    }

    public LaborExecution startLabor(String laborId) {

        validateStatusForLaborExecution();

        LaborExecution labor = findLabor(laborId);

        if (labor.getStartDate() != null) {
            throw new ServiceOrderBusinessException(
                    "O serviço já foi iniciado."
            );
        }

        if (labor.getSituation()
                != br.com.officyna_execution_production.serviceorder.domain.enums.LaborSituation.APROVADO) {

            throw new ServiceOrderBusinessException(
                    "Somente serviços aprovados podem ser iniciados."
            );
        }

        labor.setStartDate(LocalDateTime.now());

        if (ServiceOrderStatus.APROVADA.equals(status)) {
            setStatus(ServiceOrderStatus.EM_EXECUCAO);
        } else {
            touch();
        }

        return labor;
    }

    public LaborExecution finishLabor(String laborId) {

        validateStatusForLaborExecution();

        LaborExecution labor = findLabor(laborId);

        if (labor.getStartDate() == null) {
            throw new ServiceOrderBusinessException(
                    "Não é possível finalizar um serviço que não foi iniciado."
            );
        }

        if (labor.getEndDate() != null) {
            throw new ServiceOrderBusinessException(
                    "O serviço já foi finalizado."
            );
        }

        labor.setEndDate(LocalDateTime.now());

        touch();

        return labor;
    }

    public void finishExecution() {

        if (!ServiceOrderStatus.EM_EXECUCAO.equals(status)) {
            throw new ServiceOrderBusinessException(
                    "Apenas ordens EM EXECUÇÃO podem ser finalizadas."
            );
        }

        List<LaborExecution> approvedLabors = labors.stream()
                .filter(labor ->
                        br.com.officyna_execution_production.serviceorder.domain.enums.LaborSituation.APROVADO
                                .equals(labor.getSituation())
                )
                .toList();

        if (approvedLabors.isEmpty()) {
            throw new ServiceOrderBusinessException(
                    "A Ordem de Serviço não possui serviços aprovados."
            );
        }

        boolean hasUnfinishedLabor = approvedLabors.stream()
                .anyMatch(labor -> !labor.isFinished());

        if (hasUnfinishedLabor) {
            throw new ServiceOrderBusinessException(
                    "Todos os serviços aprovados devem ser finalizados antes de finalizar a O.S."
            );
        }

        setStatus(ServiceOrderStatus.FINALIZADA);
    }

    public void setStatus(ServiceOrderStatus newStatus) {

        if (newStatus == null) {
            throw new ServiceOrderBusinessException(
                    "O status não pode ser nulo."
            );
        }

        if (newStatus.equals(this.status)) {
            throw new ServiceOrderBusinessException(
                    "A Ordem de Serviço já foi processada com status "
                            + newStatus.getStatusName() + "."
            );
        }

        if (ServiceOrderStatus.RECEBIDA.equals(newStatus)) {

            throw new ServiceOrderBusinessException(
                    "A Ordem de Serviço já foi recebida e não pode retornar a este status."
            );
        }

        if (ServiceOrderStatus.EM_DIAGNOSTICO.equals(newStatus)
                && !ServiceOrderStatus.RECEBIDA.equals(this.status)) {

            throw new ServiceOrderBusinessException(
                    "Para iniciar o diagnóstico, a O.S. deve estar no status RECEBIDA."
            );
        }

        if (ServiceOrderStatus.AGUARDANDO_APROVACAO.equals(newStatus)
                && !ServiceOrderStatus.EM_DIAGNOSTICO.equals(this.status)) {

            throw new ServiceOrderBusinessException(
                    "Para aguardar aprovação, a O.S. deve ter passado pelo diagnóstico."
            );
        }

        if (ServiceOrderStatus.APROVADA.equals(newStatus)
                && !ServiceOrderStatus.AGUARDANDO_APROVACAO.equals(this.status)) {

            throw new ServiceOrderBusinessException(
                    "Apenas ordens AGUARDANDO APROVAÇÃO podem ser aprovadas."
            );
        }

        if (ServiceOrderStatus.RECUSADA.equals(newStatus)
                && !ServiceOrderStatus.AGUARDANDO_APROVACAO.equals(this.status)) {

            throw new ServiceOrderBusinessException(
                    "Apenas ordens AGUARDANDO APROVAÇÃO podem ser recusadas."
            );
        }

        if (ServiceOrderStatus.EM_EXECUCAO.equals(newStatus)
                && !ServiceOrderStatus.APROVADA.equals(this.status)) {

            throw new ServiceOrderBusinessException(
                    "Apenas ordens APROVADAS podem entrar em execução."
            );
        }

        if (ServiceOrderStatus.FINALIZADA.equals(newStatus)
                && !ServiceOrderStatus.EM_EXECUCAO.equals(this.status)) {

            throw new ServiceOrderBusinessException(
                    "Apenas ordens EM EXECUÇÃO podem ser finalizadas."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        switch (newStatus) {

            case EM_DIAGNOSTICO ->
                    this.diagnosisStartDate = now;

            case AGUARDANDO_APROVACAO ->
                    this.sentToApprovalAt = now;

            case APROVADA ->
                    this.approvalDate = now;

            case RECUSADA ->
                    this.refuseDate = now;

            case EM_EXECUCAO ->
                    this.executionStartDate = now;

            case FINALIZADA ->
                    this.finalizationDate = now;

            default -> {
            }
        }

        this.status = newStatus;
        this.updatedAt = now;
    }

    private LaborExecution findLabor(String laborId) {

        return labors.stream()
                .filter(labor ->
                        labor.getLaborId().equals(laborId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new ServiceOrderBusinessException(
                                "A O.S não possui este serviço."
                        )
                );
    }

    private void validateCanEditDiagnosis() {

        if (!ServiceOrderStatus.EM_DIAGNOSTICO.equals(status)) {

            throw new ServiceOrderBusinessException(
                    "Serviços e suprimentos só podem ser alterados durante o diagnóstico."
            );
        }
    }

    private void validateStatusForLaborExecution() {

        if (!ServiceOrderStatus.APROVADA.equals(status)
                && !ServiceOrderStatus.EM_EXECUCAO.equals(status)) {

            throw new ServiceOrderBusinessException(
                    "Um serviço só pode ser iniciado ou finalizado se a O.S estiver APROVADA ou EM EXECUÇÃO."
            );
        }

        if (labors == null || labors.isEmpty()) {

            throw new ServiceOrderBusinessException(
                    "A Ordem de Serviço não possui serviços cadastrados."
            );
        }
    }

    private void calculateBudget() {

        BigDecimal laborTotal = labors == null
                ? BigDecimal.ZERO
                : labors.stream()
                  .map(LaborExecution::getPrice)
                  .filter(price -> price != null)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal supplyTotal = supplies == null
                ? BigDecimal.ZERO
                : supplies.stream()
                  .map(SupplyExecution::getTotalPrice)
                  .filter(price -> price != null)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = laborTotal.add(supplyTotal);
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
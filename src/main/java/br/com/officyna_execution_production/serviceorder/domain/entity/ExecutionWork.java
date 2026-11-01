package br.com.officyna_execution_production.serviceorder.domain.entity;

import br.com.officyna_execution_production.serviceorder.domain.enums.*;
import br.com.officyna_execution_production.serviceorder.domain.exception.ServiceOrderBusinessException;
import lombok.*;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionWork {
    private String id;
    private String serviceOrderId;
    private String mechanicId;
    private String mechanicName;
    private String correlationId;
    private ServiceOrderStatus status;
    private LocalDateTime registrationDate, diagnosisStartDate, clientSendDate, approvalDate,
            executionStartDate, finalizationDate, refuseDate;
    private boolean stockReserved;
    private boolean stockConsumed;
    private List<StockItemRequest> stockReservationItems = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void initializeStockReservationItems() {
        if (stockReservationItems == null) stockReservationItems = new ArrayList<>();
    }
    public void setStatus(ServiceOrderStatus status){
        if (status == null) {
            this.status = null;
            return;
        }

        if (this.status == null) {
            recordStatusStartDate(status);
            this.status = status;
            return;
        }

        validateStatusTransition(status);
        recordStatusStartDate(status);
        this.status = status;
    }

    private void validateStatusTransition(ServiceOrderStatus nextStatus) {
        if (nextStatus == this.status) {
            throw new ServiceOrderBusinessException("A Ordem de Serviço já foi processada com status " + nextStatus.getStatusName() + ".");
        }

        switch (nextStatus) {
            case RECEBIDA -> throw new ServiceOrderBusinessException("A Ordem de Serviço já foi recebida e não pode retornar a este status.");
            case EM_DIAGNOSTICO -> validatePreviousStatus(ServiceOrderStatus.RECEBIDA,
                    "Para iniciar o diagnóstico, a O.S. deve estar no status RECEBIDA.");
            case AGUARDANDO_APROVACAO -> validatePreviousStatus(ServiceOrderStatus.EM_DIAGNOSTICO,
                    "Para aguardar aprovação, a O.S. deve ter passado pelo diagnóstico.");
            case APROVADA -> validatePreviousStatus(ServiceOrderStatus.AGUARDANDO_APROVACAO,
                    "Apenas ordens AGUARDANDO APROVAÇÃO podem ser aprovadas.");
            case EM_EXECUCAO -> validatePreviousStatus(ServiceOrderStatus.APROVADA,
                    "Apenas ordens APROVADAS podem entrar em execução.");
            case FINALIZADA -> validatePreviousStatus(ServiceOrderStatus.EM_EXECUCAO,
                    "Apenas ordens EM EXECUÇÃO podem ser finalizadas.");
            case ENTREGUE -> validatePreviousStatus(ServiceOrderStatus.FINALIZADA,
                    "Apenas ordes FINALIZADAS podem ser consideradas entregues");
            case RECUSADA -> validatePreviousStatus(ServiceOrderStatus.AGUARDANDO_APROVACAO,
                    "Apenas ordens AGUARDANDO APROVAÇÃO podem ser recusadas.");
        }
    }

    private void validatePreviousStatus(ServiceOrderStatus expectedStatus, String errorMessage) {
        if (this.status != expectedStatus) {
            throw new ServiceOrderBusinessException(errorMessage);
        }
    }
    private void recordStatusStartDate(ServiceOrderStatus status){
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case RECEBIDA -> this.setRegistrationDate(now);
            case EM_DIAGNOSTICO -> this.setDiagnosisStartDate(now);
            case AGUARDANDO_APROVACAO -> this.setClientSendDate(now);
            case APROVADA -> this.setApprovalDate(now);
            case EM_EXECUCAO -> this.setExecutionStartDate(now);
            case ENTREGUE -> throw new ServiceOrderBusinessException("Entrega pertence ao os-service");
            case FINALIZADA -> this.setFinalizationDate(now);
            case RECUSADA -> this.setRefuseDate(now);
        }
    }

}

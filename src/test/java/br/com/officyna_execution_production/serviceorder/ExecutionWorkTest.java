package br.com.officyna_execution_production.serviceorder;

import br.com.officyna_execution_production.serviceorder.domain.entity.ExecutionWork;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import br.com.officyna_execution_production.serviceorder.domain.exception.ServiceOrderBusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionWorkTest {
    @Test
    void lifecycleRecordsStatusDatesWithoutCopyingOsItems() {
        var work = new ExecutionWork();
        work.setStatus(ServiceOrderStatus.RECEBIDA);
        assertNotNull(work.getRegistrationDate());
        work.setStatus(ServiceOrderStatus.EM_DIAGNOSTICO);
        assertNotNull(work.getDiagnosisStartDate());
        work.setStatus(ServiceOrderStatus.AGUARDANDO_APROVACAO);
        assertNotNull(work.getClientSendDate());
        work.setStatus(ServiceOrderStatus.APROVADA);
        assertNotNull(work.getApprovalDate());
        work.setStatus(ServiceOrderStatus.EM_EXECUCAO);
        assertNotNull(work.getExecutionStartDate());
        work.setStatus(ServiceOrderStatus.FINALIZADA);
        assertNotNull(work.getFinalizationDate());
    }

    @Test
    void cannotSkipDiagnosis() {
        var work = ExecutionWork.builder().status(ServiceOrderStatus.RECEBIDA).build();
        assertThrows(ServiceOrderBusinessException.class,
                () -> work.setStatus(ServiceOrderStatus.AGUARDANDO_APROVACAO));
    }

    @Test
    void cannotStartBeforeApproval() {
        var work = ExecutionWork.builder().status(ServiceOrderStatus.EM_DIAGNOSTICO).build();
        assertThrows(ServiceOrderBusinessException.class,
                () -> work.setStatus(ServiceOrderStatus.EM_EXECUCAO));
    }

    @Test
    void duplicateStatusCommandIsRejected() {
        var work = ExecutionWork.builder().status(ServiceOrderStatus.EM_DIAGNOSTICO).build();
        assertThrows(ServiceOrderBusinessException.class,
                () -> work.setStatus(ServiceOrderStatus.EM_DIAGNOSTICO));
    }
}

package br.com.officyna_execution_production.serviceorder;

import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceOrderStatusTest {

    @Test
    void resolvesEveryConfiguredStatusById() {
        for (ServiceOrderStatus status : ServiceOrderStatus.values()) {
            assertEquals(status, ServiceOrderStatus.fromId(status.getIdStatus()));
        }
        assertThrows(IllegalArgumentException.class, () -> ServiceOrderStatus.fromId(99));
    }

    @Test
    void exposesQueuePriorityForActiveStatusesAndLowestPriorityForTerminalOnes() {
        assertEquals(1, ServiceOrderStatus.EM_EXECUCAO.getPriority());
        assertEquals(2, ServiceOrderStatus.AGUARDANDO_APROVACAO.getPriority());
        assertEquals(3, ServiceOrderStatus.APROVADA.getPriority());
        assertEquals(4, ServiceOrderStatus.EM_DIAGNOSTICO.getPriority());
        assertEquals(5, ServiceOrderStatus.RECEBIDA.getPriority());
        assertEquals(Integer.MAX_VALUE, ServiceOrderStatus.FINALIZADA.getPriority());
        assertEquals(Integer.MAX_VALUE, ServiceOrderStatus.ENTREGUE.getPriority());
        assertEquals(Integer.MAX_VALUE, ServiceOrderStatus.RECUSADA.getPriority());
    }
}

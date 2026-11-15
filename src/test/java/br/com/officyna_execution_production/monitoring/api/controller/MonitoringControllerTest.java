package br.com.officyna_execution_production.monitoring.api.controller;

import br.com.officyna_execution_production.monitoring.api.resources.ForceRecalcResponse;
import br.com.officyna_execution_production.monitoring.api.resources.LaborMonitoringResponse;
import br.com.officyna_execution_production.monitoring.domain.controller.MonitoringControllerAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringControllerTest {

    @Mock
    private MonitoringControllerAdapter adapter;

    @InjectMocks
    private MonitoringController controller;

    @Test
    @DisplayName("Deve buscar todos os monitoramentos de labor com sucesso")
    void shouldFindAll() {
        LaborMonitoringResponse item = new LaborMonitoringResponse(
                "labor-1",
                "Troca de óleo",
                "Descrição",
                1.5,
                "1d 4h",
                10,
                LocalDateTime.now()
        );
        when(adapter.findAll()).thenReturn(List.of(item));

        ResponseEntity<List<LaborMonitoringResponse>> response = controller.findAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("labor-1", response.getBody().getFirst().laborId());
        verify(adapter).findAll();
    }

    @Test
    @DisplayName("Deve forçar recálculo dos monitoramentos com sucesso")
    void shouldForceRecalc() {
        ForceRecalcResponse recalcResponse = new ForceRecalcResponse(5);
        when(adapter.forceRecalc()).thenReturn(recalcResponse);

        ResponseEntity<ForceRecalcResponse> response = controller.forceRecalc();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().laborsProcessed());
        verify(adapter).forceRecalc();
    }
}

package br.com.officyna_execution_production.serviceorder.controller;

import br.com.officyna_execution_production.serviceorder.api.request.AddLaborRequest;
import br.com.officyna_execution_production.serviceorder.api.request.AddSupplyRequest;
import br.com.officyna_execution_production.serviceorder.domain.entity.ExecutionWork;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderExecutionControllerTest {

    @Mock
    private ServiceOrderExecutionService executionService;

    @InjectMocks
    private ServiceOrderExecutionController controller;

    private ExecutionWork sampleWork;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        sampleWork = ExecutionWork.builder()
                .serviceOrderId("os-100")
                .status(ServiceOrderStatus.RECEBIDA)
                .build();
    }

    @Test
    @DisplayName("Deve listar todas as ordens de serviço")
    void shouldFindAll() {
        when(executionService.findAll()).thenReturn(List.of(sampleWork));

        List<ExecutionWork> result = controller.findAll();

        assertEquals(1, result.size());
        assertEquals("os-100", result.getFirst().getServiceOrderId());
        verify(executionService).findAll();
    }

    @Test
    @DisplayName("Deve buscar ordem de serviço por ID")
    void shouldFindByServiceOrderId() {
        when(executionService.findByServiceOrderId("os-100")).thenReturn(sampleWork);

        ExecutionWork result = controller.findByServiceOrderId("os-100");

        assertNotNull(result);
        assertEquals("os-100", result.getServiceOrderId());
        verify(executionService).findByServiceOrderId("os-100");
    }

    @Test
    @DisplayName("Deve iniciar diagnóstico com sucesso")
    void shouldStartDiagnosis() {
        ResponseEntity<Void> response = controller.startDiagnosis("os-100");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(executionService).startDiagnosis("os-100");
    }

    @Test
    @DisplayName("Deve iniciar execução da OS com sucesso")
    void shouldStartServiceOrderExecution() {
        ResponseEntity<Void> response = controller.startServiceOrderExecution("os-100", "Bearer token");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(executionService).startServiceOrderExecution("os-100", "Bearer token");
    }

    @Test
    @DisplayName("Deve adicionar labor à OS")
    void shouldAddLabor() {
        AddLaborRequest request = new AddLaborRequest("labor-1");
        JsonNode expectedNode = mapper.createObjectNode().put("success", true);
        when(executionService.addLabor("os-100", request, "Bearer token")).thenReturn(expectedNode);

        JsonNode result = controller.addLabor("os-100", request, "Bearer token");

        assertEquals(expectedNode, result);
        verify(executionService).addLabor("os-100", request, "Bearer token");
    }

    @Test
    @DisplayName("Deve remover labor da OS")
    void shouldRemoveLabor() {
        JsonNode expectedNode = mapper.createObjectNode().put("success", true);
        when(executionService.removeLabor("os-100", "labor-1", "Bearer token")).thenReturn(expectedNode);

        JsonNode result = controller.removeLabor("os-100", "labor-1", "Bearer token");

        assertEquals(expectedNode, result);
        verify(executionService).removeLabor("os-100", "labor-1", "Bearer token");
    }

    @Test
    @DisplayName("Deve adicionar supply à OS")
    void shouldAddSupply() {
        AddSupplyRequest request = new AddSupplyRequest("sup-1", 5);
        JsonNode expectedNode = mapper.createObjectNode().put("success", true);
        when(executionService.addSupply("os-100", request, "Bearer token")).thenReturn(expectedNode);

        JsonNode result = controller.addSupply("os-100", request, "Bearer token");

        assertEquals(expectedNode, result);
        verify(executionService).addSupply("os-100", request, "Bearer token");
    }

    @Test
    @DisplayName("Deve remover supply da OS")
    void shouldRemoveSupply() {
        JsonNode expectedNode = mapper.createObjectNode().put("success", true);
        when(executionService.removeSupply("os-100", "sup-1", "Bearer token")).thenReturn(expectedNode);

        JsonNode result = controller.removeSupply("os-100", "sup-1", "Bearer token");

        assertEquals(expectedNode, result);
        verify(executionService).removeSupply("os-100", "sup-1", "Bearer token");
    }

    @Test
    @DisplayName("Deve atualizar informações do diagnóstico")
    void shouldUpdateDiagnosisInformation() {
        ServiceOrderExecutionController.DiagnosisInformationRequest request =
                new ServiceOrderExecutionController.DiagnosisInformationRequest("Diagnóstico atualizado");
        JsonNode expectedNode = mapper.createObjectNode().put("success", true);
        when(executionService.updateInformation("os-100", "Diagnóstico atualizado", "Bearer token")).thenReturn(expectedNode);

        JsonNode result = controller.updateDiagnosisInformation("os-100", request, "Bearer token");

        assertEquals(expectedNode, result);
        verify(executionService).updateInformation("os-100", "Diagnóstico atualizado", "Bearer token");
    }

    @Test
    @DisplayName("Deve submeter para aprovação com status 202 ACCEPTED")
    void shouldSubmitForApproval() {
        ResponseEntity<Void> response = controller.submitForApproval("os-100", "Bearer token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(executionService).submitForApproval("os-100", "Bearer token");
    }

    @Test
    @DisplayName("Deve iniciar labor com status 202 ACCEPTED")
    void shouldStartLabor() {
        ResponseEntity<Void> response = controller.startLabor("os-100", "labor-1", "Bearer token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(executionService).startLabor("os-100", "labor-1", "Bearer token");
    }

    @Test
    @DisplayName("Deve finalizar labor com status 202 ACCEPTED")
    void shouldFinishLabor() {
        ResponseEntity<Void> response = controller.finishLabor("os-100", "labor-1", "Bearer token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(executionService).finishLabor("os-100", "labor-1", "Bearer token");
    }

    @Test
    @DisplayName("Deve finalizar execução da OS com status 202 ACCEPTED")
    void shouldFinishServiceOrderExecution() {
        ResponseEntity<Void> response = controller.finishServiceOrderExecution("os-100", "Bearer token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(executionService).finishServiceOrderExecution("os-100", "Bearer token");
    }
}

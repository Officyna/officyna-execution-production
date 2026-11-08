package br.com.officyna_execution_production.serviceorder.controller;

import br.com.officyna_execution_production.serviceorder.api.request.*;
import br.com.officyna_execution_production.serviceorder.domain.entity.ExecutionWork;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/execution")
@RequiredArgsConstructor
public class ServiceOrderExecutionController {
    private final ServiceOrderExecutionService executionService;

    @GetMapping
    public java.util.List<ExecutionWork> findAll() {
        return executionService.findAll();
    }

    @GetMapping("/{id}")
    public ExecutionWork findByServiceOrderId(@PathVariable("id") String serviceOrderId) {
        return executionService.findByServiceOrderId(serviceOrderId);
    }

    @PostMapping("/{id}/diagnosis/start")
    public ResponseEntity<Void> startDiagnosis(@PathVariable("id") String serviceOrderId) {
        executionService.startDiagnosis(serviceOrderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startServiceOrderExecution(@PathVariable("id") String serviceOrderId,
                                                           @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        executionService.startServiceOrderExecution(serviceOrderId, authorization);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/labors")
    public JsonNode addLabor(@PathVariable("id") String serviceOrderId, @Valid @RequestBody AddLaborRequest request,
                             @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return executionService.addLabor(serviceOrderId, request, authorization);
    }

    @DeleteMapping("/{id}/labors/{laborId}")
    public JsonNode removeLabor(@PathVariable("id") String serviceOrderId, @PathVariable String laborId,
                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return executionService.removeLabor(serviceOrderId, laborId, authorization);
    }

    @PostMapping("/{id}/supplies")
    public JsonNode addSupply(@PathVariable("id") String serviceOrderId, @Valid @RequestBody AddSupplyRequest request,
                              @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return executionService.addSupply(serviceOrderId, request, authorization);
    }

    @DeleteMapping("/{id}/supplies/{supplyId}")
    public JsonNode removeSupply(@PathVariable("id") String serviceOrderId, @PathVariable String supplyId,
                                 @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return executionService.removeSupply(serviceOrderId, supplyId, authorization);
    }

    public record DiagnosisInformationRequest(String informationText) {
    }

    @PutMapping("/{id}/information")
    public JsonNode updateDiagnosisInformation(@PathVariable("id") String serviceOrderId, @RequestBody DiagnosisInformationRequest request,
                                               @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return executionService.updateInformation(serviceOrderId, request.informationText(), authorization);
    }

    @PostMapping("/{id}/submit-for-approval")
    public ResponseEntity<Void> submitForApproval(@PathVariable("id") String serviceOrderId,
                                                  @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        executionService.submitForApproval(serviceOrderId, authorization);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/labors/{laborId}/start")
    public ResponseEntity<Void> startLabor(@PathVariable("id") String serviceOrderId, @PathVariable String laborId,
                                           @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        executionService.startLabor(serviceOrderId, laborId, authorization);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/labors/{laborId}/finish")
    public ResponseEntity<Void> finishLabor(@PathVariable("id") String serviceOrderId, @PathVariable String laborId,
                                            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        executionService.finishLabor(serviceOrderId, laborId, authorization);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<Void> finishServiceOrderExecution(@PathVariable("id") String serviceOrderId,
                                                            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        executionService.finishServiceOrderExecution(serviceOrderId, authorization);
        return ResponseEntity.accepted().build();
    }

}

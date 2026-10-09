package br.com.officyna_execution_production.serviceorder.controller;

import br.com.officyna_execution_production.serviceorder.api.request.*;
import br.com.officyna_execution_production.serviceorder.domain.entity.ExecutionWork;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/execution")
@RequiredArgsConstructor
public class ServiceOrderExecutionController {
    private final ServiceOrderExecutionService service;

    @PostMapping("/{id}/diagnosis/request")
    public ResponseEntity<String> request(@PathVariable String id) {
        return ResponseEntity.accepted().body(service.requestDiagnosis(id));
    }

    @PostMapping("/diagnosis/{correlationId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String correlationId, @RequestBody AssignMechanicRequest r) {
        service.confirmDiagnosis(correlationId, r.mechanicId());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/labors")
    public ExecutionWork addLabor(@PathVariable String id, @Valid @RequestBody AddLaborRequest r) {
        return service.addLabor(id, r);
    }

    @DeleteMapping("/{id}/labors/{laborId}")
    public ExecutionWork removeLabor(@PathVariable String id, @PathVariable String laborId) {
        return service.removeLabor(id, laborId);
    }

    @PostMapping("/{id}/supplies")
    public ExecutionWork addSupply(@PathVariable String id, @Valid @RequestBody AddSupplyRequest r) {
        return service.addSupply(id, r);
    }

    @DeleteMapping("/{id}/supplies/{supplyId}")
    public ExecutionWork removeSupply(@PathVariable String id, @PathVariable String supplyId) {
        return service.removeSupply(id, supplyId);
    }

    @PostMapping("/{id}/submit-for-approval")
    public ResponseEntity<Void> approval(@PathVariable String id) {
        service.submitForApproval(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/labors/{laborId}/start")
    public ResponseEntity<Void> start(@PathVariable String id, @PathVariable String laborId) {
        service.startLabor(id, laborId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/labors/{laborId}/finish")
    public ResponseEntity<Void> end(@PathVariable String id, @PathVariable String laborId) {
        service.finishLabor(id, laborId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<Void> finish(@PathVariable String id) {
        service.finish(id);
        return ResponseEntity.accepted().build();
    }
}

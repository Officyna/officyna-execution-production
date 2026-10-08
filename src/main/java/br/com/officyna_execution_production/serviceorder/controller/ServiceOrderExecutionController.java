package br.com.officyna_execution_production.serviceorder.controller;

import br.com.officyna_execution_production.serviceorder.domain.entity.ServiceOrderExecution;
import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;
import br.com.officyna_execution_production.serviceorder.service.ServiceOrderExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/execution/service-orders")
@RequiredArgsConstructor
public class ServiceOrderExecutionController {

    private final ServiceOrderExecutionService service;

    @GetMapping
    public ResponseEntity<List<ServiceOrderExecution>> findAll() {

        return ResponseEntity.ok(
                service.findAll()
        );
    }

    @GetMapping("/received")
    public ResponseEntity<List<ServiceOrderExecution>> findReceived() {

        return ResponseEntity.ok(
                service.findByStatus(ServiceOrderStatus.RECEBIDA)
        );
    }

    @PatchMapping("/{serviceOrderId}/diagnosis/start")
    public ResponseEntity<ServiceOrderExecution> startDiagnosis(
            @PathVariable String serviceOrderId
    ) {

        return ResponseEntity.ok(
                service.updateStatus(
                        serviceOrderId,
                        ServiceOrderStatus.EM_DIAGNOSTICO
                )
        );
    }

    @PatchMapping("/{serviceOrderId}/approval/wait")
    public ResponseEntity<ServiceOrderExecution> waitForApproval(
            @PathVariable String serviceOrderId
    ) {

        return ResponseEntity.ok(
                service.updateStatus(
                        serviceOrderId,
                        ServiceOrderStatus.AGUARDANDO_APROVACAO
                )
        );
    }

    @PatchMapping("/{serviceOrderId}/approve")
    public ResponseEntity<ServiceOrderExecution> approve(
            @PathVariable String serviceOrderId
    ) {

        return ResponseEntity.ok(
                service.updateStatus(
                        serviceOrderId,
                        ServiceOrderStatus.APROVADA
                )
        );
    }

    @PatchMapping("/{serviceOrderId}/reject")
    public ResponseEntity<ServiceOrderExecution> reject(
            @PathVariable String serviceOrderId
    ) {

        return ResponseEntity.ok(
                service.updateStatus(
                        serviceOrderId,
                        ServiceOrderStatus.RECUSADA
                )
        );
    }

    @PatchMapping("/{serviceOrderId}/execution/start")
    public ResponseEntity<ServiceOrderExecution> startExecution(
            @PathVariable String serviceOrderId
    ) {

        return ResponseEntity.ok(
                service.updateStatus(
                        serviceOrderId,
                        ServiceOrderStatus.EM_EXECUCAO
                )
        );
    }

    @PatchMapping("/{serviceOrderId}/finish")
    public ResponseEntity<ServiceOrderExecution> finish(
            @PathVariable String serviceOrderId
    ) {

        return ResponseEntity.ok(
                service.updateStatus(
                        serviceOrderId,
                        ServiceOrderStatus.FINALIZADA
                )
        );
    }
}
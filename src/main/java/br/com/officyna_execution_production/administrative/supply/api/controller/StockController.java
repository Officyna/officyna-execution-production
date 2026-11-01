package br.com.officyna_execution_production.administrative.supply.api.controller;

import br.com.officyna_execution_production.administrative.supply.api.resources.StockMovementRequest;
import br.com.officyna_execution_production.administrative.supply.domain.service.StockService;
import br.com.officyna_execution_production.administrative.supply.domain.entity.StockMovement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {
    private final StockService stock;

    @PostMapping("/service-orders/{serviceOrderId}/reserve")
    public ResponseEntity<Void> reserve(@PathVariable String serviceOrderId, @Valid @RequestBody StockMovementRequest request) {
        stock.reserveSupplies(serviceOrderId, request.supplies());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/service-orders/{serviceOrderId}/consume")
    public ResponseEntity<Void> consume(@PathVariable String serviceOrderId, @Valid @RequestBody StockMovementRequest request) {
        stock.consumeSupplies(serviceOrderId, request.supplies());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/service-orders/{serviceOrderId}/release")
    public ResponseEntity<Void> release(@PathVariable String serviceOrderId, @Valid @RequestBody StockMovementRequest request) {
        stock.releaseSupplies(serviceOrderId, request.supplies());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/service-orders/{serviceOrderId}/movements")
    public ResponseEntity<List<StockMovement>> history(@PathVariable String serviceOrderId) {
        return ResponseEntity.ok(stock.history(serviceOrderId));
    }
}

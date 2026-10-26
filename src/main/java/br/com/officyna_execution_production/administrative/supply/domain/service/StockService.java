package br.com.officyna_execution_production.administrative.supply.domain.service;

import br.com.officyna_execution_production.administrative.supply.domain.entity.Supply;
import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyBusinessException;
import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyNotFoundException;
import br.com.officyna_execution_production.administrative.supply.domain.repository.SupplyRepository;
import br.com.officyna_execution_production.administrative.supply.domain.entity.StockMovement;
import br.com.officyna_execution_production.administrative.supply.domain.repository.StockMovementRepository;
import br.com.officyna_execution_production.administrative.supply.api.resources.StockItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@org.springframework.transaction.annotation.Transactional
@Service
@RequiredArgsConstructor
public class StockService {
    private final SupplyRepository supplies;
    private final StockMovementRepository movements;

    public void reserveSupplies(String serviceOrderId, List<StockItemRequest> items) {
        move(serviceOrderId, items, "RESERVA");
    }

    public void consumeSupplies(String serviceOrderId, List<StockItemRequest> items) {
        move(serviceOrderId, items, "CONSUMO");
    }

    public void releaseSupplies(String serviceOrderId, List<StockItemRequest> items) {
        move(serviceOrderId, items, "LIBERACAO");
    }

    public List<StockMovement> history(String serviceOrderId) {
        return movements.findByServiceOrderIdOrderByCreatedAtDesc(serviceOrderId);
    }

    public void move(String serviceOrderId, List<StockItemRequest> items, String operation) {
        if (serviceOrderId == null || serviceOrderId.isBlank()) throw new SupplyBusinessException("Identificador da OS obrigatório");
        if (items == null || items.isEmpty()) return;
        for (StockItemRequest item : items) {
            if (item == null || item.supplyId() == null || item.supplyId().isBlank() || item.quantity() == null || item.quantity() <= 0)
                throw new SupplyBusinessException("Insumo e quantidade positiva são obrigatórios");
            Supply supply = supplies.findById(item.supplyId()).orElseThrow(() -> SupplyNotFoundException.of(item.supplyId()));
            int quantity = item.quantity();
            int stock = supply.getStockQuantity() == null ? 0 : supply.getStockQuantity();
            int reserved = supply.getReservedQuantity() == null ? 0 : supply.getReservedQuantity();
            switch (operation) {
                case "RESERVA" -> {
                    supply.setReservedQuantity(reserved + quantity);
                }
                case "CONSUMO" -> {
                    supply.setStockQuantity(stock - quantity);
                    supply.setReservedQuantity(Math.max(0, reserved - quantity));
                }
                case "LIBERACAO" -> {
                    supply.setReservedQuantity(Math.max(0, reserved - quantity));
                }
                default -> throw new SupplyBusinessException("Operação de estoque inválida: " + operation);
            }
            supplies.save(supply);
            movements.save(StockMovement.builder().serviceOrderId(serviceOrderId)
                .supplyId(item.supplyId()).operation(operation).quantity(quantity)
                .createdAt(LocalDateTime.now()).build());
            if (operation.equals("CONSUMO") && supply.getMinimumQuantity() != null && supply.getStockQuantity() < supply.getMinimumQuantity())
                log.warn("[ESTOQUE BAIXO] supplyId={}, stock={}, minimum={}", supply.getId(), supply.getStockQuantity(), supply.getMinimumQuantity());
            log.info("Stock movement. operation={}, serviceOrderId={}, supplyId={}, quantity={}", operation, serviceOrderId, item.supplyId(), quantity);
        }
    }
}

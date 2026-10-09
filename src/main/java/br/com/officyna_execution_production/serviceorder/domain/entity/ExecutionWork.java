package br.com.officyna_execution_production.serviceorder.domain.entity;

import br.com.officyna_execution_production.serviceorder.domain.enums.*;
import br.com.officyna_execution_production.serviceorder.domain.exception.ServiceOrderBusinessException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionWork {
    private String id;
    private String serviceOrderId;
    private String mechanicId;
    @Builder.Default
    private List<LaborExecution> labors = new ArrayList<>();
    @Builder.Default
    private List<SupplyExecution> supplies = new ArrayList<>();
    private LocalDateTime createdAt, updatedAt;

    public void editable() {
        if (labors == null) labors = new ArrayList<>();
        if (supplies == null) supplies = new ArrayList<>();
    }

    public void addLabor(LaborExecution labor) {
        editable();
        if (labors.stream().anyMatch(x -> x.getLaborId().equals(labor.getLaborId())))
            throw new ServiceOrderBusinessException("Serviço já incluído");
        labor.setSituation(LaborSituation.PENDENTE);
        labor.setSituationDate(LocalDateTime.now());
        labors.add(labor);
        updatedAt = LocalDateTime.now();
    }

    public void removeLabor(String id) {
        editable();
        if (!labors.removeIf(x -> x.getLaborId().equals(id)))
            throw new ServiceOrderBusinessException("Serviço não encontrado");
        updatedAt = LocalDateTime.now();
    }

    public void addSupply(SupplyExecution supply) {
        editable();
        if (supply.getQuantity() == null || supply.getQuantity() < 1 || supply.getUnitPrice() == null || supply.getUnitPrice().signum() < 0)
            throw new ServiceOrderBusinessException("Insumo inválido");
        if (supplies.stream().anyMatch(x -> x.getSupplyId().equals(supply.getSupplyId())))
            throw new ServiceOrderBusinessException("Insumo já incluído");
        supply.setTotalPrice(supply.getUnitPrice().multiply(BigDecimal.valueOf(supply.getQuantity())));
        supplies.add(supply);
        updatedAt = LocalDateTime.now();
    }

    public void removeSupply(String id) {
        editable();
        if (!supplies.removeIf(x -> x.getSupplyId().equals(id)))
            throw new ServiceOrderBusinessException("Insumo não encontrado");
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal total() {
        editable();
        return labors.stream().map(LaborExecution::getPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).add(supplies.stream().map(SupplyExecution::getTotalPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public LaborExecution labor(String id) {
        editable();
        return labors.stream().filter(x -> x.getLaborId().equals(id)).findFirst().orElseThrow(() -> new ServiceOrderBusinessException("Serviço não pertence à OS"));
    }
}

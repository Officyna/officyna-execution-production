package br.com.officyna_execution_production.administrative.supply.api.resources;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockItemRequest(@NotBlank String supplyId, @NotNull @Min(1) Integer quantity) {
}

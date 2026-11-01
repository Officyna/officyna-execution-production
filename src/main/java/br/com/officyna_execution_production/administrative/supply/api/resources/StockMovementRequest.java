package br.com.officyna_execution_production.administrative.supply.api.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record StockMovementRequest(@NotEmpty List<@Valid StockItemRequest> supplies) {}

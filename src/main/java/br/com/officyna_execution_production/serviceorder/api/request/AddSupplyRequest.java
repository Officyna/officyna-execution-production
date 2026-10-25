package br.com.officyna_execution_production.serviceorder.api.request;

import jakarta.validation.constraints.*;

public record AddSupplyRequest(@NotBlank String supplyId, @NotNull @Min(1) Integer quantity) {
}

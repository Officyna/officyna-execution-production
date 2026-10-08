package br.com.officyna_execution_production.serviceorder.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddSupplyRequest(

        @NotBlank
        String supplyId,

        @NotBlank
        String name,

        String description,

        @NotNull
        @Min(1)
        Integer quantity,

        @NotNull
        BigDecimal unitPrice
) {
}
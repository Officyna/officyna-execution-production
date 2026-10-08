package br.com.officyna_execution_production.serviceorder.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddLaborRequest(

        @NotBlank
        String laborId,

        @NotBlank
        String name,

        String description,

        @NotNull
        BigDecimal price
) {
}
package br.com.officyna_execution_production.serviceorder.api.request;

import jakarta.validation.constraints.NotBlank;

public record AssignMechanicRequest(

        @NotBlank
        String mechanicId,

        @NotBlank
        String mechanicName
) {
}
package br.com.officyna_execution_production.serviceorder.api.request;

import jakarta.validation.constraints.NotBlank;

public record AddLaborRequest(@NotBlank String laborId) {
}

package br.com.officyna_execution_production.serviceorder.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceOrderStatus {

    RECEBIDA("Recebida"),
    EM_DIAGNOSTICO("Em diagnóstico"),
    AGUARDANDO_APROVACAO("Aguardando aprovação"),
    APROVADA("Aprovada"),
    EM_EXECUCAO("Em execução"),
    FINALIZADA("Finalizada"),
    RECUSADA("Recusada");

    private final String statusName;
}
package br.com.officyna_execution_production.serviceorder.event;

import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;

public record ExecutionStatusEvent(String serviceOrderId, ServiceOrderStatus status) {}

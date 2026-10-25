package br.com.officyna_execution_production.serviceorder.event;

import br.com.officyna_execution_production.serviceorder.domain.enums.ServiceOrderStatus;

public record DiagnosisStartedEvent(String serviceOrderId, ServiceOrderStatus status,
                                   String mechanicId, String mechanicName) {}

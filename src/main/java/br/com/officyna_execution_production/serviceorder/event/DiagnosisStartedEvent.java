package br.com.officyna_execution_production.serviceorder.event;

public record DiagnosisStartedEvent(String serviceOrderId, String mechanicId, String mechanicName) {}

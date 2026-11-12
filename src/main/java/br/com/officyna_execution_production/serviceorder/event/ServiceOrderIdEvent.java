package br.com.officyna_execution_production.serviceorder.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceOrderIdEvent(String serviceOrderId) {}

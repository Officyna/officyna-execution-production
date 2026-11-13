package br.com.officyna_execution_production.serviceorder.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiagnosisStartedEvent(
        String serviceOrderId,
        @JsonAlias("mechanichId") String mechanicId,
        String mechanicName
) {}

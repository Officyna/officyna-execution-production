package br.com.officyna_execution_production.serviceorder.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaborEvent {

    private String laborId;

    private String laborName;

    private String laborDescription;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer estimatedDays;

    private String status;
}
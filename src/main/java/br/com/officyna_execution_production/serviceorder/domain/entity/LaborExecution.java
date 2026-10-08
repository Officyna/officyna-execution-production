package br.com.officyna_execution_production.serviceorder.domain.entity;

import br.com.officyna_execution_production.serviceorder.domain.enums.LaborSituation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaborExecution {

    private String laborId;

    private String name;

    private String description;

    private BigDecimal price;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LaborSituation situation;

    private LocalDateTime situationDate;

    public boolean isFinished() {
        return startDate != null && endDate != null;
    }
}
package br.com.officyna_execution_production.monitoring.domain.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaborExecution {

    private String id;

    /**
     * Identifica a OS à qual essa execução pertence.
     */
    private String serviceOrderId;

    /**
     * Identifica o labor executado.
     */
    private String laborId;

    /**
     * Momento em que a execução começou.
     */
    private LocalDateTime startDate;

    /**
     * Momento em que a execução terminou.
     */
    private LocalDateTime endDate;
}
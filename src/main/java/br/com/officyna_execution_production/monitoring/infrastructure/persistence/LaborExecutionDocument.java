package br.com.officyna_execution_production.monitoring.infrastructure.persistence;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("labor_executions")
public class LaborExecutionDocument {
    @Id
    private String id;
    private String serviceOrderId, laborId;
    private LocalDateTime startDate, endDate;
}

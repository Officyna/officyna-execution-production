package br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.document;

import br.com.officyna_execution_production.serviceorder.domain.entity.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("execution_work")
public class ExecutionWorkDocument {
    @Id
    private String id;
    @Indexed(unique = true)
    private String serviceOrderId;
    private String mechanicId;
    private List<LaborExecution> labors;
    private List<SupplyExecution> supplies;
    private LocalDateTime createdAt, updatedAt;
}

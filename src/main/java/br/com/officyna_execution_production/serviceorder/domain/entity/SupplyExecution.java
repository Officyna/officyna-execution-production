package br.com.officyna_execution_production.serviceorder.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyExecution {

    private String supplyId;

    private String name;

    private String description;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;
}
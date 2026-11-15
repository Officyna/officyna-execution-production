package br.com.officyna_execution_production.administrative.labor.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LaborTest {

    @Test
    @DisplayName("Deve construir e manipular atributos de Labor")
    void shouldCreateAndMutateLabor() {
        Labor labor = Labor.builder()
                .id("l-1")
                .name("Alinhamento")
                .description("Alinhamento e balanceamento")
                .price(BigDecimal.valueOf(120.00))
                .executionTimeInDays(1)
                .active(true)
                .build();

        assertEquals("l-1", labor.getId());
        assertEquals("Alinhamento", labor.getName());
        assertEquals("Alinhamento e balanceamento", labor.getDescription());
        assertEquals(BigDecimal.valueOf(120.00), labor.getPrice());
        assertEquals(1, labor.getExecutionTimeInDays());
        assertTrue(labor.getActive());

        labor.setName("Alinhamento 3D");
        labor.setActive(false);

        assertEquals("Alinhamento 3D", labor.getName());
        assertFalse(labor.getActive());

        Labor noArgLabor = new Labor();
        assertNotNull(noArgLabor);
    }
}

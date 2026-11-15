package br.com.officyna_execution_production.administrative.supply.api.controller;

import br.com.officyna_execution_production.administrative.supply.api.resources.SupplyRequest;
import br.com.officyna_execution_production.administrative.supply.api.resources.SupplyResponse;
import br.com.officyna_execution_production.administrative.supply.domain.controller.SupplyControllerAdapter;
import br.com.officyna_execution_production.administrative.supply.domain.entity.SupplyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplyControllerTest {

    @Mock
    private SupplyControllerAdapter supplyControllerAdapter;

    @InjectMocks
    private SupplyController controller;

    private SupplyResponse sampleResponse;
    private SupplyRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new SupplyResponse(
                "sup-1",
                "Óleo Motor 5W30",
                "Óleo sintético",
                SupplyType.SUPPLY,
                BigDecimal.valueOf(45.90),
                BigDecimal.valueOf(60.00),
                BigDecimal.valueOf(30.72),
                50,
                10,
                5,
                45,
                false,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        sampleRequest = new SupplyRequest(
                "Óleo Motor 5W30",
                "Óleo sintético",
                SupplyType.SUPPLY,
                BigDecimal.valueOf(45.90),
                BigDecimal.valueOf(30.72),
                50,
                10,
                5
        );
    }

    @Test
    @DisplayName("Deve buscar todos os supplies com sucesso")
    void shouldFindAll() {
        when(supplyControllerAdapter.findAll()).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<SupplyResponse>> response = controller.findAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(supplyControllerAdapter).findAll();
    }

    @Test
    @DisplayName("Deve buscar supplies por tipo com sucesso")
    void shouldFindByType() {
        when(supplyControllerAdapter.findByType(SupplyType.SUPPLY)).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<SupplyResponse>> response = controller.findByType(SupplyType.SUPPLY);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(supplyControllerAdapter).findByType(SupplyType.SUPPLY);
    }

    @Test
    @DisplayName("Deve buscar supply por ID com sucesso")
    void shouldFindById() {
        when(supplyControllerAdapter.findById("sup-1")).thenReturn(sampleResponse);

        ResponseEntity<SupplyResponse> response = controller.findById("sup-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(supplyControllerAdapter).findById("sup-1");
    }

    @Test
    @DisplayName("Deve criar supply retornando status 201 CREATED")
    void shouldCreate() {
        when(supplyControllerAdapter.create(sampleRequest)).thenReturn(sampleResponse);

        ResponseEntity<SupplyResponse> response = controller.create(sampleRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(supplyControllerAdapter).create(sampleRequest);
    }

    @Test
    @DisplayName("Deve atualizar supply com sucesso")
    void shouldUpdate() {
        when(supplyControllerAdapter.update("sup-1", sampleRequest)).thenReturn(sampleResponse);

        ResponseEntity<SupplyResponse> response = controller.update("sup-1", sampleRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(supplyControllerAdapter).update("sup-1", sampleRequest);
    }

    @Test
    @DisplayName("Deve deletar supply retornando status 204 NO_CONTENT")
    void shouldDelete() {
        ResponseEntity<Void> response = controller.delete("sup-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(supplyControllerAdapter).delete("sup-1");
    }
}

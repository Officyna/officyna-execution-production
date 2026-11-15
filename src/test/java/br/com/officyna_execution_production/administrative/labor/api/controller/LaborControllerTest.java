package br.com.officyna_execution_production.administrative.labor.api.controller;

import br.com.officyna_execution_production.administrative.labor.api.resources.LaborRequest;
import br.com.officyna_execution_production.administrative.labor.api.resources.LaborResponse;
import br.com.officyna_execution_production.administrative.labor.domain.controller.LaborControllerAdapter;
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
class LaborControllerTest {

    @Mock
    private LaborControllerAdapter laborControllerAdapter;

    @InjectMocks
    private LaborController controller;

    private LaborResponse sampleResponse;
    private LaborRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new LaborResponse(
                "labor-1",
                "Troca de óleo",
                "Troca de óleo do motor",
                BigDecimal.valueOf(150.0),
                1,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
        );
        sampleRequest = new LaborRequest(
                "Troca de óleo",
                "Troca de óleo do motor",
                BigDecimal.valueOf(150.0),
                1,
                true
        );
    }

    @Test
    @DisplayName("Deve buscar todos os labors com sucesso")
    void shouldFindAll() {
        when(laborControllerAdapter.findAll()).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<LaborResponse>> response = controller.findAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(laborControllerAdapter).findAll();
    }

    @Test
    @DisplayName("Deve buscar labor por ID com sucesso")
    void shouldFindById() {
        when(laborControllerAdapter.findById("labor-1")).thenReturn(sampleResponse);

        ResponseEntity<LaborResponse> response = controller.findById("labor-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(laborControllerAdapter).findById("labor-1");
    }

    @Test
    @DisplayName("Deve criar labor retornando status 201 CREATED")
    void shouldCreate() {
        when(laborControllerAdapter.create(sampleRequest)).thenReturn(sampleResponse);

        ResponseEntity<LaborResponse> response = controller.create(sampleRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(laborControllerAdapter).create(sampleRequest);
    }

    @Test
    @DisplayName("Deve atualizar labor com sucesso")
    void shouldUpdate() {
        when(laborControllerAdapter.update("labor-1", sampleRequest)).thenReturn(sampleResponse);

        ResponseEntity<LaborResponse> response = controller.update("labor-1", sampleRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(laborControllerAdapter).update("labor-1", sampleRequest);
    }

    @Test
    @DisplayName("Deve deletar labor retornando status 204 NO_CONTENT")
    void shouldDelete() {
        ResponseEntity<Void> response = controller.delete("labor-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(laborControllerAdapter).delete("labor-1");
    }
}

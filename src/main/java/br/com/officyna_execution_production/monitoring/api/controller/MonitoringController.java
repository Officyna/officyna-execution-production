package br.com.officyna_execution_production.monitoring.api.controller;

import br.com.officyna_execution_production.monitoring.api.MonitoringApi;
import br.com.officyna_execution_production.monitoring.api.resources.ForceRecalcResponse;
import br.com.officyna_execution_production.monitoring.api.resources.LaborMonitoringResponse;
import br.com.officyna_execution_production.monitoring.domain.controller.MonitoringControllerAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MonitoringController implements MonitoringApi {

    private final MonitoringControllerAdapter monitoringControllerAdapter;

    @Override
    public ResponseEntity<List<LaborMonitoringResponse>> findAll() {
        return ResponseEntity.ok(monitoringControllerAdapter.findAll());
    }

    @Override
    public ResponseEntity<ForceRecalcResponse> forceRecalc() {
        return ResponseEntity.ok(monitoringControllerAdapter.forceRecalc());
    }
}
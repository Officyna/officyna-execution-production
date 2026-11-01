package br.com.officyna_execution_production.infrastructure.persistence.config;

import br.com.officyna_execution_production.administrative.labor.domain.controller.LaborControllerAdapter;
import br.com.officyna_execution_production.administrative.labor.domain.mapper.LaborMapper;
import br.com.officyna_execution_production.administrative.labor.domain.presenter.LaborPresenter;
import br.com.officyna_execution_production.administrative.labor.domain.repository.LaborRepository;
import br.com.officyna_execution_production.administrative.labor.domain.service.LaborService;
import br.com.officyna_execution_production.administrative.supply.domain.controller.SupplyControllerAdapter;
import br.com.officyna_execution_production.administrative.supply.domain.mapper.SupplyMapper;
import br.com.officyna_execution_production.administrative.supply.domain.presenter.SupplyPresenter;
import br.com.officyna_execution_production.administrative.supply.domain.repository.SupplyRepository;
import br.com.officyna_execution_production.administrative.supply.domain.service.SupplyService;
import br.com.officyna_execution_production.monitoring.domain.controller.MonitoringControllerAdapter;
import br.com.officyna_execution_production.monitoring.domain.presenter.LaborMonitoringPresenter;
import br.com.officyna_execution_production.monitoring.domain.repository.LaborExecutionRepository;
import br.com.officyna_execution_production.monitoring.domain.repository.LaborMonitoringRepository;
import br.com.officyna_execution_production.monitoring.domain.service.LaborMonitoringService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistenceBeanConfig {
    @Bean
    SupplyService supplyService(SupplyRepository supplyRepository) {
        return new SupplyService(supplyRepository);
    }

    @Bean
    SupplyControllerAdapter supplyControllerAdapter(
            SupplyService supplyService,
            SupplyMapper supplyMapper,
            SupplyPresenter supplyPresenter) {
        return new SupplyControllerAdapter(supplyService, supplyMapper, supplyPresenter);
    }

    @Bean
    LaborMonitoringService laborMonitoringService(
            LaborMonitoringRepository monitoringRepository,
            LaborRepository laborRepository,
            LaborExecutionRepository executionRepository) {
        return new LaborMonitoringService(monitoringRepository, laborRepository, executionRepository);
    }

    @Bean
    LaborService laborService(LaborRepository laborRepository, LaborMonitoringService monitoringService) {
        return new LaborService(laborRepository, monitoringService);
    }

    @Bean
    LaborControllerAdapter laborControllerAdapter(
            LaborService laborService,
            LaborMapper laborMapper,
            LaborPresenter laborPresenter) {
        return new LaborControllerAdapter(laborService, laborMapper, laborPresenter);
    }

    @Bean
    MonitoringControllerAdapter monitoringControllerAdapter(
            LaborMonitoringService monitoringService,
            LaborMonitoringPresenter monitoringPresenter) {
        return new MonitoringControllerAdapter(monitoringService, monitoringPresenter);
    }
}

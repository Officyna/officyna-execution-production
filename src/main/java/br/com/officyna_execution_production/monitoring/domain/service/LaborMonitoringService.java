package br.com.officyna_execution_production.monitoring.domain.service;

import br.com.officyna_execution_production.administrative.labor.domain.entity.Labor;
import br.com.officyna_execution_production.administrative.labor.domain.repository.LaborRepository;
import br.com.officyna_execution_production.monitoring.domain.entity.LaborExecution;
import br.com.officyna_execution_production.monitoring.domain.entity.LaborMonitoring;
import br.com.officyna_execution_production.monitoring.domain.repository.LaborExecutionRepository;
import br.com.officyna_execution_production.monitoring.domain.repository.LaborMonitoringRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class LaborMonitoringService {

    private static final double WORK_DAY_SECONDS_DOUBLE = 28800.0;

    private final LaborMonitoringRepository repository;
    private final LaborRepository laborRepository;
    private final LaborExecutionRepository laborExecutionRepository;

    public LaborMonitoringService(
            LaborMonitoringRepository monitoringRepository,
            LaborRepository laborRepository,
            LaborExecutionRepository laborExecutionRepository) {

        this.repository = monitoringRepository;
        this.laborRepository = laborRepository;
        this.laborExecutionRepository = laborExecutionRepository;
    }

    public List<LaborMonitoring> findAll() {

        log.info("Finding all labor monitoring records");

        List<LaborMonitoring> monitoring = repository.findAll();

        log.info(
                "Labor monitoring records found: {}",
                monitoring.size()
        );

        return monitoring;
    }

    /**
     * Registra uma execução finalizada e atualiza o monitoramento
     * do labor.
     */
    public void registerExecution(
            String serviceOrderId,
            String laborId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info(
                "Registering labor execution. serviceOrderId={}, laborId={}, startDate={}, endDate={}",
                serviceOrderId,
                laborId,
                startDate,
                endDate
        );

        if (startDate == null || endDate == null) {

            log.warn(
                    "Ignoring labor execution because startDate or endDate is null. " +
                            "serviceOrderId={}, laborId={}",
                    serviceOrderId,
                    laborId
            );

            return;
        }

        double durationInDays = calculateDurationInDays(
                startDate,
                endDate
        );

        if (durationInDays < 0) {

            log.warn(
                    "Ignoring labor execution because endDate is before startDate. " +
                            "serviceOrderId={}, laborId={}",
                    serviceOrderId,
                    laborId
            );

            return;
        }

        /*
         * SQS pode entregar a mesma mensagem mais de uma vez.
         * Antes de salvar, verificamos se essa execução já foi registrada.
         */
        Optional<LaborExecution> existingExecution =
                laborExecutionRepository
                        .findByServiceOrderIdAndLaborId(
                                serviceOrderId,
                                laborId
                        );

        if (existingExecution.isPresent()) {

            log.info(
                    "Labor execution already registered. " +
                            "Ignoring duplicate event. serviceOrderId={}, laborId={}",
                    serviceOrderId,
                    laborId
            );

            return;
        }

        LaborExecution execution = LaborExecution.builder()
                .serviceOrderId(serviceOrderId)
                .laborId(laborId)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        laborExecutionRepository.save(execution);

        updateMonitoring(
                laborId,
                durationInDays
        );

        log.info(
                "Labor execution registered successfully. " +
                        "serviceOrderId={}, laborId={}, durationInDays={}",
                serviceOrderId,
                laborId,
                durationInDays
        );
    }

    /**
     * Atualiza o monitoramento incrementalmente usando
     * uma nova execução.
     */
    private void updateMonitoring(
            String laborId,
            double durationInDays) {

        Optional<LaborMonitoring> existing =
                repository.findByLaborId(laborId);

        if (existing.isPresent()) {

            LaborMonitoring entity = existing.get();

            double newAverage = calculateNewAverage(
                    entity.getAverageExecutionTimeInDays(),
                    entity.getTotalExecutions(),
                    durationInDays
            );

            entity.setAverageExecutionTimeInDays(newAverage);
            entity.setTotalExecutions(
                    entity.getTotalExecutions() + 1
            );

            repository.save(entity);

            log.info(
                    "Labor execution monitoring updated. " +
                            "laborId={}, durationInDays={}, newAverage={}, totalExecutions={}",
                    laborId,
                    durationInDays,
                    newAverage,
                    entity.getTotalExecutions()
            );

            return;
        }

        Optional<Labor> laborOpt =
                laborRepository.findById(laborId);

        if (laborOpt.isEmpty()) {

            log.warn(
                    "Labor not found for monitoring update. laborId={}",
                    laborId
            );

            return;
        }

        Labor labor = laborOpt.get();

        LaborMonitoring newEntity = LaborMonitoring.builder()
                .laborId(laborId)
                .laborName(labor.getName())
                .laborDescription(labor.getDescription())
                .averageExecutionTimeInDays(durationInDays)
                .totalExecutions(1)
                .build();

        repository.save(newEntity);

        log.info(
                "Labor execution monitoring initialized. " +
                        "laborId={}, durationInDays={}",
                laborId,
                durationInDays
        );
    }

    public void initializeFromEstimate(
            String laborId,
            String laborName,
            String laborDescription,
            Integer estimatedDays) {

        log.info(
                "Initializing labor monitoring from estimate. " +
                        "laborId={}, estimatedDays={}",
                laborId,
                estimatedDays
        );

        if (estimatedDays == null) {

            log.debug(
                    "Skipping labor monitoring initialization because estimatedDays is null. " +
                            "laborId={}",
                    laborId
            );

            return;
        }

        repository.findByLaborId(laborId).ifPresentOrElse(
                entity -> {

                    entity.setLaborName(laborName);
                    entity.setLaborDescription(laborDescription);

                    repository.save(entity);

                    log.info(
                            "Labor monitoring information updated from estimate. " +
                                    "laborId={}",
                            laborId
                    );
                },
                () -> {

                    LaborMonitoring entity = LaborMonitoring.builder()
                            .laborId(laborId)
                            .laborName(laborName)
                            .laborDescription(laborDescription)
                            .averageExecutionTimeInDays(
                                    (double) estimatedDays
                            )
                            .totalExecutions(0)
                            .build();

                    repository.save(entity);

                    log.info(
                            "Labor monitoring created from estimate. " +
                                    "laborId={}, estimatedDays={}",
                            laborId,
                            estimatedDays
                    );
                }
        );
    }

    /**
     * Recalcula todos os monitoramentos usando o histórico
     * de LaborExecution.
     *
     * Não depende mais de ServiceOrderRepository.
     */
    public int forceRecalc() {

        log.info("Starting labor monitoring force recalculation");

        List<Labor> labors =
                laborRepository.findByActiveTrue();

        log.info(
                "Active labors found for force recalculation: {}",
                labors.size()
        );

        /*
         * Busca todas as execuções uma única vez.
         */
        List<LaborExecution> executions =
                laborExecutionRepository.findAll();

        log.info(
                "Labor executions found for force recalculation: {}",
                executions.size()
        );

        /*
         * Agrupa as execuções pelo laborId.
         */
        Map<String, List<LaborExecution>> executionsByLabor =
                executions.stream()
                        .filter(execution ->
                                execution.getLaborId() != null
                                        && execution.getStartDate() != null
                                        && execution.getEndDate() != null
                        )
                        .collect(Collectors.groupingBy(
                                LaborExecution::getLaborId
                        ));

        int processed = 0;

        for (Labor labor : labors) {

            log.debug(
                    "Calculating execution time for labor. " +
                            "laborId={}, laborName={}",
                    labor.getId(),
                    labor.getName()
            );

            List<LaborExecution> laborExecutions =
                    executionsByLabor.getOrDefault(
                            labor.getId(),
                            List.of()
                    );

            if (laborExecutions.isEmpty()) {

                log.debug(
                        "No completed executions found for labor. " +
                                "laborId={}",
                        labor.getId()
                );

                continue;
            }

            List<Double> durations =
                    laborExecutions.stream()
                            .map(execution ->
                                    calculateDurationInDays(
                                            execution.getStartDate(),
                                            execution.getEndDate()
                                    )
                            )
                            .filter(duration -> duration >= 0)
                            .toList();

            if (durations.isEmpty()) {

                log.debug(
                        "No valid execution durations found for labor. " +
                                "laborId={}",
                        labor.getId()
                );

                continue;
            }

            LaborMonitoring entity =
                    repository.findByLaborId(labor.getId())
                            .orElseGet(() ->
                                    LaborMonitoring.builder()
                                            .laborId(labor.getId())
                                            .build()
                            );

            entity.setLaborName(labor.getName());
            entity.setLaborDescription(labor.getDescription());

            double average =
                    durations.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0);

            entity.setAverageExecutionTimeInDays(average);
            entity.setTotalExecutions(durations.size());

            repository.save(entity);

            processed++;

            log.debug(
                    "Labor monitoring recalculated. " +
                            "laborId={}, averageExecutionTimeInDays={}, totalExecutions={}",
                    labor.getId(),
                    average,
                    durations.size()
            );
        }

        log.info(
                "Labor monitoring force recalculation completed. " +
                        "Records processed: {}",
                processed
        );

        return processed;
    }

    private double calculateDurationInDays(
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return ChronoUnit.SECONDS.between(
                startDate.atOffset(ZoneOffset.UTC),
                endDate.atOffset(ZoneOffset.UTC)
        ) / WORK_DAY_SECONDS_DOUBLE;
    }

    private double calculateNewAverage(
            double currentAverage,
            int totalExecutions,
            double newDuration) {

        return (
                currentAverage * totalExecutions
                        + newDuration
        ) / (totalExecutions + 1);
    }
}
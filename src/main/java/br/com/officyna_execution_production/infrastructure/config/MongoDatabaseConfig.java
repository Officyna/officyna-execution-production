package br.com.officyna_execution_production.infrastructure.config;

import br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository.LaborMongoRepository;
import br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository.LaborMonitoringMongoRepository;
import br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository.SupplyMongoRepository;
import br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository.StockMovementMongoRepository;
import br.com.officyna_execution_production.infrastructure.converter.LocalDateTimeConverters;
import br.com.officyna_execution_production.monitoring.infrastructure.persistence.LaborExecutionMongoRepository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import java.util.List;

@Configuration
@EnableMongoAuditing
public class MongoDatabaseConfig {
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                LocalDateTimeConverters.LocalDateTimeToDateConverter.INSTANCE,
                LocalDateTimeConverters.DateToLocalDateTimeConverter.INSTANCE
        ));
    }

    @Bean
    MongoClient mongoClient(@Value("${spring.mongodb.uri}") String uri) {
        return MongoClients.create(uri);
    }

    @Bean
    @Primary
    MongoDatabaseFactory mongoDatabaseFactory(MongoClient c, @Value("${database.snapshot-name}") String name) {
        return new SimpleMongoClientDatabaseFactory(c, name);
    }

    @Bean
    @Primary
    MongoTemplate mongoTemplate(MongoDatabaseFactory f, MongoConverter converter) {
        return new MongoTemplate(f, converter);
    }

    @Bean
    MongoTemplate laborMongoTemplate(MongoDatabaseFactory f, @Value("${database.labor-name}") String name, MongoConverter converter) {
        return new NamedMongoTemplate(f, name, converter);
    }

    @Bean
    MongoTemplate supplyMongoTemplate(MongoDatabaseFactory f, @Value("${database.supply-name}") String name, MongoConverter converter) {
        return new NamedMongoTemplate(f, name, converter);
    }

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory f) {
        return new MongoTransactionManager(f);
    }

    @Configuration
    @EnableMongoRepositories(basePackages = "br.com.officyna_execution_production.serviceorder.infrastructure.persistence.mongodb.repository", mongoTemplateRef = "mongoTemplate")
    static class SnapshotRepositories {
    }

    @Configuration
    @EnableMongoRepositories(
            basePackages = {"br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository", "br.com.officyna_execution_production.monitoring.infrastructure.persistence"},
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {LaborMongoRepository.class, LaborMonitoringMongoRepository.class, LaborExecutionMongoRepository.class}),
            mongoTemplateRef = "laborMongoTemplate")
    static class LaborRepositories {
    }

    @Configuration
    @EnableMongoRepositories(
            basePackages = "br.com.officyna_execution_production.infrastructure.persistence.mongodb.repository",
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {SupplyMongoRepository.class, StockMovementMongoRepository.class}),
            mongoTemplateRef = "supplyMongoTemplate")
    static class SupplyRepositories {
    }
}

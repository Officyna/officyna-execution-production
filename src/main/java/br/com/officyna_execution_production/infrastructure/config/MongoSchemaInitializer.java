package br.com.officyna_execution_production.infrastructure.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;

@Configuration
public class MongoSchemaInitializer {
    @Bean
    ApplicationRunner schema(@Qualifier("mongoTemplate") MongoTemplate snapshots, @Qualifier("laborMongoTemplate") MongoTemplate labor, @Qualifier("supplyMongoTemplate") MongoTemplate supply) {
        return args -> {
            for (String name : new String[]{"execution_work"})
                if (!snapshots.collectionExists(name)) snapshots.createCollection(name);
            for (String name : new String[]{"labors", "labor_monitoring", "labor_executions"})
                if (!labor.collectionExists(name)) labor.createCollection(name);
            for (String name : new String[]{"supplies", "stock_movements"})
                if (!supply.collectionExists(name)) supply.createCollection(name);
            snapshots.indexOps("execution_work").ensureIndex(new Index().on("serviceOrderId", Sort.Direction.ASC).unique());
            labor.indexOps("labor_monitoring").ensureIndex(new Index().on("laborId", Sort.Direction.ASC).unique());
        };
    }
}

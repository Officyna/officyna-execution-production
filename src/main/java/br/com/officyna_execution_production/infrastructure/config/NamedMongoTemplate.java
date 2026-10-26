package br.com.officyna_execution_production.infrastructure.config;

import com.mongodb.client.MongoDatabase;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoDatabaseUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

/**
 * Uses the same session-bound factory for transactions across logical databases.
 */
public class NamedMongoTemplate extends MongoTemplate {
    private final MongoDatabaseFactory factory;
    private final String database;

    public NamedMongoTemplate(MongoDatabaseFactory factory, String database, MongoConverter converter) {
        super(factory, converter);
        this.factory = factory;
        this.database = database;
    }

    @Override
    protected MongoDatabase doGetDatabase() {
        return MongoDatabaseUtils.getDatabase(database, factory);
    }
}

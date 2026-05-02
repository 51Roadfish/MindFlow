package com.mindflow.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PgVectorConfig {

    @Value("${spring.ai.vectorstore.pgvector.host}")
    private String host;

    @Value("${spring.ai.vectorstore.pgvector.port}")
    private int port;

    @Value("${spring.ai.vectorstore.pgvector.database}")
    private String database;

    @Value("${spring.ai.vectorstore.pgvector.username}")
    private String username;

    @Value("${spring.ai.vectorstore.pgvector.password}")
    private String password;

    @Value("${spring.ai.vectorstore.pgvector.initialize-schema}")
    private boolean initializeSchema;

    private final EmbeddingModel embeddingModel;

    public PgVectorConfig(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    // 用于直接操作 PostgreSQL 的 JdbcTemplate（供 VectorStoreService 使用）
    @Bean
    public JdbcTemplate pgJdbcTemplate() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        return new JdbcTemplate(dataSource);
    }

    // 创建 VectorStore Bean（注入 EmbeddingModel）
    @Bean
    public VectorStore vectorStore() {
        // 复用 pgJdbcTemplate 所建的数据源，也可直接使用 pgJdbcTemplate
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");

        PgVectorStore store = new PgVectorStore(new JdbcTemplate(dataSource), embeddingModel);
        if (initializeSchema) {
            store.afterPropertiesSet();
        }
        return store;
    }
}
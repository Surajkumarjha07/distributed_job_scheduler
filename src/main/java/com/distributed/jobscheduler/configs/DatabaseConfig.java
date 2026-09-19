package com.distributed.jobscheduler.configs;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@SuppressWarnings("FieldCanBeLocal")
@Configuration
public class DatabaseConfig {
    @Value("")
    private String url;

    @Value("")
    private String username;

    @Value("")
    private String password;

    @Value("")
    private String driverClassName;

    private final Integer DB_TIMEOUT_MS = 30000;
    private final Integer MAX_POOL_SIZE = 10;
    private final Integer MIN_IDLE_SIZE = 2;
    private final Integer CONNECTION_TIMEOUT_MS = 20000;

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        dataSource.setMaximumPoolSize(MAX_POOL_SIZE);
        dataSource.setMinimumIdle(MIN_IDLE_SIZE);
        dataSource.setIdleTimeout(DB_TIMEOUT_MS);
        dataSource.setConnectionTimeout(CONNECTION_TIMEOUT_MS);

        return dataSource;
    }
}

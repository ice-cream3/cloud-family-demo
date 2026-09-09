package com.kfpd.cloud.common.datasource;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@ConditionalOnClass({DataSource.class, JdbcTemplate.class, HikariDataSource.class})
@EnableConfigurationProperties({MultiDataSourceProperties.class, DataSourceCryptoProperties.class})
public class MultiDataSourceAutoConfiguration {

    @Bean(MultiDataSourceNames.FA_CLOUD)
    @Primary
    @ConditionalOnMissingBean(name = MultiDataSourceNames.FA_CLOUD)
    DataSource faCloudDataSource(MultiDataSourceProperties properties, DataSourceCryptoProperties cryptoProperties) {
        return createDataSource(properties.getCloud(), cryptoProperties, "fa-cloud");
    }

    @Bean(MultiDataSourceNames.FA_MODEL)
    @ConditionalOnMissingBean(name = MultiDataSourceNames.FA_MODEL)
    DataSource faModelDataSource(MultiDataSourceProperties properties, DataSourceCryptoProperties cryptoProperties) {
        return createDataSource(properties.getModel(), cryptoProperties, "fa-model");
    }

    @Bean(MultiDataSourceNames.FA_CLOUD_JDBC_TEMPLATE)
    @Primary
    @ConditionalOnMissingBean(name = MultiDataSourceNames.FA_CLOUD_JDBC_TEMPLATE)
    JdbcTemplate faCloudJdbcTemplate(@Qualifier(MultiDataSourceNames.FA_CLOUD) DataSource faCloudDataSource) {
        return new JdbcTemplate(faCloudDataSource);
    }

    @Bean(MultiDataSourceNames.FA_MODEL_JDBC_TEMPLATE)
    @ConditionalOnMissingBean(name = MultiDataSourceNames.FA_MODEL_JDBC_TEMPLATE)
    JdbcTemplate faModelJdbcTemplate(@Qualifier(MultiDataSourceNames.FA_MODEL) DataSource faModelDataSource) {
        return new JdbcTemplate(faModelDataSource);
    }

    @Bean(MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    @Primary
    @ConditionalOnMissingBean(name = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    PlatformTransactionManager faCloudTransactionManager(@Qualifier(MultiDataSourceNames.FA_CLOUD) DataSource faCloudDataSource) {
        return new JdbcTransactionManager(faCloudDataSource);
    }

    @Bean(MultiDataSourceNames.FA_MODEL_TRANSACTION_MANAGER)
    @ConditionalOnMissingBean(name = MultiDataSourceNames.FA_MODEL_TRANSACTION_MANAGER)
    PlatformTransactionManager faModelTransactionManager(@Qualifier(MultiDataSourceNames.FA_MODEL) DataSource faModelDataSource) {
        return new JdbcTransactionManager(faModelDataSource);
    }

    private DataSource createDataSource(MultiDataSourceProperties.DataSourcePool properties,
                                        DataSourceCryptoProperties cryptoProperties,
                                        String defaultPoolName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(EncryptedDataSourceValues.decryptIfNecessary(properties.getUsername(), cryptoProperties.getSecret()));
        dataSource.setPassword(EncryptedDataSourceValues.decryptIfNecessary(properties.getPassword(), cryptoProperties.getSecret()));
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        dataSource.setMinimumIdle(properties.getMinimumIdle());
        dataSource.setPoolName(StringUtils.hasText(properties.getPoolName()) ? properties.getPoolName() : defaultPoolName);
        return dataSource;
    }
}

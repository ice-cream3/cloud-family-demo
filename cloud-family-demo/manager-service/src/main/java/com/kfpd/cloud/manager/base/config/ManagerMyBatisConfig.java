package com.kfpd.cloud.manager.base.config;

import javax.sql.DataSource;

import com.kfpd.cloud.common.datasource.MultiDataSourceNames;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManagerMyBatisConfig {

    @Bean
    SqlSessionFactory managerSqlSessionFactory(
            @Qualifier(MultiDataSourceNames.FA_CLOUD) DataSource dataSource,
            ApplicationContext applicationContext
    ) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(applicationContext.getResources("classpath*:mapper/**/*.xml"));
        return factoryBean.getObject();
    }

    @Bean
    static MapperScannerConfigurer managerMapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.kfpd.cloud.manager.dao");
        configurer.setSqlSessionFactoryBeanName("managerSqlSessionFactory");
        return configurer;
    }
}

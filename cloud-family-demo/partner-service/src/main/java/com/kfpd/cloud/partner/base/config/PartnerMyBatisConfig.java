package com.kfpd.cloud.partner.base.config;

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
public class PartnerMyBatisConfig {

    @Bean
    SqlSessionFactory partnerSqlSessionFactory(
            @Qualifier(MultiDataSourceNames.FA_CLOUD) DataSource dataSource,
            ApplicationContext applicationContext
    ) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(applicationContext.getResources("classpath*:mapper/**/*.xml"));
        return factoryBean.getObject();
    }

    @Bean
    static MapperScannerConfigurer partnerMapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.kfpd.cloud.partner.dao");
        configurer.setSqlSessionFactoryBeanName("partnerSqlSessionFactory");
        return configurer;
    }
}

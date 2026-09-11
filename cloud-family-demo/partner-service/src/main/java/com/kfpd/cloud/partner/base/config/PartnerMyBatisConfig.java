package com.kfpd.cloud.partner.base.config;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.kfpd.cloud.common.datasource.MultiDataSourceNames;

import org.apache.ibatis.session.SqlSessionFactory;
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
            ApplicationContext applicationContext,
            MybatisPlusInterceptor partnerMybatisPlusInterceptor
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(applicationContext.getResources("classpath*:mapper/**/*.xml"));
        factoryBean.setPlugins(partnerMybatisPlusInterceptor);
        return factoryBean.getObject();
    }

    @Bean
    MybatisPlusInterceptor partnerMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    static MapperScannerConfigurer partnerMapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.kfpd.cloud.partner.dao");
        configurer.setSqlSessionFactoryBeanName("partnerSqlSessionFactory");
        return configurer;
    }
}

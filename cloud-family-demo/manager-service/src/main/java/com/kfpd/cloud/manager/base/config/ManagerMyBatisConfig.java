package com.kfpd.cloud.manager.base.config;

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
public class ManagerMyBatisConfig {

    @Bean
    SqlSessionFactory managerSqlSessionFactory(
            @Qualifier(MultiDataSourceNames.FA_CLOUD) DataSource dataSource,
            ApplicationContext applicationContext,
            MybatisPlusInterceptor managerMybatisPlusInterceptor
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(applicationContext.getResources("classpath*:mapper/**/*.xml"));
        factoryBean.setPlugins(managerMybatisPlusInterceptor);
        return factoryBean.getObject();
    }

    @Bean
    MybatisPlusInterceptor managerMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    static MapperScannerConfigurer managerMapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.kfpd.cloud.manager.dao");
        configurer.setSqlSessionFactoryBeanName("managerSqlSessionFactory");
        return configurer;
    }
}

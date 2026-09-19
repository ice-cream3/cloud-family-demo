package com.kfpd.cloud.manager.base.config;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManagerModelMyBatisConfig {

    @Bean
    SqlSessionFactory managerModelSqlSessionFactory(
            @Qualifier(MultiDataSourceNames.FA_MODEL) DataSource dataSource,
            ApplicationContext applicationContext,
            MybatisPlusInterceptor managerModelMybatisPlusInterceptor
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(applicationContext.getResources("classpath*:mapper/model/**/*.xml"));
        factoryBean.setPlugins(managerModelMybatisPlusInterceptor);
        return factoryBean.getObject();
    }

    @Bean
    MybatisPlusInterceptor managerModelMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    static MapperScannerConfigurer managerModelMapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.kfpd.cloud.manager.dao.model");
        configurer.setSqlSessionFactoryBeanName("managerModelSqlSessionFactory");
        return configurer;
    }
}

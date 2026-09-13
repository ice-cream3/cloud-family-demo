package com.kfpd.cloud.job.base.config.mybatis;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor;
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
public class JobCloudMyBatisConfig {

    @Bean
    SqlSessionFactory jobCloudSqlSessionFactory(
            @Qualifier(MultiDataSourceNames.FA_CLOUD) DataSource dataSource,
            ApplicationContext applicationContext,
            MybatisPlusInterceptor jobCloudMybatisPlusInterceptor
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(applicationContext.getResources("classpath*:mapper/cloud/**/*.xml"));
        factoryBean.setPlugins(jobCloudMybatisPlusInterceptor);
        return factoryBean.getObject();
    }

    @Bean
    MybatisPlusInterceptor jobCloudMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new IllegalSQLInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    static MapperScannerConfigurer jobCloudMapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.kfpd.cloud.job.dao.cloud");
        configurer.setSqlSessionFactoryBeanName("jobCloudSqlSessionFactory");
        return configurer;
    }
}

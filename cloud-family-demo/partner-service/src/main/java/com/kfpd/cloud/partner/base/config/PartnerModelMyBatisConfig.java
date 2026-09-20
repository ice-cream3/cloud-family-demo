package com.kfpd.cloud.partner.base.config;

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
public class PartnerModelMyBatisConfig {

    @Bean
    SqlSessionFactory partnerModelSqlSessionFactory(
            @Qualifier(MultiDataSourceNames.FA_MODEL) DataSource dataSource,
            ApplicationContext applicationContext,
            MybatisPlusInterceptor partnerModelMybatisPlusInterceptor
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(applicationContext.getResources("classpath*:mapper/model/**/*.xml"));
        factoryBean.setPlugins(partnerModelMybatisPlusInterceptor);
        return factoryBean.getObject();
    }

    @Bean
    MybatisPlusInterceptor partnerModelMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    static MapperScannerConfigurer partnerModelMapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.kfpd.cloud.partner.dao.model");
        configurer.setSqlSessionFactoryBeanName("partnerModelSqlSessionFactory");
        return configurer;
    }
}

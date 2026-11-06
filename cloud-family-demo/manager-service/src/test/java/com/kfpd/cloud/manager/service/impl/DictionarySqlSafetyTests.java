package com.kfpd.cloud.manager.service.impl;

import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DictionarySqlSafetyTests {
    @Test void dictionarySyncUsesInsertCommandAndProductRefreshPassesSqlProtection() throws Exception {
        Configuration configuration=new Configuration();
        String resource="mapper/cloud/DictionaryMapper.xml";
        try(var input=getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            new XMLMapperBuilder(input,configuration,resource,configuration.getSqlFragments()).parse();
        }
        String namespace="com.kfpd.cloud.manager.dao.cloud.DictionaryDao.";
        assertThat(configuration.getMappedStatement(namespace+"syncMerchantTypes").getSqlCommandType()).isEqualTo(SqlCommandType.INSERT);
        String update=configuration.getMappedStatement(namespace+"refreshMerchantProducts").getBoundSql(null).getSql();
        assertThatCode(()->new BlockAttackInnerInterceptor().parserMulti(update,null)).doesNotThrowAnyException();
        for(String kind:java.util.List.of("types","items")) {
            String sql=configuration.getMappedStatement(namespace+"updateStatus").getBoundSql(java.util.Map.of("kind",kind,"id",1L,"status","DISABLED")).getSql();
            assertThatCode(()->new BlockAttackInnerInterceptor().parserMulti(sql,null)).doesNotThrowAnyException();
        }
    }
}

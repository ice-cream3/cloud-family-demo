package com.kfpd.cloud.partner.dao.cloud;
import java.util.*;
import org.apache.ibatis.annotations.Param;
public interface DictionaryDao {
    List<Map<String,Object>> options(@Param("typeCode") String typeCode);
}

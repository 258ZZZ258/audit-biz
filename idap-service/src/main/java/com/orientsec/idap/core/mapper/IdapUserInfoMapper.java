package com.orientsec.idap.core.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orientsec.idap.core.model.IdapUserInfo;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** User information mapper from the intranet IDAP base. */
public interface IdapUserInfoMapper extends BaseMapper<IdapUserInfo> {

    @Select("select * from idap_user_info ${ew.customSqlSegment}")
    List<Map<String, Object>> selectListToMap(@Param("ew") Wrapper<IdapUserInfo> wrapper);
}

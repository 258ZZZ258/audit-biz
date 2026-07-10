package com.orientsec.idap.core.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** audit-biz 自有操作日志写入。 */
@Mapper
public interface OperationLogMapper {

    @Insert(
            "INSERT INTO operation_logs "
                    + "(trace_id, actor, action, action_point, status, detail_json, created_at) "
                    + "VALUES "
                    + "(#{traceId}, #{actor}, #{action}, #{actionPoint}, #{status}, #{detailJson}, CURRENT_TIMESTAMP)")
    void insert(
            @Param("traceId") String traceId,
            @Param("actor") String actor,
            @Param("action") String action,
            @Param("actionPoint") String actionPoint,
            @Param("status") String status,
            @Param("detailJson") String detailJson);
}

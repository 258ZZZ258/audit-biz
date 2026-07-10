package com.orientsec.idap.core.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.common.model.ResultGenerator;
import com.orientsec.idap.common.utils.LogHelper;
import com.orientsec.idap.core.model.IdapUserInfo;
import com.orientsec.idap.core.service.IdapUserInfoService;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** User management controller migrated from the intranet IDAP base. */
@RestController
@RequestMapping("/idap/v1/idapUserInfo")
public class IdapUserInfoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdapUserInfoController.class);

    private final IdapUserInfoService idapUserInfoService;

    public IdapUserInfoController(IdapUserInfoService idapUserInfoService) {
        this.idapUserInfoService = idapUserInfoService;
    }

    /** Query all matching users; pagination remains a frontend concern as in the intranet code. */
    @GetMapping("/list")
    public Result<List<IdapUserInfo>> list(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer sex,
            @RequestParam(required = false) Integer status) {
        LogHelper.log(LOGGER, "进入用户列表查询, 参数:", userName, mobile, email, sex, status);
        try {
            LambdaQueryWrapper<IdapUserInfo> param = new LambdaQueryWrapper<>();
            param.eq(IdapUserInfo::getDeleted, (byte) 0)
                    .like(userName != null, IdapUserInfo::getUserName, userName)
                    .like(mobile != null, IdapUserInfo::getMobile, mobile)
                    .like(email != null, IdapUserInfo::getEmail, email)
                    .eq(sex != null, IdapUserInfo::getSex, sex)
                    .eq(status != null, IdapUserInfo::getStatus, status)
                    .orderByDesc(IdapUserInfo::getCreateTime);

            List<IdapUserInfo> userList = idapUserInfoService.list(param);
            LOGGER.info("用户列表查询成功, 数量: {}", userList.size());
            return ResultGenerator.genSuccessResult(userList);
        } catch (Exception e) {
            LOGGER.error("用户列表查询失败", e);
            return ResultGenerator.genFailResult(e);
        }
    }

    @GetMapping("/detail")
    public Result<IdapUserInfo> detail(@RequestParam String userId) {
        LOGGER.info("进入用户详情查询, userId={}", userId);
        try {
            IdapUserInfo userInfo = idapUserInfoService.getById(userId);
            requireActive(userInfo);
            LOGGER.info("用户详情查询成功");
            return ResultGenerator.genSuccessResult(userInfo);
        } catch (Exception e) {
            LOGGER.error("用户详情查询失败", e);
            return ResultGenerator.genFailResult(e);
        }
    }

    @PostMapping("/create")
    public Result<Void> create(@RequestBody Map<String, Object> data) {
        LOGGER.info("进入用户新增");
        try {
            IdapUserInfo userInfo = new IdapUserInfo();
            userInfo.setUserId(IdUtil.fastSimpleUUID());
            applyEditableFields(userInfo, data, true);
            userInfo.setCreateTime(new Date());
            userInfo.setCreateUser("admin");
            userInfo.setUpdateTime(new Date());
            userInfo.setUpdateUser("admin");
            userInfo.setDeleted((byte) 0);

            idapUserInfoService.save(userInfo);
            LOGGER.info("用户新增成功, userId={}", userInfo.getUserId());
            return success();
        } catch (Exception e) {
            LOGGER.error("用户新增失败", e);
            return failure(e);
        }
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody Map<String, Object> data) {
        LOGGER.info("进入用户更新");
        try {
            String userId = stringValue(data.get("userId"));
            IdapUserInfo userInfo = idapUserInfoService.getById(userId);
            requireActive(userInfo);

            applyEditableFields(userInfo, data, false);
            userInfo.setUpdateTime(new Date());
            userInfo.setUpdateUser("admin");
            idapUserInfoService.updateById(userInfo);
            LOGGER.info("用户更新成功, userId={}", userId);
            return success();
        } catch (Exception e) {
            LOGGER.error("用户更新失败", e);
            return failure(e);
        }
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Map<String, Object> data) {
        Object idsObj = data.get("ids");
        LOGGER.info("进入用户删除");
        try {
            List<String> ids = stringList(idsObj);
            for (String userId : ids) {
                IdapUserInfo userInfo = idapUserInfoService.getById(userId);
                if (userInfo != null && !Byte.valueOf((byte) 1).equals(userInfo.getDeleted())) {
                    userInfo.setDeleted((byte) 1);
                    userInfo.setUpdateTime(new Date());
                    userInfo.setUpdateUser("admin");
                    idapUserInfoService.updateById(userInfo);
                }
            }
            LOGGER.info("用户删除成功, 数量={}", ids.size());
            return success();
        } catch (Exception e) {
            LOGGER.error("用户删除失败", e);
            return failure(e);
        }
    }

    @PostMapping("/changeStatus")
    public Result<Void> changeStatus(@RequestBody Map<String, Object> data) {
        String userId = stringValue(data.get("userId"));
        Number status = numberValue(data.get("status"), "status");
        LOGGER.info("进入用户状态修改, userId={}, status={}", userId, status);
        try {
            IdapUserInfo userInfo = idapUserInfoService.getById(userId);
            requireActive(userInfo);
            userInfo.setStatus(status.byteValue());
            userInfo.setUpdateTime(new Date());
            userInfo.setUpdateUser("admin");
            idapUserInfoService.updateById(userInfo);
            LOGGER.info("用户状态修改成功, userId={}, status={}", userId, status);
            return success();
        } catch (Exception e) {
            LOGGER.error("用户状态修改失败", e);
            return failure(e);
        }
    }

    @PostMapping("/resetPassword")
    public Result<Void> resetPassword(@RequestBody Map<String, Object> data) {
        String userId = stringValue(data.get("userId"));
        LOGGER.info("进入用户密码重置, userId={}", userId);
        try {
            // Password storage belongs to the intranet authentication service; this endpoint
            // deliberately preserves the base project's successful placeholder behavior.
            LOGGER.info("用户密码重置成功, userId={}", userId);
            return success();
        } catch (Exception e) {
            LOGGER.error("用户密码重置失败", e);
            return failure(e);
        }
    }

    private static void applyEditableFields(
            IdapUserInfo userInfo, Map<String, Object> data, boolean applyDefaults) {
        userInfo.setUserName(stringValue(data.get("userName")));
        userInfo.setEmail(stringValue(data.get("email")));
        userInfo.setMobile(stringValue(data.get("mobile")));
        Number sex = optionalNumber(data.get("sex"), "sex");
        if (sex != null) {
            userInfo.setSex(sex.byteValue());
        } else if (applyDefaults) {
            userInfo.setSex((byte) 0);
        }
        userInfo.setNote(stringValue(data.get("note")));
        userInfo.setDepartmentId(stringValue(data.get("departmentId")));
        Number status = optionalNumber(data.get("status"), "status");
        if (status != null) {
            userInfo.setStatus(status.byteValue());
        } else if (applyDefaults) {
            userInfo.setStatus((byte) 1);
        }
        userInfo.setAvatar(stringValue(data.get("avatar")));
    }

    private static void requireActive(IdapUserInfo userInfo) {
        if (userInfo == null || Byte.valueOf((byte) 1).equals(userInfo.getDeleted())) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) value;
            java.util.ArrayList<String> ids = new java.util.ArrayList<>();
            for (Object item : values) {
                ids.add(stringValue(item));
            }
            return ids;
        }
        if (value instanceof String[]) {
            return Arrays.asList((String[]) value);
        }
        return Arrays.asList(stringValue(value));
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Number optionalNumber(Object value, String field) {
        if (value == null) {
            return null;
        }
        return numberValue(value, field);
    }

    private static Number numberValue(Object value, String field) {
        if (value instanceof Number) {
            return (Number) value;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(field + "必须是数字", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Result<T> success() {
        return ResultGenerator.genSuccessResult();
    }

    @SuppressWarnings("unchecked")
    private static <T> Result<T> failure(Exception e) {
        return ResultGenerator.genFailResult(e);
    }
}

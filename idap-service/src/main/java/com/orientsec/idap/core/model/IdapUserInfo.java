package com.orientsec.idap.core.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

/** IDAP user entity retained for compatibility with the intranet management endpoints. */
@TableName("idap_user_info")
public class IdapUserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("user_id")
    private String userId;

    @TableField("user_name")
    private String userName;

    @TableField("email")
    private String email;

    @TableField("mobile")
    private String mobile;

    @TableField("sex")
    private Byte sex;

    @TableField("note")
    private String note;

    @TableField("department_id")
    private String departmentId;

    @TableField("role_id")
    private String roleId;

    @TableField("status")
    private Byte status;

    @TableField("avatar")
    private String avatar;

    @TableField("create_time")
    private Date createTime;

    @TableField("create_user")
    private String createUser;

    @TableField("update_time")
    private Date updateTime;

    @TableField("update_user")
    private String updateUser;

    @TableField("deleted")
    private Byte deleted;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Byte getSex() {
        return sex;
    }

    public void setSex(Byte sex) {
        this.sex = sex;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public Byte getDeleted() {
        return deleted;
    }

    public void setDeleted(Byte deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "IdapUserInfo{"
                + "userId = "
                + userId
                + ", userName = "
                + userName
                + ", email = "
                + email
                + ", mobile = "
                + mobile
                + ", departmentId = "
                + departmentId
                + ", roleId = "
                + roleId
                + ", sex = "
                + sex
                + ", note = "
                + note
                + ", avatar = "
                + avatar
                + ", status = "
                + status
                + ", createTime = "
                + createTime
                + ", createUser = "
                + createUser
                + ", updateTime = "
                + updateTime
                + ", updateUser = "
                + updateUser
                + ", deleted = "
                + deleted
                + "}";
    }
}

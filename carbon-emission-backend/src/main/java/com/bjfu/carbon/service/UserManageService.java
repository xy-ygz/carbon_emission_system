package com.bjfu.carbon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bjfu.carbon.domain.User;
import com.bjfu.carbon.vo.UserVo;

import java.util.List;

/**
 * 用户管理服务接口
 * 接口隔离：只提供用户管理相关方法
 *
 * @author xgy
 */
public interface UserManageService {

    /**
     * 分页查询用户列表
     *
     * @param page     分页参数
     * @param name     姓名（可选）
     * @param username 用户名（可选）
     * @param department 部门（可选）
     * @return 用户分页列表
     */
    IPage<UserVo> getUserList(Page<UserVo> page, String name, String username, String department);

    /**
     * 获取用户详情（包含角色）
     *
     * @param userId 用户ID
     * @return 用户视图对象
     */
    UserVo getUserWithRoles(Long userId);

    /**
     * 新增用户
     *
     * @param user    用户对象
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean addUser(User user, List<Long> roleIds);

    /**
     * 更新用户
     *
     * @param user    用户对象
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean updateUser(User user, List<Long> roleIds);

    /**
     * 重置用户密码
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean resetPassword(Long userId);

    /**
     * 分配角色给用户
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean assignRoles(Long userId, List<Long> roleIds);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Long userId);
}


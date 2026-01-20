package com.bjfu.carbon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjfu.carbon.domain.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联Mapper接口
 *
 * @author xgy
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 获取用户的角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> getRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 批量插入用户角色关联
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     * @return 插入数量
     */
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 删除用户的所有角色关联
     *
     * @param userId 用户ID
     * @return 删除数量
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除角色的所有用户关联
     *
     * @param roleId 角色ID
     * @return 删除数量
     */
    int deleteByRoleId(@Param("roleId") Long roleId);
}


package com.bjfu.carbon.service;

import com.bjfu.carbon.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务类
 *
 * @author xgy
 * @since 2023-02-13
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户对象
     */
    User getUserByUsername(String username);
}

package com.bjfu.carbon.service;

import com.bjfu.carbon.domain.School;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 学校服务类
 *
 * @author xgy
 * @since 2023-02-13
 */
public interface SchoolService extends IService<School> {
    Long selectTotalPeople(String schoolName);
}

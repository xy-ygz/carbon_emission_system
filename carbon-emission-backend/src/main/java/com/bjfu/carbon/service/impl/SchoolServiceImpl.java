package com.bjfu.carbon.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjfu.carbon.domain.School;
import com.bjfu.carbon.service.SchoolService;
import com.bjfu.carbon.mapper.SchoolMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 学校服务类实现类
 *
 * @author xgy
 * @since 2023-02-13
 */
@Service
public class SchoolServiceImpl extends ServiceImpl<SchoolMapper, School> implements SchoolService {
    @Autowired
    private SchoolMapper schoolMapper;

    @Override
    public Long selectTotalPeople(String schoolName) {
        return schoolMapper.selectTotalPeople(schoolName);
    }
}





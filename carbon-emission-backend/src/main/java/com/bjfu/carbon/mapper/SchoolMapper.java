package com.bjfu.carbon.mapper;

import com.bjfu.carbon.domain.School;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Entity com.bjfu.carbon.domain.School
 */
public interface SchoolMapper extends BaseMapper<School> {

    Long selectTotalPeople(@Param("schoolName") String schoolName);
}





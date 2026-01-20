package com.bjfu.carbon.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjfu.carbon.annotation.RedisCache;
import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.School;
import com.bjfu.carbon.service.CarbonEmissionService;
import com.bjfu.carbon.service.SchoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * 学校 控制层
 *
 * @author xgy
 * @since 2023-02-16
 */
@Slf4j
@RestController
@RequestMapping("/school")
public class SchoolController {

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private CarbonEmissionService carbonEmissionService;

    /**
     * 校园概括
     * 显示校园名称[北京林业大学]，校园总人数，在校老师人数，在校学生人数，校园总占地面积，校园总建筑面积，校园总绿地面积
     * 学校碳排放总量，人均碳排放量，地均碳排放量
     * 学校电耗总量，学校热耗总量，学校水耗总量，学校能耗总量 todo
     * @return
     */
    @RedisCache(prefix = "school", key = "'info'", expire = 1800)
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/info")
    public Object schoolInfo(){
        Map<String,Object> map = new HashMap<>();
        Calendar now = Calendar.getInstance();
        int requestYear = now.get(Calendar.YEAR);//首页为当前年份数据
        Map<String,Double> emissionMap = carbonEmissionService.collegeCarbonEmission(requestYear,null);
        School school = schoolService.getOne(new LambdaQueryWrapper<School>().eq(School::getSchoolName, "北京林业大学"));
        
        // 检查学校信息是否存在
        if (school == null) {
            return ResultUtils.error(com.bjfu.carbon.common.ErrorCode.NULL_ERROR, "学校信息不存在，请先添加学校信息");
        }
        
        map.put("totalNumber",Double.valueOf(school.getTotalNumber()));
        map.put("totalArea",Double.valueOf(school.getTotalArea()));
        map.put("buildingArea",Double.valueOf(school.getBuildingArea()));
        map.put("greenArea",Double.valueOf(school.getGreenArea()));
        map.put("teacherNumber", (double) (school.getTeacherNumber()));
        map.put("studentNumber", (double) (school.getStudentNumber()));
        map.put("totalEmission",emissionMap.get("totalEmission"));
        map.put("personAverageEmission",emissionMap.get("personAverageEmission"));
        map.put("groundAverageEmission",emissionMap.get("groundAverageEmission"));
        // 返回学校图片地址
        map.put("imageUrl", school.getImageUrl());
        // 返回实际使用的年份（如果当前年份无数据，会自动回退到有数据的年份）
        if (emissionMap.containsKey("actualYear")) {
            map.put("actualYear", emissionMap.get("actualYear"));
        } else {
            map.put("actualYear", (double) requestYear);
        }
        return ResultUtils.success(map);
    }

    /**
     * 获取学校信息（用于管理界面）
     * @return
     */
    @RateLimit(ipLimit = 30, apiLimit = 100, timeWindow = 1)
    @GetMapping("/getSchoolInfo")
    @PreAuthorize("hasAuthority('SCHOOL_MANAGE_QUERY')")
    public Object getSchoolInfo(){
        School school = schoolService.getOne(new LambdaQueryWrapper<School>().eq(School::getSchoolName, "北京林业大学"));
        if (school == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "学校信息不存在");
        }
        return ResultUtils.success(school);
    }

    /**
     * 更新学校信息
     * @param school 学校信息
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/updateSchoolInfo")
    @PreAuthorize("hasAuthority('SCHOOL_MANAGE_UPDATE')")
    public Object updateSchoolInfo(@RequestBody School school){
        School existingSchool = schoolService.getOne(new LambdaQueryWrapper<School>().eq(School::getSchoolName, "北京林业大学"));
        if (existingSchool == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "学校信息不存在");
        }
        school.setId(existingSchool.getId());
        boolean success = schoolService.updateById(school);
        if (success) {
            return ResultUtils.success("更新成功");
        } else {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
    }
}

package com.bjfu.carbon.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.PlaceInfo;
import com.bjfu.carbon.service.PlaceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

/**
 * Describe: placeInfo controller layer
 * Author：Xie geyun
 * Time：2023/5/13
 */
@Slf4j
@RestController
@RequestMapping("/placeInfo")
public class PlaceInfoController {

    @Autowired
    private PlaceInfoService placeInfoService;

    /**
     * 查询所有地点
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getPlace")
    @PreAuthorize("hasAuthority('PLACE_MANAGE_QUERY')")
    public Object getPlace(){
        List<PlaceInfo> list = placeInfoService.list();
        list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return ResultUtils.success(list);
    }

    /**
     * 分页查询所有地点
     * @param current 当前页
     * @param size 每页大小
     * @param name 地点名称（可选，用于搜索）
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getAllPlaceInfo")
    @PreAuthorize("hasAuthority('PLACE_MANAGE_QUERY')")
    public IPage<PlaceInfo> getAllPlaceInfo(int current, int size, String name){
        QueryWrapper<PlaceInfo> qw = new QueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            qw.like("name", name);
        }
        qw.orderByAsc("name");
        IPage<PlaceInfo> page = new Page<>(current, size);
        return placeInfoService.page(page, qw);
    }

    /**
     * 添加地点信息
     * @param placeInfo 地点信息
     * @param results 验证结果
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/addPlaceInfo")
    @PreAuthorize("hasAuthority('PLACE_MANAGE_ADD')")
    public Object addPlaceInfo(@RequestBody @Valid PlaceInfo placeInfo, BindingResult results){
        if(results.hasErrors())
            return ResultUtils.error(ErrorCode.NULL_ERROR, Objects.requireNonNull(results.getFieldError()).getDefaultMessage());

        // 检查地点名称是否已存在
        QueryWrapper<PlaceInfo> qw = new QueryWrapper<>();
        qw.eq("name", placeInfo.getName());
        PlaceInfo existing = placeInfoService.getOne(qw);
        if (existing != null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "地点名称已存在！");
        }

        boolean flag = placeInfoService.save(placeInfo);
        if(!flag)
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "添加失败！");
        return ResultUtils.success("添加成功！");
    }

    /**
     * 更新地点信息
     * @param placeInfo 地点信息
     * @param results 验证结果
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/updatePlaceInfo")
    @PreAuthorize("hasAuthority('PLACE_MANAGE_UPDATE')")
    public Object updatePlaceInfo(@RequestBody @Valid PlaceInfo placeInfo, BindingResult results){
        if(results.hasErrors())
            return ResultUtils.error(ErrorCode.NULL_ERROR, Objects.requireNonNull(results.getFieldError()).getDefaultMessage());

        if(placeInfo.getId() == null)
            return ResultUtils.error(ErrorCode.NULL_ERROR, "地点ID不能为空");

        // 检查地点名称是否已被其他记录使用
        QueryWrapper<PlaceInfo> qw = new QueryWrapper<>();
        qw.eq("name", placeInfo.getName());
        qw.ne("id", placeInfo.getId());
        PlaceInfo existing = placeInfoService.getOne(qw);
        if (existing != null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "地点名称已被其他记录使用！");
        }

        boolean flag = placeInfoService.updateById(placeInfo);
        if(!flag)
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "修改失败！");
        return ResultUtils.success("修改成功！");
    }

    /**
     * 根据ID删除地点信息
     * @param placeInfoId 地点ID
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/deletePlaceInfoById")
    @PreAuthorize("hasAuthority('PLACE_MANAGE_DELETE')")
    public Object deletePlaceInfoById(String placeInfoId){
        if(StringUtils.isEmpty(placeInfoId))
            return ResultUtils.error(ErrorCode.NULL_ERROR, "删除数据id为空");

        PlaceInfo placeInfo = placeInfoService.getById(placeInfoId);
        if(placeInfo == null)
            return ResultUtils.error(ErrorCode.NULL_ERROR, "删除数据不存在或已被删除");

        boolean flag = placeInfoService.removeById(placeInfoId);
        if(!flag)
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "删除失败！");
        return ResultUtils.success("删除成功！");
    }

}


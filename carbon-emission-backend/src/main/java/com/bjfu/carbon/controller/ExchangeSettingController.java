package com.bjfu.carbon.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.ExchangeSetting;
import com.bjfu.carbon.service.ExchangeSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Objects;

/**
 * Describe: exchangeSetting controller layer
 * Author：Xie geyun
 * Time：2023/5/13
 */
@Slf4j
@RestController
@RequestMapping("/exchangeSetting")
public class ExchangeSettingController {

    @Autowired
    private ExchangeSettingService exchangeSettingService;

    /**
     * 增
     * @param exchangeSetting
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/addExchangeSetting")
    @PreAuthorize("hasAuthority('EXCHANGE_SETTING_ADD')")
    public Object addExchangeSetting(@Valid @RequestBody ExchangeSetting exchangeSetting, BindingResult result){
        if(result.hasErrors())
            return ResultUtils.error(ErrorCode.NULL_ERROR, Objects.requireNonNull(result.getFieldError()).getDefaultMessage());
        return exchangeSettingService.save(exchangeSetting)?ResultUtils.success("新增成功"):ResultUtils.success("新增失败");
    }

    /**
     * 删
     * @param exchangeSetting
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/deleteExchangeSetting")
    @PreAuthorize("hasAuthority('EXCHANGE_SETTING_DELETE')")
    public Object deleteExchangeSetting(@RequestBody ExchangeSetting exchangeSetting){
        if(exchangeSetting==null || exchangeSetting.getId()==null)
            return ResultUtils.error(ErrorCode.NULL_ERROR,"删除id为空");
        return exchangeSettingService.removeById(exchangeSetting)?ResultUtils.success("删除成功"):ResultUtils.success("删除失败");
    }
    /**
     * 改
     * @param exchangeSetting
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/updateExchangeSetting")
    @PreAuthorize("hasAuthority('EXCHANGE_SETTING_UPDATE')")
    public Object updateExchangeSetting(@RequestBody ExchangeSetting exchangeSetting){
        System.out.println(exchangeSetting);
        if(exchangeSetting==null || exchangeSetting.getId()==null)
            return ResultUtils.error(ErrorCode.NULL_ERROR,"修改id为空");
        return exchangeSettingService.updateById(exchangeSetting)?ResultUtils.success("修改成功"):ResultUtils.success("修改失败");
    }

    /**
     * 查
     * @return
     */
    @RateLimit(ipLimit = 30, apiLimit = 100, timeWindow = 1)
    @GetMapping("/getAll")
    @PreAuthorize("hasAuthority('EXCHANGE_SETTING_QUERY')")
    public Object getAll(){
        return ResultUtils.success(exchangeSettingService.list());
    }
    /**
     * 查
     * @param current
     * @param size
     * @param objectCategory
     * @return
     */
    @RateLimit(ipLimit = 30, apiLimit = 100, timeWindow = 1)
    @GetMapping("/getAllExchangeSetting")
    @PreAuthorize("hasAuthority('EXCHANGE_SETTING_QUERY')")
    public IPage<ExchangeSetting> getAllExchangeSetting(int current, int size, String objectCategory){
        IPage<ExchangeSetting> page = new Page<>(current,size);
        QueryWrapper<ExchangeSetting> wrapper =new QueryWrapper<>();
        if(objectCategory!=null && objectCategory!="")
            wrapper.like("object_category",objectCategory);
        return exchangeSettingService.page(page,wrapper);
    }

    /**
     * 查
     * @param id
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getExchangeSetting")
    @PreAuthorize("hasAuthority('EXCHANGE_SETTING_QUERY')")
    public Object getExchangeSetting(String id){
        return ResultUtils.success(exchangeSettingService.getById(id));
    }
}

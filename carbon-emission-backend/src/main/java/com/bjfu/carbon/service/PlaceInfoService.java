package com.bjfu.carbon.service;

import com.bjfu.carbon.domain.PlaceInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bjfu.carbon.vo.PlaceEmissionVo;

import java.util.List;

/**
 * 地点信息服务接口
 */
public interface PlaceInfoService extends IService<PlaceInfo> {
    List<PlaceEmissionVo> selectEmission();
}


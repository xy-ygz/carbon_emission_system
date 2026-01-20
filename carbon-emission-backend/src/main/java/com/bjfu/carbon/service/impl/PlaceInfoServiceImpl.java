package com.bjfu.carbon.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjfu.carbon.domain.PlaceInfo;
import com.bjfu.carbon.service.PlaceInfoService;
import com.bjfu.carbon.mapper.PlaceInfoMapper;
import com.bjfu.carbon.vo.PlaceEmissionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 地点信息服务实现类
 */
@Service
public class PlaceInfoServiceImpl extends ServiceImpl<PlaceInfoMapper, PlaceInfo>
    implements PlaceInfoService{
    @Autowired
    private PlaceInfoMapper placeInfoMapper;


    @Override
    public List<PlaceEmissionVo> selectEmission() {
        return placeInfoMapper.selectEmission();
    }
}


package com.bjfu.carbon.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjfu.carbon.domain.ExchangeSetting;
import com.bjfu.carbon.service.ExchangeSettingService;
import com.bjfu.carbon.mapper.ExchangeSettingMapper;
import com.bjfu.carbon.utils.CarbonUtils;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 *
 */
@Service
public class ExchangeSettingServiceImpl extends ServiceImpl<ExchangeSettingMapper, ExchangeSetting>
    implements ExchangeSettingService{

    @Override
    public boolean save(ExchangeSetting entity) {
        boolean result = super.save(entity);
        if (result) {
            CarbonUtils.getInstance().refreshCache();
        }
        return result;
    }

    @Override
    public boolean updateById(ExchangeSetting entity) {
        boolean result = super.updateById(entity);
        if (result) {
            CarbonUtils.getInstance().refreshCache();
        }
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean result = super.removeById(id);
        if (result) {
            CarbonUtils.getInstance().refreshCache();
        }
        return result;
    }
}





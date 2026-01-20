package com.bjfu.carbon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjfu.carbon.domain.CarbonEmission;
import com.bjfu.carbon.service.CarbonEmissionService;
import com.bjfu.carbon.utils.CarbonUtils;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
@Slf4j
public class CarbonApplicationTests {

    @Autowired
    CarbonEmissionService carbonEmissionService;

    @Test
    public void test() {
        CarbonEmission c = carbonEmissionService.getOne(new LambdaQueryWrapper<CarbonEmission>()
                .eq(CarbonEmission::getId, 90));
        System.out.println(CarbonUtils.getInstance().countCarbonEmission(c));
    }

    @Test
    public void test1() {
        List<CarbonEmission> list = carbonEmissionService.list(new LambdaQueryWrapper<CarbonEmission>().eq(CarbonEmission::getYear,2023));
        System.out.println("list:"+ JSONObject.toJSONString(list));
        Map<String, Double> stringDoubleMap = carbonEmissionService.collegeCarbonEmission(2023,null);
        System.out.println(stringDoubleMap);
        System.out.println("-----------------------");
//        System.out.println(CarbonUtils.getInstance().countAllCarbonEmission(list));

    }
}
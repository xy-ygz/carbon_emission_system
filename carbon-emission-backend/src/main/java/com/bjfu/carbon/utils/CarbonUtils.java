package com.bjfu.carbon.utils;

import com.bjfu.carbon.domain.CarbonEmission;
import com.bjfu.carbon.domain.ExchangeSetting;
import com.bjfu.carbon.mapper.CarbonEmissionMapper;
import com.bjfu.carbon.service.ExchangeSettingService;
import com.bjfu.carbon.vo.CarbonConsumptionVo;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CarbonUtils {
    private static volatile CarbonUtils instance;
    private volatile ConcurrentHashMap<String, Double> exchangeCoefficientMap;
    private final ExchangeSettingService exchangeSettingService;
    private final CarbonEmissionMapper carbonEmissionMapper;
    private final int fallbackYears;

    public static CarbonUtils getInstance() {
        if (instance == null) {
            synchronized (CarbonUtils.class) {
                if (instance == null) {
                    instance = new CarbonUtils();
                }
            }
        }
        return instance;
    }

    private CarbonUtils() {
        this.exchangeSettingService = getExchangeSettingService();
        this.carbonEmissionMapper = getCarbonEmissionMapper();
        this.fallbackYears = getFallbackYears();
        this.exchangeCoefficientMap = loadExchangeCoefficientMap("initialize");
    }


    /**
     * 返回物品对应的碳排放数值
     */
    public double countCarbonEmission(CarbonEmission c) {
        if (c == null || c.getCategory() == null || c.getConsumption() == null || exchangeCoefficientMap == null) {
            return 0.0;
        }
        return exchangeCoefficientMap.getOrDefault(c.getCategory(),0.0) * c.getConsumption();
    }

    /**
     * 得到carbonEmissionList对应的总碳排放量(计算值)
     */
    public double countAllCarbonEmission(List<CarbonConsumptionVo> cList) {
        if (cList == null || cList.isEmpty() || exchangeCoefficientMap == null) {
            return 0.0;
        }

        return cList.stream()
                .filter(c -> c != null && c.getCategory() != null && c.getConsumptionCount() != null)
                .mapToDouble(c -> exchangeCoefficientMap.getOrDefault(c.getCategory(),0.0) * c.getConsumptionCount())
                .sum();
    }

    /**
     * 获取近 fallbackYears 实际有数据的年份
     * @return 实际有数据的年份，如果没有数据则返回-1
     */
    public int getActualYear() {
        if (carbonEmissionMapper == null) {
            return -1;
        }

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<Integer> yearsWithData = carbonEmissionMapper.selectYearsWithData(currentYear - fallbackYears, currentYear);

        if (yearsWithData != null && !yearsWithData.isEmpty()) {
            return yearsWithData.get(0);
        }

        return -1;
    }



    /**
     * 加载碳排放转化系数
     */
    private ConcurrentHashMap<String, Double> loadExchangeCoefficientMap(String operation) {
        try {
            if (exchangeSettingService != null) {
                List<ExchangeSetting> settings = exchangeSettingService.list();
                if (settings != null && !settings.isEmpty()) {
                    return settings.stream()
                            .collect(Collectors.toMap(
                                    ExchangeSetting::getObjectCategory,
                                    ExchangeSetting::getExchangeCoefficient,
                                    (existing, replacement) -> replacement,
                                    ConcurrentHashMap::new));
                }
            }
            return new ConcurrentHashMap<>();
        } catch (Exception e) {
            System.err.println("Failed to " + operation + " CarbonUtils: " + e.getMessage());
            return new ConcurrentHashMap<>();
        }
    }

    public synchronized void refreshCache() {
        this.exchangeCoefficientMap = loadExchangeCoefficientMap("refresh");
    }

    private ExchangeSettingService getExchangeSettingService() {
        try {
            /*
              单例模式与Spring容器管理的冲突:
                @Autowired 只有在Spring容器创建和管理Bean时才会生效
                手动 new 出来的对象，Spring不会为其注入依赖
            */
            return ApplicationContextHelperUtil.getBean(ExchangeSettingService.class);
        } catch (Exception e) {
            System.err.println("Failed to get ExchangeSettingService from Spring context: " + e.getMessage());
            return null;
        }
    }

    private CarbonEmissionMapper getCarbonEmissionMapper() {
        try {
            return ApplicationContextHelperUtil.getBean(CarbonEmissionMapper.class);
        } catch (Exception e) {
            System.err.println("Failed to get CarbonEmissionMapper from Spring context: " + e.getMessage());
            return null;
        }
    }

    private int getFallbackYears() {
        try {
            // 加载动态配置
            org.springframework.core.env.Environment env = ApplicationContextHelperUtil.getBean(org.springframework.core.env.Environment.class);
            return env.getProperty("carbon.fallback-years", Integer.class, 3);
        } catch (Exception e) {
            // 默认值
            return 3;
        }
    }
}

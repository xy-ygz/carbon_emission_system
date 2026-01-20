package com.bjfu.carbon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.Result;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.CarbonEmission;
import com.bjfu.carbon.domain.EmissionAndConsume;
import com.bjfu.carbon.domain.PlaceInfo;
import com.bjfu.carbon.domain.School;
import com.bjfu.carbon.mapper.CarbonEmissionMapper;
import com.bjfu.carbon.service.CarbonEmissionService;
import com.bjfu.carbon.service.PlaceInfoService;
import com.bjfu.carbon.service.SchoolService;
import com.bjfu.carbon.strategy.ImportStrategy;
import com.bjfu.carbon.strategy.ImportStrategyFactory;
import com.bjfu.carbon.utils.CarbonUtils;
import com.bjfu.carbon.vo.CarbonBuildingBarVo;
import com.bjfu.carbon.vo.CarbonBuildingVo;
import com.bjfu.carbon.vo.CarbonConsumptionVo;
import com.bjfu.carbon.vo.EmissionCategoryTimePeriodVo;
import com.bjfu.carbon.vo.EmissionMulberryVo;
import com.bjfu.carbon.vo.EmissionVo;
import com.bjfu.carbon.vo.MonthEmissionVo;
import com.bjfu.carbon.vo.MulberryDiagramVo;
import com.bjfu.carbon.vo.PlaceEmissionVo;
import com.bjfu.carbon.vo.SubCarbonBuildingBarVo;
import com.bjfu.carbon.vo.SubPlaceEmissionVo;
import com.bjfu.carbon.vo.TimeEmissionVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
@Slf4j
@Service
public class CarbonEmissionServiceImpl extends ServiceImpl<CarbonEmissionMapper, CarbonEmission> implements CarbonEmissionService{
    @Autowired
    private CarbonEmissionService carbonEmissionService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private PlaceInfoService placeInfoService;

    @Autowired
    private CarbonEmissionMapper carbonEmissionMapper;

    @Autowired
    private ImportStrategyFactory importStrategyFactory;

    @Override
    public Map<String, Double> collegeCarbonEmission(int year, Integer month) {
        Map<String, Double> map = new HashMap<>();
        double totalEmission = 0.0;

        if (year == -1) {
            map.put("totalEmission", 0.0);
            map.put("actualYear", (double) year);
        } else {
            Double result = carbonEmissionMapper.selectTotalEmission(year, month);
            totalEmission = result != null ? result : 0.0;
            map.put("totalEmission", totalEmission);
            map.put("actualYear", (double) year);
        }

        // 人均和地均碳排放
        School school = schoolService.list().stream().findFirst().orElse(null);
        if (school == null) {
            map.put("personAverageEmission", 0.0);
            map.put("groundAverageEmission", 0.0);
        }else{
            map.put("personAverageEmission", school.getTotalNumber() != null && school.getTotalNumber() > 0 ? totalEmission / school.getTotalNumber() : 0.0);
            map.put("groundAverageEmission", school.getTotalArea() != null && school.getTotalArea() > 0 ? totalEmission / school.getTotalArea() : 0.0);
        }

        return map;
    }

    @Override
    public List<CarbonEmission> selectAllCarbonEmission(int year, Integer month) {
        if (year == -1) {
            return new ArrayList<>();
        }
        return carbonEmissionMapper.selectAllCarbonEmission(year, month);
    }

    @Override
    public List<CarbonEmission> selectAllCarbonEmissionByTimeRange(int startYear, int startMonth, int endYear, int endMonth) {
        return carbonEmissionMapper.selectAllCarbonEmissionByTimeRange(startYear, startMonth, endYear, endMonth);
    }

    @Override
    public IPage<CarbonEmission> pageByObjectName(int current, int size, String name, String year, String month) {
        Page<CarbonEmission> page = new Page<>(current, size);

        LambdaQueryWrapper<CarbonEmission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(name), CarbonEmission::getName, name);
        if (StringUtils.hasText(year)) {
            queryWrapper.eq(CarbonEmission::getYear, Integer.parseInt(year));
        }
        if (StringUtils.hasText(month)) {
            queryWrapper.eq(CarbonEmission::getMonth, Integer.parseInt(month));
        }

        return carbonEmissionMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Result<Map<String, Object>> getMulberryData(int year, Integer month) {
        List<CarbonEmission> carbonEmissions = carbonEmissionService.selectAllCarbonEmission(year, month);

        if(carbonEmissions == null || carbonEmissions.isEmpty()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        List<MulberryDiagramVo> list = carbonEmissions.stream()
                .collect(Collectors.groupingBy(CarbonEmission::getCategory,
                        Collectors.groupingBy(CarbonEmission::getEmissionType,
                                Collectors.summingDouble(c -> CarbonUtils.getInstance().countCarbonEmission(c)))))
                // {分类-> {排放类型 => 全年排放量}}
                .entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<EmissionMulberryVo> emissionVos = entry.getValue().entrySet().stream()
                            .map(emissionEntry -> new EmissionMulberryVo(emissionEntry.getKey(), emissionEntry.getValue()))
                            .collect(Collectors.toList());
                    return new MulberryDiagramVo(category, emissionVos);
                })
                .collect(Collectors.toList());
        
        Map<String,Object> result = new HashMap<>();
        result.put("list", list);
        result.put("actualYear", year);
        return ResultUtils.success(result);
    }

    @Override
    public Result<List<EmissionCategoryTimePeriodVo>> getEmissionCategoryData(Integer startYear, Integer startMonth, Integer endYear, Integer endMonth) {
        List<CarbonEmission> carbonEmissions = carbonEmissionService.selectAllCarbonEmissionByTimeRange(startYear, startMonth, endYear, endMonth);

        if (carbonEmissions == null || carbonEmissions.isEmpty()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        List<EmissionCategoryTimePeriodVo> result = carbonEmissions.stream()
                 // {分类 -> {年月字符串 -> 排放量}}
                .collect(Collectors.groupingBy(
                        CarbonEmission::getCategory,
                        Collectors.groupingBy(
                                c -> c.getYear() + "年" + c.getMonth() + "月",
                                Collectors.summingDouble(c -> CarbonUtils.getInstance().countCarbonEmission(c))
                        )
                ))
                .entrySet().stream()
                .map(categoryEntry -> {
                    String category = categoryEntry.getKey();
                    List<TimeEmissionVo> timeEmissionList = categoryEntry.getValue().entrySet().stream()
                            .map(timeEntry -> new TimeEmissionVo(timeEntry.getKey(), timeEntry.getValue()))
                            .collect(Collectors.toList());
                    return new EmissionCategoryTimePeriodVo(category, timeEmissionList);
                })
                .collect(Collectors.toList());

        return ResultUtils.success(result);
    }

    @Override
    public Result<Map<String, Object>> getSpeciesConsumptionData(Integer year, Integer month) {
        List<CarbonEmission> carbonEmissions = carbonEmissionService.selectAllCarbonEmission(year, month);

        if (carbonEmissions == null || carbonEmissions.isEmpty()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        List<CarbonConsumptionVo> list = carbonEmissions.stream()
                // 分类 -> 消耗总量
                .collect(Collectors.groupingBy(
                        CarbonEmission::getCategory,
                        Collectors.summingDouble(CarbonEmission::getConsumption)
                ))
                .entrySet().stream()
                .map(entry -> new CarbonConsumptionVo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("actualYear", year);
        return ResultUtils.success(result);
    }

    @Override
    public Result<Map<String, Object>> getSpeciesCarbonData(Integer year, Integer month) {
        List<CarbonEmission> carbonEmissions = carbonEmissionService.selectAllCarbonEmission(year, month);

        if (carbonEmissions == null || carbonEmissions.isEmpty()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        List<EmissionVo> list = carbonEmissions.stream()
                // 分类 -> 碳排放总量
                .collect(Collectors.groupingBy(
                        CarbonEmission::getCategory,
                        Collectors.summingDouble(c -> CarbonUtils.getInstance().countCarbonEmission(c))
                ))
                .entrySet().stream()
                .map(entry -> new EmissionVo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("actualYear", year);
        return ResultUtils.success(result);
    }

    @Override
    public List<EmissionAndConsume> selectEmiAndConsume(int year) {
        return carbonEmissionMapper.selectEmiAndConsume(year);
    }

    @Override
    public List<CarbonEmission> selectEmissionType(int year) {
        return carbonEmissionMapper.selectEmissionType(year);
    }

    @Override
    public Result<List<CarbonBuildingVo>> getBuildingCarbonLineData(Integer year, boolean area) {
        List<CarbonEmission> carbonEmissions = carbonEmissionService.selectAllCarbonEmission(year, null);

        if (carbonEmissions == null || carbonEmissions.isEmpty()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        // 获取地点面积Map
        Map<String, Double> buildingAreaMap = placeInfoService.list().stream()
                .collect(Collectors.toMap(PlaceInfo::getName, PlaceInfo::getArea));


        List<CarbonBuildingVo> list = carbonEmissions.stream()
                // {地点 -> {年份 -> {月份 -> 碳排放量}}}
                .collect(Collectors.groupingBy(
                        CarbonEmission::getPlace,
                        Collectors.groupingBy(
                                CarbonEmission::getYear,
                                Collectors.groupingBy(
                                        CarbonEmission::getMonth,
                                        Collectors.summingDouble(c -> CarbonUtils.getInstance().countCarbonEmission(c))
                                )
                        )
                ))
                .entrySet().stream()
                .flatMap(buildingEntry -> {
                    String building = buildingEntry.getKey();
                    Double buildingArea = buildingAreaMap.get(building);
                    return buildingEntry.getValue().entrySet().stream()
                            .map(yearEntry -> {
                                Integer yearValue = yearEntry.getKey();
                                // 根据 area 判断是否除以面积
                                List<MonthEmissionVo> monthEmissionList = yearEntry.getValue().entrySet().stream()
                                        .map(monthEntry -> {
                                            Integer month = monthEntry.getKey();
                                            Double amount = monthEntry.getValue();
                                            // 如果 area为true，计算地均排放（四舍五入到两位小数）
                                            Double finalAmount = area && buildingArea != null && buildingArea > 0
                                                    ? Math.round(amount / buildingArea * 100) * 0.01
                                                    : amount;
                                            return new MonthEmissionVo(month, finalAmount);
                                        })
                                        .sorted((a, b) -> b.getMonth().compareTo(a.getMonth())) // 按月份降序排序
                                        .collect(Collectors.toList());
                                return new CarbonBuildingVo(building, yearValue, monthEmissionList);
                            });
                })
                .collect(Collectors.toList());

        return ResultUtils.success(list);
    }

    @Override
    public Result<List<CarbonBuildingBarVo>> getBuildingCarbonBarData(Integer year, Integer month, boolean area) {
        List<CarbonEmission> carbonEmissions = carbonEmissionService.selectAllCarbonEmission(year, month);

        if (carbonEmissions == null || carbonEmissions.isEmpty()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        // 按地点分组，只计算"电力"类别的碳排放量
        Map<String, Double> buildingEmission = carbonEmissions.stream()
                .filter(c -> "电力".equals(c.getCategory()))
                .collect(Collectors.groupingBy(
                        CarbonEmission::getPlace,
                        Collectors.summingDouble(c -> CarbonUtils.getInstance().countCarbonEmission(c))
                ));

        // 根据 area 参数决定是否计算地均排放
        List<SubCarbonBuildingBarVo> subCarbonBuildingBarVos;
        if (!area) {
            // 直接返回地点排放量
            subCarbonBuildingBarVos = buildingEmission.entrySet().stream()
                    .map(entry -> new SubCarbonBuildingBarVo(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
        } else {
            // 计算地均排放（除以地点面积）
            Map<String, Double> buildingAreaMap = placeInfoService.list().stream()
                    .collect(Collectors.toMap(PlaceInfo::getName, PlaceInfo::getArea));

            subCarbonBuildingBarVos = buildingEmission.entrySet().stream()
                    .map(entry -> {
                        String building = entry.getKey();
                        Double emission = entry.getValue();
                        Double buildingArea = buildingAreaMap.get(building);
                        // 小数取后两位四舍五入
                        double finalAmount = (buildingArea != null && buildingArea > 0)
                                ? Math.round(emission / buildingArea * 100) * 0.01
                                : 0.0;
                        return new SubCarbonBuildingBarVo(building, finalAmount);
                    })
                    .collect(Collectors.toList());
        }

        List<CarbonBuildingBarVo> result = new ArrayList<>();
        result.add(new CarbonBuildingBarVo(year, month, subCarbonBuildingBarVos));

        return ResultUtils.success(result);
    }

    @Override
    public Result<List<PlaceEmissionVo>> getBuildingInfoData(Integer year, Integer month) {
        // 查询碳排放数据
        List<CarbonEmission> carbonEmissions = carbonEmissionService.selectAllCarbonEmission(year, month);

        // 获取地点面积信息
        List<PlaceInfo> placeInfos = placeInfoService.list();
        if (carbonEmissions == null || carbonEmissions.isEmpty() || placeInfos == null || placeInfos.isEmpty()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请求数据为空");
        }

        // 构建地点面积Map
        Map<String, Double> buildingAreaMap = placeInfos.stream()
                .collect(Collectors.toMap(PlaceInfo::getName, PlaceInfo::getArea));

        // 按地点分组，计算累积排放量（所有类别）
        Map<String, Double> buildingEmissionMap = carbonEmissions.stream()
                .collect(Collectors.groupingBy(
                        CarbonEmission::getPlace,
                        Collectors.summingDouble(c -> CarbonUtils.getInstance().countCarbonEmission(c))
                ));

        // 按地点分组，计算累积电力消耗量（只计算"电力"类别）
        Map<String, Double> buildingEleConsumptionMap = carbonEmissions.stream()
                .filter(c -> "电力".equals(c.getCategory()))
                .collect(Collectors.groupingBy(
                        CarbonEmission::getPlace,
                        Collectors.summingDouble(CarbonEmission::getConsumption)
                ));

        List<SubPlaceEmissionVo> subList = buildingEleConsumptionMap.entrySet().stream()
                .map(entry -> {
                    String building = entry.getKey();
                    Double powerConsumption = entry.getValue();
                    Double emissionAmount = buildingEmissionMap.getOrDefault(building, 0.0);
                    Double buildingArea = buildingAreaMap.get(building);
                    // 计算单位面积排放量（小数取后两位四舍五入）
                    double placePerArea = (buildingArea != null && buildingArea > 0)
                            ? Math.round(emissionAmount / buildingArea * 100) * 0.01
                            : 0.0;
                    return new SubPlaceEmissionVo(building, powerConsumption, emissionAmount, placePerArea);
                })
                .collect(Collectors.toList());

        List<PlaceEmissionVo> result = new ArrayList<>();
        result.add(new PlaceEmissionVo(year, month, subList));

        return ResultUtils.success(result);
    }

    /**
     * 验证排放类型是否有效（0/1/2:直接排放/间接排放/其他排放）
     * @param emissionType 排放类型
     * @return true表示有效，false表示无效
     */
    private boolean isValidEmissionType(Integer emissionType) {
        if (emissionType == null) {
            return false;
        }
        String regex = "[0-2]";
        return Pattern.matches(regex, emissionType.toString());
    }

    /**
     * 验证年份和月份是否有效
     * @param year 年份
     * @param month 月份
     * @return true表示有效，false表示无效
     */
    private boolean isValidYearAndMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            return false;
        }
        // 年份必须是4位数
        if (year.toString().length() != 4) {
            return false;
        }
        // 月份必须在1-12之间
        return month >= 1 && month <= 12;
    }

    @Override
    public Result<String> addCarbonEmission(CarbonEmission carbonEmission) {
        // 验证排放类型
        if (!isValidEmissionType(carbonEmission.getEmissionType())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "添加失败，排放类型须为数字(0/1/2:直接排放/间接排放/其他排放)");
        }

        // 验证年份和月份
        if (!isValidYearAndMonth(carbonEmission.getYear(), carbonEmission.getMonth())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "添加失败，排放时间(年/月)输入有误");
        }

        // 根据碳排放转化系数计算碳排放量
        if (carbonEmission.getConsumption() != null) {
            carbonEmission.setAmount(CarbonUtils.getInstance().countCarbonEmission(carbonEmission));
        }

        // 保存数据
        boolean success = this.save(carbonEmission);
        if (!success) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "添加失败！");
        }

        return ResultUtils.success("添加成功！");
    }

    @Override
    public Result<String> updateCarbonEmission(CarbonEmission carbonEmission) {
        // 验证排放类型
        if (!isValidEmissionType(carbonEmission.getEmissionType())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "修改失败，排放类型须为数字(0/1/2:直接排放/间接排放/其他排放)");
        }

        // 验证年份和月份
        if (!isValidYearAndMonth(carbonEmission.getYear(), carbonEmission.getMonth())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "修改失败，排放时间(年/月)输入有误");
        }

        // 使用 CarbonUtils 自动计算碳排放量
        if (carbonEmission.getConsumption() != null) {
            carbonEmission.setAmount(CarbonUtils.getInstance().countCarbonEmission(carbonEmission));
        }

        // 更新数据
        boolean success = this.updateById(carbonEmission);
        if (!success) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "修改失败！");
        }

        return ResultUtils.success("修改成功！");
    }

    @Override
    public Result<String> deleteCarbonEmissionById(String carbonEmissionId) {
        // 参数验证
        if (carbonEmissionId == null || carbonEmissionId.trim().isEmpty()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "删除数据id为空");
        }

        // 检查数据是否存在
        CarbonEmission carbonEmission = this.getById(carbonEmissionId);
        if (carbonEmission == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "删除数据不存在或已被删除");
        }

        // 删除数据
        boolean success = this.removeById(carbonEmissionId);
        if (!success) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "删除失败！");
        }

        return ResultUtils.success("删除成功！");
    }

    /**
     * Excel表头定义
     */
    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
            "名称", "分类", "消耗量", "用途", "排放类型", "地点", "年份", "月份"
    );

    /**
     * 验证数字格式的正则表达式（匹配整数或小数）
     */
    private static final String NUMBER_REGEX = "^\\d+(\\.\\d+)?$";

    @Override
    public Result<String> batchImportFromFile(InputStream fileInputStream, String filename) {
        try {
            // 根据文件名获取导入策略
            ImportStrategy strategy = importStrategyFactory.getStrategyByFilename(filename);
            
            // 策略模式读取数据
            List<Map<String, String>> fileData = strategy.readData(fileInputStream);
            
            if (fileData == null || fileData.isEmpty()) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件数据为空，请录入数据！");
            }

            // 验证表头
            Map<String, String> headerRow = fileData.get(0);
            String headerValidationError = validateHeaders(headerRow);
            if (headerValidationError != null) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, headerValidationError);
            }

            // 获取数据行（跳过表头）
            List<Map<String, String>> dataRows = fileData.stream()
                    .skip(1)
                    .collect(Collectors.toList());

            if (dataRows.isEmpty()) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件中没有数据行！");
            }

            // 验证并转换数据
            List<String> validationErrors = new ArrayList<>();
            List<CarbonEmission> carbonEmissionList = IntStream.range(0, dataRows.size())
                    .mapToObj(index -> {
                        Map<String, String> row = dataRows.get(index);
                        String rowError = validateDataRow(row, index + 2); // +2 因为跳过表头且从1开始计数
                        if (rowError != null) {
                            validationErrors.add(rowError);
                            return null;
                        }
                        return convertRowToCarbonEmission(row);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 如果有验证错误，返回错误信息
            if (!validationErrors.isEmpty()) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, 
                        "数据验证失败：\n" + String.join("\n", validationErrors));
            }

            // 批量计算碳排放量
            carbonEmissionList.forEach(carbonEmission -> {
                if (carbonEmission.getConsumption() != null) {
                    carbonEmission.setAmount(CarbonUtils.getInstance().countCarbonEmission(carbonEmission));
                }
            });

            // 批量保存
            boolean success = this.saveBatch(carbonEmissionList);
            if (!success) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, "导入失败，请检查录入数据！");
            }

            return ResultUtils.success("批量导入成功！共导入 " + carbonEmissionList.size() + " 条记录");
        } catch (RuntimeException e) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, e.getMessage());
        } catch (Exception e) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "导入失败：" + e.getMessage());
        }
    }

    /**
     * 验证文件表头
     */
    private String validateHeaders(Map<String, String> headerRow) {
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            String expectedHeader = EXPECTED_HEADERS.get(i);
            String actualHeader = headerRow.get("col" + i);
            if (!expectedHeader.equals(actualHeader)) {
                return String.format("表头不对，第%d列应为'%s'，实际为'%s'。请将录入数据列表头设为(%s)",
                        i + 1, expectedHeader, actualHeader, String.join("，", EXPECTED_HEADERS));
            }
        }
        return null;
    }

    /**
     * 验证数据行
     */
    private String validateDataRow(Map<String, String> row, int rowNumber) {
        // 验证物品消耗量 (col2)
        String consumption = row.get("col2");
        if (consumption == null || consumption.trim().isEmpty()) {
            return String.format("第%d行：物品消耗量不能为空", rowNumber);
        }
        if (!Pattern.matches(NUMBER_REGEX, consumption)) {
            return String.format("第%d行：物品消耗量格式错误，应为数字", rowNumber);
        }

        // 验证排放类型 (col4，因为col3是排放用途)
        String emissionType = row.get("col4");
        if (emissionType == null || emissionType.trim().isEmpty()) {
            return String.format("第%d行：排放类型不能为空", rowNumber);
        }
        if (!Pattern.matches(NUMBER_REGEX, emissionType)) {
            return String.format("第%d行：排放类型格式错误，应为数字(0/1/2)", rowNumber);
        }
        // 先解析为double，再转换为int（处理Excel中可能的小数格式如"0.0"）
        int type = (int) Double.parseDouble(emissionType);
        if (type < 0 || type > 2) {
            return String.format("第%d行：排放类型值错误，应为0/1/2(直接排放/间接排放/其他排放)", rowNumber);
        }

        // 验证年份 (col6)
        String year = row.get("col6");
        if (year == null || year.trim().isEmpty()) {
            return String.format("第%d行：排放时间(年)不能为空", rowNumber);
        }
        if (!Pattern.matches(NUMBER_REGEX, year)) {
            return String.format("第%d行：排放时间(年)格式错误，应为数字", rowNumber);
        }
        // 先解析为double，再转换为int（处理Excel中可能的小数格式）
        int yearValue = (int) Double.parseDouble(year);
        if (yearValue < 1900 || yearValue > 2100) {
            return String.format("第%d行：排放时间(年)值不合理，应在1900-2100之间", rowNumber);
        }

        // 验证月份 (col7)
        String month = row.get("col7");
        if (month == null || month.trim().isEmpty()) {
            return String.format("第%d行：排放时间(月)不能为空", rowNumber);
        }
        if (!Pattern.matches(NUMBER_REGEX, month)) {
            return String.format("第%d行：排放时间(月)格式错误，应为数字", rowNumber);
        }
        // 先解析为double，再转换为int（处理Excel中可能的小数格式）
        int monthValue = (int) Double.parseDouble(month);
        if (monthValue < 1 || monthValue > 12) {
            return String.format("第%d行：排放时间(月)值错误，应在1-12之间", rowNumber);
        }

        return null;
    }

    /**
     * 将文件行数据转换为CarbonEmission对象
     * 列映射：
     * col0: 名称 (name)
     * col1: 分类 (category)
     * col2: 消耗量 (consumption)
     * col3: 用途 (purpose)
     * col4: 排放类型 (emissionType)
     * col5: 地点 (place)
     * col6: 年份 (year)
     * col7: 月份 (month)
     */
    private CarbonEmission convertRowToCarbonEmission(Map<String, String> row) {
        CarbonEmission carbonEmission = new CarbonEmission();
        
        try {
            // col0: 名称
            String name = row.get("col0");
            if (name == null || name.trim().isEmpty()) {
                log.warn("转换数据时发现col0(名称)为空，原始数据: {}", row);
            }
            carbonEmission.setName(name != null ? name.trim() : "");
            
            // col1: 分类
            String category = row.get("col1");
            if (category == null || category.trim().isEmpty()) {
                log.warn("转换数据时发现col1(分类)为空，原始数据: {}", row);
            }
            carbonEmission.setCategory(category != null ? category.trim() : "");
            
            // col2: 消耗量
            try {
                String consumptionStr = row.get("col2");
                if (consumptionStr == null || consumptionStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("消耗量不能为空");
                }
                carbonEmission.setConsumption(Double.valueOf(consumptionStr.trim()));
            } catch (NumberFormatException e) {
                log.error("转换col2(消耗量)失败，原始值: '{}', 错误: {}", row.get("col2"), e.getMessage());
                throw new RuntimeException(String.format("第2列(消耗量)数据格式错误，值: '%s'，应为数字", row.get("col2")), e);
            } catch (Exception e) {
                log.error("转换col2(消耗量)时发生异常，原始值: '{}', 错误: {}", row.get("col2"), e.getMessage(), e);
                throw new RuntimeException(String.format("第2列(消耗量)转换失败，值: '%s'", row.get("col2")), e);
            }
            
            // col3: 用途（可为空）
            String purpose = row.get("col3");
            carbonEmission.setPurpose(purpose != null ? purpose.trim() : "");
            
            // col4: 排放类型转换为整数
            try {
                String emissionTypeStr = row.get("col4");
                if (emissionTypeStr == null || emissionTypeStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("排放类型不能为空");
                }
                double typeValue = Double.parseDouble(emissionTypeStr.trim());
                carbonEmission.setEmissionType((int) typeValue);
            } catch (NumberFormatException e) {
                log.error("转换col4(排放类型)失败，原始值: '{}', 错误: {}", row.get("col4"), e.getMessage());
                throw new RuntimeException(String.format("第4列(排放类型)数据格式错误，值: '%s'，应为数字(0/1/2)", row.get("col4")), e);
            } catch (Exception e) {
                log.error("转换col4(排放类型)时发生异常，原始值: '{}', 错误: {}", row.get("col4"), e.getMessage(), e);
                throw new RuntimeException(String.format("第4列(排放类型)转换失败，值: '%s'", row.get("col4")), e);
            }
            
            // col5: 地点
            String place = row.get("col5");
            carbonEmission.setPlace(place != null ? place.trim() : "");
            
            // col6: 年份转换为整数
            try {
                String yearStr = row.get("col6");
                if (yearStr == null || yearStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("年份不能为空");
                }
                double yearValue = Double.parseDouble(yearStr.trim());
                carbonEmission.setYear((int) yearValue);
            } catch (NumberFormatException e) {
                log.error("转换col6(年份)失败，原始值: '{}', 错误: {}", row.get("col6"), e.getMessage());
                throw new RuntimeException(String.format("第6列(年份)数据格式错误，值: '%s'，应为数字", row.get("col6")), e);
            } catch (Exception e) {
                log.error("转换col6(年份)时发生异常，原始值: '{}', 错误: {}", row.get("col6"), e.getMessage(), e);
                throw new RuntimeException(String.format("第6列(年份)转换失败，值: '%s'", row.get("col6")), e);
            }
            
            // col7: 月份转换为整数
            try {
                String monthStr = row.get("col7");
                if (monthStr == null || monthStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("月份不能为空");
                }
                double monthValue = Double.parseDouble(monthStr.trim());
                carbonEmission.setMonth((int) monthValue);
            } catch (NumberFormatException e) {
                log.error("转换col7(月份)失败，原始值: '{}', 错误: {}", row.get("col7"), e.getMessage());
                throw new RuntimeException(String.format("第7列(月份)数据格式错误，值: '%s'，应为数字", row.get("col7")), e);
            } catch (Exception e) {
                log.error("转换col7(月份)时发生异常，原始值: '{}', 错误: {}", row.get("col7"), e.getMessage(), e);
                throw new RuntimeException(String.format("第7列(月份)转换失败，值: '%s'", row.get("col7")), e);
            }
            
            return carbonEmission;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("转换行数据为CarbonEmission对象时发生未知异常，原始数据: {}, 错误: {}", row, e.getMessage(), e);
            throw new RuntimeException(String.format("数据转换失败，原始数据: %s，错误: %s", row, e.getMessage()), e);
        }
    }

    @Override
    public Map<String, String> getExportReportDataByYear(Integer year) {
        // 获取基础数据
        List<EmissionAndConsume> emissionAndConsumes = selectEmiAndConsume(year);
        List<CarbonEmission> carbonEmissions = selectEmissionType(year);
        
        // 数据验证
        if (emissionAndConsumes == null || emissionAndConsumes.isEmpty() ||
            carbonEmissions == null || carbonEmissions.isEmpty()) {
            throw new RuntimeException("该年份暂无数据");
        }
        
        // 获取学校信息
        Long totalPeople = schoolService.selectTotalPeople("北京林业大学");
        if (totalPeople == null) {
            totalPeople = 0L;
        }
        
        // 获取总体碳排放情况
        Map<String, Double> totalEmiMap = collegeCarbonEmission(year, null);
        
        // 构建结果Map
        Map<String, String> resultMap = new HashMap<>();
        
        // 1. 基础统计信息（从第一条记录获取）
        EmissionAndConsume firstRecord = emissionAndConsumes.get(0);
        resultMap.put("maxMonth", String.valueOf(firstRecord.getEmissionMonthMax()));
        resultMap.put("minMonth", String.valueOf(firstRecord.getEmissionMonthMin()));
        resultMap.put("maxCategory", firstRecord.getObjectCategoryMax());
        resultMap.put("minCategory", firstRecord.getObjectCategoryMin());
        resultMap.put("maxEmi", String.valueOf(firstRecord.getEmiMaxNumber()));
        resultMap.put("minEmi", String.valueOf(firstRecord.getEmiMinNumber()));
        
        // 2. 按排放类型分组计算总排放量
        Map<Byte, Double> emissionByType = emissionAndConsumes.stream()
                .collect(Collectors.groupingBy(
                        EmissionAndConsume::getEmissionType,
                        Collectors.summingDouble(e -> e.getEmissionAmount() != null ? e.getEmissionAmount() : 0.0)
                ));
        
        resultMap.put("dirEmiNumber", String.valueOf(emissionByType.getOrDefault((byte) 0, 0.0)));
        resultMap.put("indEmiNumber", String.valueOf(emissionByType.getOrDefault((byte) 1, 0.0)));
        resultMap.put("otherEmiNumber", String.valueOf(emissionByType.getOrDefault((byte) 2, 0.0)));
        
        // 3. 获取前5名排放源
        List<EmissionAndConsume> emissionTopFive = emissionAndConsumes.stream()
                .sorted(Comparator.comparingDouble((EmissionAndConsume e) -> 
                        e.getEmissionAmount() != null ? e.getEmissionAmount() : 0.0
                ).reversed())
                .limit(5)
                .collect(Collectors.toList());
        
        // 前5名数据
        IntStream.range(0, emissionTopFive.size())
                .forEach(i -> {
                    EmissionAndConsume item = emissionTopFive.get(i);
                    resultMap.put("EmissionName" + (i + 1), item.getObjectCategory());
                    resultMap.put("EmissionNumber" + (i + 1), String.valueOf(item.getEmissionAmount()));
                });
        
        // 4. 其他信息
        resultMap.put("totalPeople", String.valueOf(totalPeople));
        resultMap.put("year", String.valueOf(year));
        
        // 5. 格式化总体碳排放数据
        DecimalFormat df = new DecimalFormat("0.00");
        totalEmiMap.forEach((key, value) -> resultMap.put(key, df.format(value)));
        
        return resultMap;
    }

}

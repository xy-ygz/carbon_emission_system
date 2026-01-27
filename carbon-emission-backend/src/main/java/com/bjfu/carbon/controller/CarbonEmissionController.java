package com.bjfu.carbon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bjfu.carbon.annotation.RedisCache;
import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.CarbonEmission;
import com.bjfu.carbon.security.UserDetailsImpl;
import com.bjfu.carbon.service.AsyncExportService;
import com.bjfu.carbon.service.CarbonEmissionService;
import com.bjfu.carbon.service.PlaceInfoService;
import com.bjfu.carbon.service.SchoolService;
import com.bjfu.carbon.strategy.ExportStrategy;
import com.bjfu.carbon.strategy.ExportStrategyFactory;
import com.bjfu.carbon.utils.CachedHttpServletResponse;
import com.bjfu.carbon.utils.CarbonUtils;
import com.bjfu.carbon.utils.FileCacheUtils;
import com.bjfu.carbon.utils.ResourceService;
import com.bjfu.carbon.vo.CarbonConsumptionVo;
import com.bjfu.carbon.vo.ExportTask;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/carbonEmission")
public class CarbonEmissionController {
    @Autowired
    private CarbonEmissionService carbonEmissionService;
    @Autowired
    private PlaceInfoService placeInfoService;
    @Autowired
    private SchoolService schoolService;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private ExportStrategyFactory exportStrategyFactory;
    @Autowired
    private AsyncExportService asyncExportService;
    
    @Autowired
    private FileCacheUtils fileCacheUtils;


    /**
     * 计算碳排放总和
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping(value = "/countAllCarbonEmission", consumes = "application/json")
    public Object countAllCarbonEmission(@RequestBody List<CarbonConsumptionVo> cList){
        if (cList == null || cList.isEmpty()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        try {
            return ResultUtils.success(CarbonUtils.getInstance().countAllCarbonEmission(cList));
        } catch (Exception e) {
            log.error("计算碳排放量失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "计算失败：" + e.getMessage());
        }
    }

	/**
	 * 得到总碳排放量，人均，地均碳排放量
     * 若当年无数据，默认回退 fallbackYears
	 */
	@RateLimit(ipLimit = 30, apiLimit = 100, timeWindow = 1)
	@GetMapping("/collegeCarbonEmission")
	public Object collegeCarbonEmission(@RequestParam(required = false) String year, @RequestParam(required = false) Integer month) {
		if (month != null && (month <= 0 || month > 12))
			return ResultUtils.error(ErrorCode.NULL_ERROR, "请输入正确的月份");

        int actualYear = year == null ? CarbonUtils.getInstance().getActualYear() : Integer.parseInt(year);
		Map<String, Double> map = carbonEmissionService.collegeCarbonEmission(actualYear, month);
        return ResultUtils.success(map);
    }

    /**
     * 桑葚图 => 指定年份不同物品分类对应的排放类型及排放量
     */
    @RateLimit(ipLimit = 30, apiLimit = 100, timeWindow = 1)
    @GetMapping("/mulberryDiagram")
    public Object mublerryDiagram(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month) {
        if (month != null && (month <= 0 || month > 12))
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请输入正确的月份");

        int actualYear = year == null ? CarbonUtils.getInstance().getActualYear() : year;
        return carbonEmissionService.getMulberryData(actualYear, month);
    }

    /**
     * 柱状堆叠图 => 指定起始年份-起始月份~终止年份-终止月份对应的每个月份不同物品分类对应的排放量
     */
    @RateLimit(ipLimit = 30, apiLimit = 100, timeWindow = 1)
    @GetMapping("/emissionCategory")
    public Object carbonReduction(@RequestParam(required = false) Integer startYear, @RequestParam(required = false) Integer startMonth,
                                   @RequestParam(required = false) Integer endYear, @RequestParam(required = false) Integer endMonth) {
        boolean hasTimeParams = startYear != null && startMonth != null && endYear != null && endMonth != null;
        if (hasTimeParams) {
            if (startYear > endYear || (startYear.equals(endYear) && startMonth > endMonth)) {
                return ResultUtils.error(ErrorCode.NULL_ERROR, "请输入正确的年份");
            }
            if (startMonth <= 0 || startMonth > 12 || endMonth > 12 || endMonth <= 0) {
                return ResultUtils.error(ErrorCode.NULL_ERROR, "请输入正确的月份");
            }
            return carbonEmissionService.getEmissionCategoryData(startYear, startMonth, endYear, endMonth);
        }

        int actualYear = CarbonUtils.getInstance().getActualYear();
        return carbonEmissionService.getEmissionCategoryData(actualYear, 1, actualYear, 12);
    }

    /**
     * 通过该接口将得到指定年份不同物品种类对应的碳排放消耗量
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/listSpeciesConsumptionCount")
    public Object listSpeciesConsumptionCount(@RequestParam(required = false) Integer year,@RequestParam(required = false) Integer month){
        if (null != month && (month <= 0 || month > 12))
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请输入正确的月份");

        int actualYear = year == null ? CarbonUtils.getInstance().getActualYear() : year;
        return carbonEmissionService.getSpeciesConsumptionData(actualYear, month);
    }

    /**
     * 通过该接口将得到指定年份不同物品种类对应的碳排放
     * @return
     */
    @RateLimit(ipLimit = 30, apiLimit = 100, timeWindow = 1)
    @GetMapping("/listSpeciesCarbon")
    public Object listSpeciesCarbon(@RequestParam(required = false) Integer year,@RequestParam(required = false) Integer month){
        if (null != month && (month <= 0 || month > 12))
            return ResultUtils.error(ErrorCode.NULL_ERROR, "请输入正确的月份");

        int actualYear = year == null ? CarbonUtils.getInstance().getActualYear() : year;
        return carbonEmissionService.getSpeciesCarbonData(actualYear, month);
    }

    /**
     * 通过该接口将得到每年的不同楼宇对应的碳排放（折线图）
     * @param year 年份（可为空，为空时使用实际年份）
     * @param area 是否计算地均排放（true：地均排放，false：总排放）
     * @param buildings 楼宇名称（可为空，多个楼宇用逗号分隔或作为数组传递）
     * @return 楼宇碳排放数据列表
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/listBuildingCarbonLine")
    public Object listBuildingCarbon(Integer year, boolean area, String buildings) {
        int actualYear = year == null ? CarbonUtils.getInstance().getActualYear() : year;
        List<String> buildingList = parseBuildings(buildings);
        return carbonEmissionService.getBuildingCarbonLineData(actualYear, area, buildingList);
    }

    /**
     * 通过该接口将得到每年的每月不同楼宇对应的碳排放（柱状图）
     * @param year 年份（可为空，为空时使用实际年份）
     * @param month 月份（可为空，为空时使用当前月份）
     * @param area 是否计算地均排放（true：地均排放，false：总排放）
     * @param buildings 楼宇名称（可为空，多个楼宇用逗号分隔或作为数组传递）
     * @return 楼宇碳排放数据列表
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/listBuildingCarbonBar")
    public Object listBuildingCarbon1(Integer year, Integer month, boolean area, String buildings){
        // 年月要么都为空，要么都不为空
        if ((year == null && month != null) || (year != null && month == null)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "年份或者月份未选择！");
        }

        int actualYear = year != null ? year : CarbonUtils.getInstance().getActualYear();
        int actualMonth = month != null ? month : LocalDate.now().getMonthValue();
        List<String> buildingList = parseBuildings(buildings);
        return carbonEmissionService.getBuildingCarbonBarData(actualYear, actualMonth, area, buildingList);
    }

    /**
     * 通过该接口将得到每年的不同楼宇的电耗，建筑面积、碳排放量、单位面积排放量
     * @param year 年份（可为空，为空时使用实际年份）
     * @param month 月份（可为空，为空时使用当前月份）
     * @param buildings 楼宇名称（可为空，多个楼宇用逗号分隔或作为数组传递）
     * @return 楼宇信息数据列表
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/listBuildingInfo")
    public Object listBuildingInfo(Integer year, Integer month, String buildings){
        // 年月要么都为空，要么都不为空
        if ((year == null && month != null) || (year != null && month == null)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "年份或者月份未选择！");
        }

        int actualYear = year != null ? year : CarbonUtils.getInstance().getActualYear();
        int actualMonth = month != null ? month : LocalDate.now().getMonthValue();
        List<String> buildingList = parseBuildings(buildings);
        return carbonEmissionService.getBuildingInfoData(actualYear, actualMonth, buildingList);
    }

    /**
     * 条件分页查询碳排放记录
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getAllCarbonEmission")
    @PreAuthorize("hasAuthority('CARBON_RECORD_QUERY')")
    public IPage<CarbonEmission> getAllCarbonEmission(int current, int size, String name, String year, String month){
        return carbonEmissionService.pageByObjectName(current, size, name, year, month);
    }

    /**
     * 添加碳排放数据(物品消耗量 字段 与 能耗表 能耗量字段 重叠，可删除 有没有皆可)
     * @param carbonEmission 碳排放数据
     * @param results 验证结果
     * @return 操作结果
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/addCarbonEmission")
    @PreAuthorize("hasAuthority('CARBON_RECORD_ADD')")
    public Object addCarbonEmission(@RequestBody @Valid CarbonEmission carbonEmission, BindingResult results){
        if(results.hasErrors()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, Objects.requireNonNull(results.getFieldError()).getDefaultMessage());
        }

        return carbonEmissionService.addCarbonEmission(carbonEmission);
    }

    /**
     * 通过id修改碳排放数据 (物品消耗量 字段 与 能耗表 能耗量字段 重叠，可删除 有没有皆可)
     * @param carbonEmission 碳排放数据
     * @param results 验证结果
     * @return 操作结果
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/updateCarbonEmission")
    @PreAuthorize("hasAuthority('CARBON_RECORD_UPDATE')")
    public Object updateCarbonEmission(@RequestBody @Valid CarbonEmission carbonEmission, BindingResult results){
        if(results.hasErrors()) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, Objects.requireNonNull(results.getFieldError()).getDefaultMessage());
        }

        return carbonEmissionService.updateCarbonEmission(carbonEmission);
    }

    /**
     * 通过id删除碳排放数据
     * @param carbonEmissionId 碳排放数据ID
     * @return 操作结果
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/deleteCarbonEmissionById")
    @PreAuthorize("hasAuthority('CARBON_RECORD_DELETE')")
    public Object deleteCarbonEmissionById(String carbonEmissionId){
        return carbonEmissionService.deleteCarbonEmissionById(carbonEmissionId);
    }

    /**
     * 批量导入碳排放数据
     * 支持多种文件格式导入（Excel、CSV等）
     * 通过文件导入实现批量导入
     * 传入的文件第一行必须为表头("名称", "分类", "消耗量", "用途", "排放类型", "地点", "年份", "月份")
     * @param file 支持.xlsx或.csv格式的文件
     * @return 操作结果
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/excelCarbonEmission")
    @PreAuthorize("hasAuthority('CARBON_RECORD_ADD')")
    public Object excelCarbonEmission(MultipartFile file) {
        // 文件验证
        if (file == null || file.isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件为空，请选择文件！");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件名不能为空！");
        }

        try {
            return carbonEmissionService.batchImportFromFile(file.getInputStream(), filename);
        } catch (Exception e) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "文件读取失败：" + e.getMessage());
        }
    }

    /**
     * 导出报告（新版本，支持Word和PDF）
     * 使用策略模式，支持多种格式导出
     * 支持Redis缓存，相同年份和格式的导出结果会被缓存
     * @param year 年份（可为空，为空时自动回退查找有数据的年份）
     * @param format 导出格式（docx/pdf），默认为docx
     * @param response HTTP响应对象
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/exportReport")
    @PreAuthorize("hasAuthority('REPORT_GENERATE')")
    public void exportReport(@RequestParam(required = false) Integer year, @RequestParam(defaultValue = "docx") String format,
                             HttpServletResponse response) {
        int actualYear = year != null ? year : CarbonUtils.getInstance().getActualYear();
        
        // 生成缓存键
        String cacheKey = FileCacheUtils.generateCacheKey("export", actualYear, format);
        
        try {
            FileCacheUtils.FileCacheMeta cachedMeta = fileCacheUtils.getCacheFile(cacheKey);
            if (cachedMeta != null) {
                // 从缓存读取文件
                byte[] fileData = fileCacheUtils.readCacheFile(cachedMeta);
                if (fileData != null) {
                    // 设置响应头
                    response.setHeader("Content-Disposition", "attachment; filename=" + cachedMeta.getFileName());
                    response.setContentType(cachedMeta.getContentType());
                    response.setContentLengthLong(cachedMeta.getFileSize());
                    
                    // 写入响应
                    response.getOutputStream().write(fileData);
                    response.getOutputStream().flush();
                    return;
                } else {
                    log.warn("缓存文件读取失败，删除缓存键: {}", cacheKey);
                    fileCacheUtils.deleteCacheFile(cacheKey);
                }
            }
            
            // 获取导出策略
            ExportStrategy strategy = exportStrategyFactory.getStrategy(format);
            
            // 使用可缓存的响应包装类
            CachedHttpServletResponse cachedResponse = new CachedHttpServletResponse(response);
            
            // 执行导出（数据会同时写入response和cachedResponse的缓存）
            strategy.export(actualYear, cachedResponse);
            
            // 3. 缓存生成的文件
            byte[] fileData = cachedResponse.getCachedData();
            if (fileData != null && fileData.length > 0) {
                // 生成文件名
                String fileName = "北京林业大学" + actualYear + "年度碳排放报告." + format;
                String contentType = strategy.getContentType();
                
                // 保存到缓存（缓存24小时）
                fileCacheUtils.saveCacheFile(cacheKey, fileName, contentType, fileData, 86400);
                log.info("导出文件已缓存，年份: {}, 格式: {}, 大小: {} bytes", 
                    actualYear, format, fileData.length);
            }
            
        } catch (Exception e) {
            log.error("导出报告失败", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("导出报告失败：" + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }

    /**
     * 通过年份获取年度报告数据
     * @param year 年份（可为空，为空时使用实际年份）
     * @return 年度报告数据
     */
    @RedisCache(prefix = "export", key = "'reportData:' + (#year != null ? #year : 'current')", expire = 3600)
    @RateLimit(ipLimit = 30, apiLimit = 100, timeWindow = 1)
    @GetMapping("/exportReportDataByYear")
    public Object exportReportDataByYear(@RequestParam(required = false) Integer year) {
        try {
            int actualYear = year != null ? year : CarbonUtils.getInstance().getActualYear();
            Map<String, String> reportData = carbonEmissionService.getExportReportDataByYear(actualYear);
            return ResultUtils.success(reportData);
        } catch (RuntimeException e) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("获取年度报告数据失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取年度报告数据失败");
        }
    }

    /**
     * 异步导出报告（创建导出任务）
     * 立即返回任务ID，用户可以通过任务ID查询导出状态和下载文件
     * 
     * @param year 年份（可为空，为空时自动回退查找有数据的年份）
     * @param format 导出格式（docx/pdf），默认为docx
     * @return 任务ID
     */
    @RateLimit(ipLimit = 10, apiLimit = 100)
    @PostMapping("/exportReportAsync")
    @PreAuthorize("hasAuthority('REPORT_GENERATE')")
    public Object exportReportAsync(@RequestParam(required = false) Integer year, @RequestParam(defaultValue = "docx") String format) {
        try {
            int actualYear = year != null ? year : CarbonUtils.getInstance().getActualYear();
            
            // 获取当前用户ID
            Long userId = null;
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                    userId = userDetails.getUserId();
                }
            } catch (Exception e) {
                log.warn("获取当前用户ID失败", e);
            }
            
            // 创建导出任务
            String taskId = asyncExportService.createExportTask(userId, actualYear, format);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("taskId", taskId);
            result.put("year", actualYear);
            result.put("format", format);
            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("创建导出任务失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "创建导出任务失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询导出任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    @RateLimit(ipLimit = 30, apiLimit = 200)
    @GetMapping("/exportTaskStatus")
    @PreAuthorize("hasAuthority('REPORT_GENERATE')")
    public Object getExportTaskStatus(@RequestParam String taskId) {
        try {
            ExportTask task = asyncExportService.getTask(taskId);
            if (task == null) {
                return ResultUtils.error(ErrorCode.NULL_ERROR, "任务不存在或已过期");
            }
            
            // 构建返回数据
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("taskId", task.getTaskId());
            result.put("status", task.getStatus().name());
            result.put("year", task.getYear());
            result.put("format", task.getFormat());
            result.put("createTime", task.getCreateTime());
            result.put("startTime", task.getStartTime());
            result.put("completeTime", task.getCompleteTime());
            result.put("errorMessage", task.getErrorMessage());
            result.put("fileName", task.getFileName());
            result.put("fileSize", task.getFileSize());
            
            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("查询导出任务状态失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "查询导出任务状态失败");
        }
    }
    
    /**
     * 下载导出任务生成的文件
     * 
     * @param taskId 任务ID
     * @param response HTTP响应对象
     */
    @RateLimit(ipLimit = 30, apiLimit = 200)
    @GetMapping("/exportTaskDownload")
    @PreAuthorize("hasAuthority('REPORT_GENERATE')")
    public void downloadExportTask(@RequestParam String taskId, HttpServletResponse response) {
        try {
            ExportTask task = asyncExportService.getTask(taskId);
            if (task == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("任务不存在或已过期");
                return;
            }
            
            if (task.getStatus() != ExportTask.TaskStatus.COMPLETED) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("任务尚未完成，当前状态：" + task.getStatus().name());
                return;
            }
            
            // 从缓存读取文件
            FileCacheUtils.FileCacheMeta cachedMeta = fileCacheUtils.getCacheFile(task.getFileCacheKey());
            if (cachedMeta == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("文件不存在或已过期");
                return;
            }
            
            byte[] fileData = fileCacheUtils.readCacheFile(cachedMeta);
            if (fileData == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("文件读取失败");
                return;
            }
            
            // 设置响应头
            response.setHeader("Content-Disposition", "attachment; filename=" + task.getFileName());
            response.setContentType(cachedMeta.getContentType());
            response.setContentLengthLong(fileData.length);
            
            // 写入响应
            response.getOutputStream().write(fileData);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            log.error("下载导出文件失败: taskId={}", taskId, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("下载文件失败：" + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }

    /**
     * 解析楼宇名称参数
     * 将逗号分隔的字符串转换为楼宇名称列表，并去除前后空格
     * @param buildings 楼宇名称字符串（可为空，多个楼宇用逗号分隔）
     * @return 楼宇名称列表，如果输入为空则返回null
     */
    private List<String> parseBuildings(String buildings) {
        if (buildings == null || buildings.trim().isEmpty()) {
            return null;
        }
        return Arrays.asList(buildings.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

}

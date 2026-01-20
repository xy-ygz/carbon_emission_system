import request from "../utils/request";

// ============================================
// 碳排放相关API（包括所有图像数据的获取方法）
// ============================================

//获取所有碳排放数据
export function getAllEmission(params) {
  return request({
    url: "/api/carbonEmission/getAllCarbonEmission",
    method: "get",
    params: params
  });
}

//根据名称搜索碳排放数据
export function searchCarbonByName(params) {
  return request({
    url: "/api/carbonEmission/getAllCarbonEmission",
    method: "get",
    params: params
  });
}

//获取碳排放折线图数据
export function getCarbonLine(params) {
  return request({
    url: "/api/carbonEmission/listBuildingCarbonLine",
    method: "get",
    params: params
  });
}

//获取碳排放柱状图数据
export function getCarbonBuilding(params) {
  return request({
    url: "/api/carbonEmission/listBuildingCarbonBar",
    method: "get",
    params: params
  });
}

//获取碳排放表格数据
export function getCarbonTable(params) {
  return request({
    url: "/api/carbonEmission/listBuildingInfo",
    method: "get",
    params: params
  });
}

//均值表格
export function getJunzhi(params) {
  return request({
    url: "/api/carbonEmission/collegeCarbonEmission",
    method: "get",
    params: params
  });
}

//饼图
export function getBingtu(params) {
  return request({
    url: "/api/carbonEmission/listSpeciesCarbon",
    method: "get",
    params: params
  });
}

//获取消耗量统计
export function getConsumptionCount(params) {
  return request({
    url: "/api/carbonEmission/listSpeciesConsumptionCount",
    method: "get",
    params: params
  });
}

//桑葚图
export function getCarbonMulberry(params) {
  return request({
    url: "/api/carbonEmission/mulberryDiagram",
    method: "get",
    params: params
  });
}

//堆积柱状图（物品分类）
export function getDuijizhuCategory(params) {
  return request({
    url: "/api/carbonEmission/emissionCategory",
    method: "get",
    params: params
  });
}

//堆积柱状图（排放类型）
export function getDuijizhuType(params) {
  return request({
    url: "/api/carbonEmission/emissionType",
    method: "get",
    params: params
  });
}

//堆积折线图（物品分类）
export function getDuijizheCategory(params) {
  return request({
    url: "/api/carbonEmission/emissionCategory",
    method: "get",
    params: params
  });
}

//堆积折线图（排放类型）
export function getDuijizheType(params) {
  return request({
    url: "/api/carbonEmission/emissionCategory",
    method: "get",
    params: params
  });
}

//柱状图
export function getEmissionZhu(params) {
  return request({
    url: "/api/carbonEmission/emissionCategory",
    method: "get",
    params: params
  });
}

//导出碳排放报告（同步方式）
export function exportCarbonReport(params) {
  return request({
    url: "/api/carbonEmission/exportReport",
    method: "get",
    params: params,
    responseType: 'blob' // 设置响应类型为blob，用于文件下载
  });
}

//创建异步导出任务
export function createExportTask(params) {
  return request({
    url: "/api/carbonEmission/exportReportAsync",
    method: "post",
    params: params
  });
}

//查询导出任务状态
export function getExportTaskStatus(taskId) {
  return request({
    url: "/api/carbonEmission/exportTaskStatus",
    method: "get",
    params: { taskId: taskId }
  });
}

//下载导出任务生成的文件
export function downloadExportTask(taskId) {
  return request({
    url: "/api/carbonEmission/exportTaskDownload",
    method: "get",
    params: { taskId: taskId },
    responseType: 'blob' // 设置响应类型为blob，用于文件下载
  });
}

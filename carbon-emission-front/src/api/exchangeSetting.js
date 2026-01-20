import request from "../utils/request";

// ============================================
// 碳排放转换配置相关API
// ============================================

//分页查所有碳排放转换配置
export function getAllExchangeSetting(params) {
  return request({
    url: "/api/exchangeSetting/getAllExchangeSetting",
    method: "get",
    params: params
  });
}

//根据id查碳排放转换配置
export function getExchangeSetting(params) {
  return request({
    url: "/api/exchangeSetting/getExchangeSetting",
    method: "get",
    params: params
  });
}

//新增碳排放转换配置
export function addExchangeSetting(params) {
  return request({
    url: "/api/exchangeSetting/addExchangeSetting",
    method: "post",
    data: params
  });
}

//修改碳排放转换配置
export function updateExchangeSetting(params) {
  return request({
    url: "/api/exchangeSetting/updateExchangeSetting",
    method: "post",
    data: params
  });
}

//根据id删除碳排放转换配置
export function deleteExchangeSetting(params) {
  return request({
    url: "/api/exchangeSetting/deleteExchangeSetting",
    method: "post",
    data: params
  });
}

//查所有碳排放转换配置
export function getAllCategory(params) {
  return request({
    url: "/api/exchangeSetting/getAll",
    method: "get",
    params: params
  });
}

import request from "../utils/request";

// ============================================
// 地点信息相关API
// ============================================

//获取地点信息
export function getPlace(params) {
  return request({
    url: "/api/placeInfo/getPlace",
    method: "get",
    params: params
  });
}

//分页查询所有地点信息
export function getAllPlaceInfo(params) {
  return request({
    url: "/api/placeInfo/getAllPlaceInfo",
    method: "get",
    params: params
  });
}

//新增地点信息
export function addPlaceInfo(params) {
  return request({
    url: "/api/placeInfo/addPlaceInfo",
    method: "post",
    data: params
  });
}

//修改地点信息
export function updatePlaceInfo(params) {
  return request({
    url: "/api/placeInfo/updatePlaceInfo",
    method: "post",
    data: params
  });
}

//根据id删除地点信息
export function deletePlaceInfo(params) {
  return request({
    url: "/api/placeInfo/deletePlaceInfoById",
    method: "get",
    params: params
  });
}

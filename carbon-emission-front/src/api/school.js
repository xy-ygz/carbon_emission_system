import request from "../utils/request";

// ============================================
// 学校管理相关API
// ============================================

//获取学校信息
export function getSchoolInfo() {
  return request({
    url: "/api/school/getSchoolInfo",
    method: "get"
  });
}

//更新学校信息
export function updateSchoolInfo(params) {
  return request({
    url: "/api/school/updateSchoolInfo",
    method: "post",
    data: params
  });
}

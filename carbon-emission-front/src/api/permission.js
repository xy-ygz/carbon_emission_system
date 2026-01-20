import request from "../utils/request";

// ============================================
// 权限管理相关API
// ============================================

//获取权限树
export function getPermissionTree() {
  return request({
    url: "/api/permissionManage/getPermissionTree",
    method: "get"
  });
}

//新增权限
export function addPermission(params) {
  return request({
    url: "/api/permissionManage/addPermission",
    method: "post",
    data: params
  });
}

//更新权限
export function updatePermission(params) {
  return request({
    url: "/api/permissionManage/updatePermission",
    method: "post",
    data: params
  });
}

//删除权限
export function deletePermission(params) {
  return request({
    url: "/api/permissionManage/deletePermission",
    method: "post",
    data: params
  });
}


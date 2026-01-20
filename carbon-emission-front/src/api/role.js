import request from "../utils/request";

// ============================================
// 角色管理相关API
// ============================================

//分页查询角色列表
export function getRoleList(params) {
  return request({
    url: "/api/roleManage/getRoleList",
    method: "get",
    params: params
  });
}

//获取角色详情（包含权限）
export function getRoleWithPermissions(params) {
  return request({
    url: "/api/roleManage/getRoleWithPermissions",
    method: "get",
    params: params
  });
}

//新增角色
export function addRole(params) {
  return request({
    url: "/api/roleManage/addRole",
    method: "post",
    data: params
  });
}

//更新角色
export function updateRole(params) {
  return request({
    url: "/api/roleManage/updateRole",
    method: "post",
    data: params
  });
}

//删除角色
export function deleteRole(params) {
  return request({
    url: "/api/roleManage/deleteRole",
    method: "post",
    data: params
  });
}

//分配权限
export function assignPermissions(params) {
  return request({
    url: "/api/roleManage/assignPermissions",
    method: "post",
    data: params
  });
}

//获取权限树（用于角色分配权限）
export function getPermissionTree() {
  return request({
    url: "/api/roleManage/getPermissionTree",
    method: "get"
  });
}


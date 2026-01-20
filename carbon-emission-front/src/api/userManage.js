import request from "../utils/request";

// ============================================
// 用户管理相关API
// ============================================

//分页查询用户列表
export function getUserList(params) {
  return request({
    url: "/api/userManage/getUserList",
    method: "get",
    params: params
  });
}

//获取用户详情（包含角色）
export function getUserWithRoles(params) {
  return request({
    url: "/api/userManage/getUserWithRoles",
    method: "get",
    params: params
  });
}

//新增用户
export function addUser(params) {
  return request({
    url: "/api/userManage/addUser",
    method: "post",
    data: params
  });
}

//更新用户
export function updateUser(params) {
  return request({
    url: "/api/userManage/updateUser",
    method: "post",
    data: params
  });
}

//删除用户
export function deleteUser(params) {
  return request({
    url: "/api/userManage/deleteUser",
    method: "post",
    data: params
  });
}

//重置密码
export function resetPassword(params) {
  return request({
    url: "/api/userManage/resetPassword",
    method: "post",
    data: params
  });
}

//分配角色
export function assignRoles(params) {
  return request({
    url: "/api/userManage/assignRoles",
    method: "post",
    data: params
  });
}

//获取所有角色列表（用于下拉选择）
export function getAllRoles() {
  return request({
    url: "/api/userManage/getAllRoles",
    method: "get"
  });
}

// 获取当前登录用户的角色信息（用于权限等级判断）
export function getCurrentUserRoles() {
  return request({
    url: "/api/userManage/getCurrentUserRoles",
    method: "get"
  });
}

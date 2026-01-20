import request from "../utils/request";

// ============================================
// 用户认证相关API
// ============================================

//登录
export function login(params) {
  return request({
    url: "/api/user/login",
    method: "post",
    data: params
  });
}

//刷新Token
export function refreshTokenApi(params) {
  return request({
    url: "/api/user/refreshToken",
    method: "post",
    data: params
  });
}

//登出
export function logoutApi() {
  return request({
    url: "/api/user/logout",
    method: "post"
  });
}


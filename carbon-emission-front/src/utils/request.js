import axios from 'axios';
import { Message } from 'element-ui';
import router from '../router';
import { publicNetworkIpAndPort } from '../api/globalVar';
import tokenManager from './tokenManager';

// 创建axios实例
const service = axios.create({
  baseURL: publicNetworkIpAndPort(),
  timeout: 30000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
});

// 防抖：避免短时间内重复显示相同的错误消息
let lastErrorTime = {};
const ERROR_DEBOUNCE_TIME = 2000; // 2秒内不重复显示相同错误

function showErrorOnce(message, errorCode) {
  const key = errorCode || message;
  const now = Date.now();
  if (!lastErrorTime[key] || now - lastErrorTime[key] > ERROR_DEBOUNCE_TIME) {
    lastErrorTime[key] = now;
    return true;
  }
  return false;
}

// 导出 axios 实例，供 el-upload 等组件使用
export const axiosInstance = service;

// 用于存储正在刷新token时的pending请求
let isRefreshing = false;
let pendingRequests = [];

// 请求拦截器 - 自动添加JWT Token
service.interceptors.request.use(
  async config => {
    const token = tokenManager.getAccessToken();
    
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token;
      
      if (tokenManager.shouldRefreshToken() && !config.url.includes('/refreshToken')) {
        try {
          const newToken = await tokenManager.refreshToken();
          config.headers['Authorization'] = 'Bearer ' + newToken;
        } catch (error) {
          console.error('Token refresh failed:', error);
          return Promise.reject(error);
        }
      }
    }
    
    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器 - 统一处理错误
service.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response;
    }
    
    const res = response.data;
    
    if (typeof res !== 'object' || res === null || Array.isArray(res) || res.code === undefined) {
      return response;
    }
    
    if (res.code !== 200) {
      // 登录接口的错误不应该走token过期处理逻辑
      const isLoginRequest = response.config.url && response.config.url.includes('/login');
      
      if (res.code === 40100 && !isLoginRequest) {
        // 非登录接口的40100错误，走token过期处理
        // 优先显示description，如果没有则显示message
        handleTokenExpired(res.description || res.message || '未登录或Token已过期，请重新登录');
      } else if (res.code === 40100 && isLoginRequest) {
        // 登录接口的40100错误，直接显示错误信息
        const errorMsg = res.description || res.message || '登录失败';
        Message.error(errorMsg);
      } else if (res.code === 40101) {
        const message = res.description || res.message || '不允许访问';
        if (showErrorOnce(message, 40101)) {
          Message.error(message);
        }
      } else {
        // 优先显示description，如果没有则显示message
        const errorMsg = res.description || res.message;
        if (errorMsg) {
          Message.error(errorMsg);
        }
      }
      const rejectMsg = res.description || res.message || '请求失败';
      return Promise.reject(new Error(rejectMsg));
    } else {
      return response;
    }
  },
  async error => {
    console.error('响应错误:', error);
    
    if (error.response) {
      const res = error.response.data;
      
      // 登录接口的错误不应该走token过期处理逻辑
      const isLoginRequest = error.config && error.config.url && error.config.url.includes('/login');
      
      if (error.response.status === 401 && !isLoginRequest) {
        await handleTokenExpired('未登录或Token已过期，请重新登录');
      } else if (res && typeof res === 'object' && res.code === 40100 && !isLoginRequest) {
        // 非登录接口的40100错误，走token过期处理
        // 优先显示description，如果没有则显示message
        await handleTokenExpired(res.description || res.message || '未登录或Token已过期，请重新登录');
      } else if (res && typeof res === 'object' && res.code === 40100 && isLoginRequest) {
        // 登录接口的40100错误，直接显示错误信息
        const errorMsg = res.description || res.message || '登录失败';
        Message.error(errorMsg);
      } else if (res && typeof res === 'object' && res.code === 40101) {
        const message = res.description || res.message || '不允许访问';
        if (showErrorOnce(message, 40101)) {
          Message.error(message);
        }
      } else if (res && typeof res === 'object') {
        // 优先显示description，如果没有则显示message
        const errorMsg = res.description || res.message;
        if (errorMsg) {
          Message.error(errorMsg);
        }
      } else {
        Message.error(`请求失败: ${error.response.status} ${error.response.statusText || ''}`);
      }
    } else if (error.request) {
      Message.error('网络错误，请检查网络连接');
    } else {
      Message.error('请求失败，请稍后重试');
    }
    
    return Promise.reject(error);
  }
);

async function handleTokenExpired(message) {
  if (isRefreshing) {
    return;
  }
  
  isRefreshing = true;
  
  try {
    const newToken = await tokenManager.refreshToken();
    isRefreshing = false;
  } catch (error) {
    isRefreshing = false;
    tokenManager.clearTokens();
    
    if (message.includes('请先完成登录') || message.includes('请先登录')) {
      Message.warning('请先完成登录');
    } else {
      Message.error(message);
    }
    
    setTimeout(() => {
      if (router.currentRoute.path !== '/Tan/TanLogin') {
        router.push('/Tan/TanLogin').catch(err => {
          if (window.location.pathname !== '/Tan/TanLogin') {
            window.location.href = '/Tan/TanLogin';
          }
        });
      }
    }, 500);
  }
}

export default service;


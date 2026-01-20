import { refreshTokenApi } from '../api/user';

const ACCESS_TOKEN_KEY = 'token';
const REFRESH_TOKEN_KEY = 'refreshToken';
const TOKEN_EXPIRE_TIME_KEY = 'tokenExpireTime';

class TokenManager {
  constructor() {
    this.refreshTimer = null;
    this.isRefreshing = false;
    this.refreshPromise = null;
  }

  getAccessToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken() {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  getTokenExpireTime() {
    const expireTime = localStorage.getItem(TOKEN_EXPIRE_TIME_KEY);
    return expireTime ? parseInt(expireTime) : null;
  }

  setTokens(accessToken, refreshToken, expiresIn) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    const expireTime = Date.now() + expiresIn * 1000;
    localStorage.setItem(TOKEN_EXPIRE_TIME_KEY, expireTime.toString());
    this.startAutoRefresh();
  }

  clearTokens() {
    this.stopAutoRefresh();
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(TOKEN_EXPIRE_TIME_KEY);
  }

  isTokenExpired() {
    const expireTime = this.getTokenExpireTime();
    if (!expireTime) return true;
    return Date.now() >= expireTime;
  }

  shouldRefreshToken() {
    const expireTime = this.getTokenExpireTime();
    if (!expireTime) return false;
    const remainingTime = expireTime - Date.now();
    return remainingTime <= 5 * 60 * 1000;
  }

  async refreshToken() {
    if (this.isRefreshing) {
      return this.refreshPromise;
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    this.isRefreshing = true;
    this.refreshPromise = refreshTokenApi({ refreshToken })
      .then(response => {
        if (response.data.code === 200) {
          const { token, refreshToken: newRefreshToken, expiresIn } = response.data.data;
          this.setTokens(token, newRefreshToken, expiresIn);
          return token;
        } else {
          throw new Error(response.data.message || 'Refresh token failed');
        }
      })
      .catch(error => {
        this.clearTokens();
        throw error;
      })
      .finally(() => {
        this.isRefreshing = false;
        this.refreshPromise = null;
      });

    return this.refreshPromise;
  }

  startAutoRefresh() {
    this.stopAutoRefresh();
    
    const expireTime = this.getTokenExpireTime();
    if (!expireTime) return;

    const remainingTime = expireTime - Date.now();
    const refreshTime = Math.max(remainingTime - 5 * 60 * 1000, 1000);

    this.refreshTimer = setTimeout(() => {
      this.refreshToken().catch(error => {
        console.error('Auto refresh token failed:', error);
      });
    }, refreshTime);
  }

  stopAutoRefresh() {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  setupMultiTabSync() {
    window.addEventListener('storage', (event) => {
      if (event.key === ACCESS_TOKEN_KEY && event.newValue === null) {
        this.clearTokens();
      } else if (event.key === TOKEN_EXPIRE_TIME_KEY && event.newValue) {
        this.startAutoRefresh();
      }
    });

    document.addEventListener('visibilitychange', async () => {
      if (!document.hidden) {
        await this.handlePageResume();
      } else {
        this.stopAutoRefresh();
      }
    });
  }

  async handlePageResume() {
    const token = this.getAccessToken();
    if (!token) {
      return;
    }

    if (this.isTokenExpired()) {
      try {
        await this.refreshToken();
      } catch (error) {
        console.error('Page resume token refresh failed:', error);
      }
    } else {
      this.startAutoRefresh();
    }
  }
}

export default new TokenManager();
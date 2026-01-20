<template>
  <div class="header-admin-fixed">
    <div class="header">
      <div class="carboncarbon">
        <div class="logo-container">
          <img :src="logoUrl" alt="北京林业大学" class="system-logo" @error="handleImageError">
        </div>
        <div class="title-container">
          <div class="title-main">北京林业大学</div>
          <div class="title-sub">碳排放核算与管理系统</div>
        </div>
      </div>
      <div class="carbonrightright">
        <el-dropdown @command="handleCommand" menu-align='start' class="float-right">
          <span class="user-img-icon">
            <i class="el-icon-user-solid" style="font-size: 30px; display: block;"></i>
            <!-- <img src="../assets/user.jpg" class="avator-img"> -->
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="signout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script>
import { logoutApi } from '../api/user'
import tokenManager from '../utils/tokenManager'

export default {
  name: 'AdminHeader',
  data() {
    return {
      logoUrl: require('../assets/logo.png')
    }
  },
  methods: {
    handleImageError(e) {
      console.error('Logo图片加载失败:', e);
      if (e.target && e.target.parentElement) {
        e.target.parentElement.style.display = 'none';
      }
    },
    async handleCommand(command) {
      if (command == 'signout') {
        try {
          await logoutApi()
        } catch (error) {
          console.error('Logout API error:', error)
        }
        tokenManager.clearTokens()
        localStorage.removeItem("userId")
        localStorage.removeItem("auth")
        this.$message.success('退出登录成功')
        this.$router.push({ path: '/Login' })
      } else if (command == 'backtoindex') {
        this.$router.push({ path: '/Tan/ManageCarbon' })
      }
    }
  }
}
</script>

<style scoped>
.carboncarbon,
.carbonrightright {
  display: inline-block;
}

.carbonrightright {
  float: right;
  vertical-align: top;
}

.carboncarbon {
  margin-left: 40px !important;
  display: flex !important;
  align-items: center !important;
  height: 80px !important;
  width: calc(33.33% - 40px) !important;
  min-width: 450px !important;
  max-width: 600px !important;
  gap: 18px !important;
  padding-right: 20px !important;
  float: none !important;
  vertical-align: middle !important;
}

/* Logo 容器 - 带背景和边框效果 */
.logo-container {
  flex-shrink: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 64px !important;
  height: 64px !important;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.1) 100%) !important;
  border: 2px solid rgba(255, 255, 255, 0.3) !important;
  border-radius: 14px !important;
  padding: 10px !important;
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.2) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  backdrop-filter: blur(4px) !important;
}

.logo-container:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0.2) 100%);
  border-color: rgba(255, 255, 255, 0.5);
  transform: translateY(-2px) scale(1.02);
  box-shadow: 
    0 6px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.system-logo {
  width: 100% !important;
  height: 100% !important;
  object-fit: contain !important;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.25)) !important;
  transition: filter 0.3s ease !important;
  display: block !important;
}

.logo-container:hover .system-logo {
  filter: drop-shadow(0 3px 8px rgba(0, 0, 0, 0.3));
}

/* 标题容器 */
.title-container {
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  gap: 6px !important;
  min-width: 0 !important;
  position: relative !important;
}

.title-main {
  font-size: 22px !important;
  font-weight: 600 !important;
  color: #FFFFFF !important;
  line-height: 1.3 !important;
  letter-spacing: 0.5px !important;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2) !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  font-family: "Microsoft YaHei", "PingFang SC", "Helvetica Neue", Arial, sans-serif !important;
  margin: 0 !important;
  padding: 0 !important;
  display: block !important;
}

.title-sub {
  font-size: 14px !important;
  font-weight: 400 !important;
  color: rgba(255, 255, 255, 0.9) !important;
  line-height: 1.4 !important;
  letter-spacing: 0.3px !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15) !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  font-family: "Microsoft YaHei", "PingFang SC", "Helvetica Neue", Arial, sans-serif !important;
  margin: 0 !important;
  padding: 0 !important;
  display: block !important;
}

.user-img-icon {
  color: steelblue;
  width: 30px;
  height: 30px;
  opacity: 0.84;
  cursor: pointer;
  font-size: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #FFFFFF;
  border-radius: 24px;
  padding: 4px;
}

.system-name-title {
  font-style: oblique;
  height: 24px;
  font-size: 24px;
  font-family: MicrosoftYaHei-Bold, MicrosoftYaHei;
  font-weight: bold;
  color: #FFFFFF;
  line-height: 24px;
  display: inline-block;
  margin-left: 100px;
  margin-top: 18px;
}

.header-admin-fixed {
  position: fixed;
  text-align: left;
  width: 100%;
  height: 80px;
  z-index: 100;
  top: 0;
  background: linear-gradient(135deg, #4a7c3a 0%, #2d5016 100%);
  min-width: 1000px;
  box-shadow: 0 2px 12px rgba(45, 80, 22, 0.15);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 40px;
  box-sizing: border-box;
}
</style>

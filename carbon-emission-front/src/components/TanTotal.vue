<template>
  <div class="total">
    <div class="header">
      <div class="header-content">
        <div class="carbonleft">
          <div class="logo-container">
            <img :src="logoUrl" alt="北京林业大学" class="system-logo" @error="handleImageError">
          </div>
          <div class="title-container">
            <div class="title-main">北京林业大学</div>
            <div class="title-sub">BJFU</div>
          </div>
          <div class="system-title">碳排放核算与管理系统</div>
        </div>
        <div class="right">
          
        <el-menu router class="tan-el-menu" mode="horizontal">
          <el-menu-item index="/Tan/TanPage" class="header-icon el-icon-s-home" style="font-size: 20px;">
            首页</el-menu-item>
          <el-submenu index="1">
            <template slot="title">
              <i class="header-icon el-icon-s-data"></i>
              <span>数据输入</span>
            </template>
            <el-menu-item-group>
              <el-menu-item index="/Tan/ManageSchool" class="el-icon-school"
                style="font-size: 16px;">&nbsp;&nbsp;学校信息</el-menu-item>
              <el-menu-item index="/Tan/ManagePlace" class="el-icon-location"
                style="font-size: 16px;">&nbsp;&nbsp;排放地点</el-menu-item>
              <el-menu-item index="/Tan/ExchangeSetting" class="el-icon-location"
                style="font-size: 16px;">&nbsp;&nbsp;碳排放转化系数</el-menu-item>
              <el-menu-item @click="jumpCarbon()" class="el-icon-location"
                style="font-size: 16px;">&nbsp;&nbsp;碳排放记录</el-menu-item>
            </el-menu-item-group>
          </el-submenu>
          <el-submenu index="2">
            <template slot="title">
              <i class="header-icon el-icon-s-flag"></i>
              <span>能耗监测</span>
            </template>
            <el-menu-item-group>
              <el-menu-item index="/Tan/TanMonitor" class="el-icon-location"
                style="font-size: 16px;">&nbsp;&nbsp;能耗碳排放监测</el-menu-item>
              <el-menu-item index="/Tan/TanAudit" class="el-icon-location"
                style="font-size: 16px;">&nbsp;&nbsp;能耗碳排放审计</el-menu-item>
              <el-menu-item index="/Tan/TanContrast" class="el-icon-location"
                style="font-size: 16px;">&nbsp;&nbsp;能耗碳排放对比</el-menu-item>
            </el-menu-item-group>
          </el-submenu>
          <el-submenu index="3">
            <template slot="title">
              <i class="header-icon el-icon-s-data"></i>
              <span>碳排放计算与减碳分析</span>
            </template>
            <el-menu-item-group>
              <el-menu-item index="/Tan/TanResult" class="el-icon-location"
                style="font-size: 16px;">&nbsp;&nbsp;流动趋势</el-menu-item>
              <el-menu-item index="/Tan/TanAnalyse" class="el-icon-location"
                style="font-size: 16px;">&nbsp;&nbsp;减碳分析</el-menu-item>
            </el-menu-item-group>
          </el-submenu>

          <el-menu-item index="/Tan/TanExport" class="header-icon el-icon-s-order"
            style="font-size: 20px;">报告生成</el-menu-item>

          <el-submenu index="4">
            <template slot="title">
              <i class="header-icon el-icon-setting"></i>
              <span style="font-size: 20px;">系统管理</span>
            </template>
            <el-menu-item-group>
              <el-menu-item index="/Tan/ManageUser" class="el-icon-user"
                style="font-size: 16px;">&nbsp;&nbsp;用户管理</el-menu-item>
              <el-menu-item index="/Tan/ManageRole" class="el-icon-s-custom"
                style="font-size: 16px;">&nbsp;&nbsp;角色管理</el-menu-item>
              <el-menu-item index="/Tan/ManagePermission" class="el-icon-key"
                style="font-size: 16px;">&nbsp;&nbsp;权限管理</el-menu-item>
            </el-menu-item-group>
          </el-submenu>

          <div class="avatar">
            <el-dropdown @command="handleCommand" placement="bottom" trigger="hover" popper-class="user-dropdown-menu">
              <span class="user-img-icon">
                <i class="el-icon-user-solid"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item v-if="isLogin === 'youke'" command="login">登录</el-dropdown-item>
                <el-dropdown-item v-else command="signout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>

        </el-menu>
        </div>
      </div>
    </div>
    <div class="child" :class="{ 'login-child': $route.path === '/Tan/TanLogin' }">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
import pubsub from 'pubsub-js'
import { logoutApi } from '../api/user'
import tokenManager from '../utils/tokenManager'

export default {
  name: 'TanTotal',
  data() {
    return {
      loginForm: false,
      isLogin: 'youke',
      logoUrl: require('../assets/bjfu-logo.png')
    }
  },
  props: {
  },
  created() {
    this.$bus.$on('updateLoginStatus', (data) => {
      this.isLogin = data
    })
    this.checkLoginStatus()
  },
  watch: {
    '$route'() {
      this.checkLoginStatus()
    }
  },
  methods: {
    checkLoginStatus() {
      const token = tokenManager.getAccessToken()
      const auth = localStorage.getItem('auth')
      if (token && auth) {
        this.isLogin = auth
      } else {
        this.isLogin = 'youke'
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
        this.isLogin = 'youke'
        this.$bus.$emit('updateLoginStatus', 'youke')
        this.$router.push({ path: '/Tan/TanPage' })
      } else if (command == 'login') {
        this.$router.push({ path: '/Tan/TanLogin' })
      }
    },
    jumpCarbon() {
      // console.log("this.$route.path:",this.$route.path)
      this.$router.push({
        path: 'ManageCarbon',
      });
    },
    handleImageError(e) {
      console.error('Logo图片加载失败:', e);
      // 如果logo加载失败，可以隐藏logo容器
      if (e.target && e.target.parentElement) {
        e.target.parentElement.style.display = 'none';
      }
    },
    jump(value) {
      //1.localStorage
      // localStorage.setItem("loginForm",true);

      this.$router.push({
        //2.路由携参
        // path:'Login',
        // query:{
        //   loginForm: true
        // }
        //使用params时 是不能通过path跳转的
        name: 'TanLogin',
        // params:{
        //   loginForm: value
        // }
      })

      //3.触发全局总线方法
      // this.loginForm = value
      // this.$bus.$emit('sendLoginForm',this.loginForm)

      //4.利用消息订阅与发布
      // this.loginForm = value
      // pubsub.publish('sendLoginForm', value)
    }
  },

  beforeDestroy() {
    //3.触发全局总线方法
    // this.jump(this.loginForm)

    //4.利用消息订阅与发布
    // pubsub.publish('sendLoginForm', this.loginForm)
  }
}
</script>

<style scoped>
/* 全局覆盖 Element UI 菜单项激活状态的边框，防止布局抖动 */
.total {
  padding: 0;
  width: 100%;
  height: 100%; /* 使用百分比，由父级 App.vue 控制 */
  overflow: hidden; /* 整体禁止滚动，由子组件控制 */
  display: flex;
  flex-direction: column;
}

.header {
  flex-shrink: 0; /* 头部固定 */
  white-space: nowrap;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #4a7c3a 0%, #2d5016 100%);
  height: 80px;
  width: 100%;
  z-index: 1000;
  padding: 0 40px;
  box-sizing: border-box;
  box-shadow: 0 2px 12px rgba(45, 80, 22, 0.15);
}

.child {
  flex: 1; /* 占据剩余全部高度 */
  min-height: 0; /* 允许 flex 子元素收缩 */
  text-align: center;
  width: 100%;
  position: relative;
  z-index: 1;
  overflow-x: hidden;
  overflow-y: auto; /* 允许纵向滚动 */
  background-color: var(--forest-bg-primary);
  padding-top: 20px; /* 统一的顶部间距 */
}

.login-child {
  padding-top: 0 !important; /* 登录页不需要顶部间距，实现全屏铺满 */
}

/* 森林系风格滚动条 - 统一应用于所有页面容器 */
.child::-webkit-scrollbar {
  width: 8px;
}

.child::-webkit-scrollbar-track {
  background: var(--forest-bg-primary);
  border-radius: 4px;
}

.child::-webkit-scrollbar-thumb {
  background: var(--forest-light);
  border-radius: 4px;
}

.child::-webkit-scrollbar-thumb:hover {
  background: var(--forest-secondary);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 20px;
}

.carbonleft {
  display: inline-block;
  margin-left: 0;
  margin-right: 0;
  flex-shrink: 0;
  display: flex !important;
  align-items: center !important;
  height: 80px !important;
  gap: 20px !important;
  width: auto !important;
  min-width: 0 !important;
  max-width: none !important;
}


.right {
  white-space: nowrap;
  height: 80px;
  flex: 1;
  flex-shrink: 1;
  min-width: 0;
  vertical-align: top;
  text-align: right;
  display: flex;
  justify-content: flex-end; /* 改为靠右对齐 */
  align-items: center;
  overflow: visible;
  margin-left: 0;
  gap: 25px; /* 系统标题和导航菜单之间的间距 */
  /* padding-left: 20px; */
  padding-right: 0; /* 确保右侧没有内边距 */
}

/* Logo 容器 */
.logo-container {
  flex-shrink: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 56px !important;
  height: 56px !important;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.1) 100%) !important;
  border: 2px solid rgba(255, 255, 255, 0.3) !important;
  border-radius: 12px !important;
  padding: 8px !important;
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.2) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  backdrop-filter: blur(4px) !important;
}

.logo-container:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0.2) 100%) !important;
  border-color: rgba(255, 255, 255, 0.5) !important;
  transform: translateY(-2px) scale(1.02) !important;
  box-shadow: 
    0 6px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3) !important;
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
  filter: drop-shadow(0 3px 8px rgba(0, 0, 0, 0.3)) !important;
}

/* 标题容器 */
.title-container {
  flex: 0 0 auto !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  gap: 4px !important;
  min-width: 0 !important;
  position: relative !important;
  white-space: nowrap !important;
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
  font-size: 24px !important;
  font-weight: 700 !important;
  color: #FFFFFF !important;
  line-height: 1.2 !important;
  letter-spacing: 8px !important;
  text-shadow: 
    0 2px 4px rgba(0, 0, 0, 0.3),
    0 4px 8px rgba(0, 0, 0, 0.2),
    0 0 10px rgba(255, 255, 255, 0.1) !important;
  white-space: nowrap !important;
  overflow: visible !important;
  text-overflow: clip !important;
  font-family: "Arial", "Helvetica Neue", sans-serif !important;
  margin: 0 !important;
  padding: 0 !important;
  display: block !important;
  text-transform: uppercase !important;
  animation: textGlow 3s ease-in-out infinite !important;
}

.carbonleft h1 {
  display: none !important;
}

.forest-logo-icon {
  display: none !important;
}
/* --- 优化后的系统标题样式 --- */
.system-title {
  /* 字体设计：增加字间距和高级感 */
  font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif !important;
  font-size: 24px !important; /* 稍微减小，避免挤压 */
  font-weight: 700 !important;
  letter-spacing: 2px !important; /* 减小字间距 */
  line-height: 1.4 !important;
  white-space: nowrap !important;
  margin-left: 20px;
  flex-shrink: 1 !important; /* 允许收缩 */
  min-width: 0 !important; /* 允许缩小 */
  position: relative !important;
  display: flex !important;
  align-items: center !important;
  max-width: 400px !important; /* 限制最大宽度 */
  
  /* 文字颜色：使用森林系渐变色（从浅草绿到纯白） */
  background: linear-gradient(to bottom, #f0f9eb 0%, #ffffff 50%, #b8d9a8 100%) !important;
  -webkit-background-clip: text !important;
  background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
  color: transparent !important;
  
  /* 层叠阴影：营造立体感和外发光，模拟森林中的丁达尔效应 */
  text-shadow: none !important; /* 取消原有阴影，改用 filter 提升性能 */
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3)) !important;
  
  /* 动画1：文字整体微弱的呼吸浮动 */
  animation: forestBreath 4s ease-in-out infinite !important;
  cursor: default !important;
  user-select: none !important;
  
  /* 确保动画可以正常工作 */
  will-change: transform, filter !important;
}

/* 动态装饰：文字前方的"绿色能源"律动小球 */
.system-title::before {
  content: '' !important;
  position: absolute !important;
  left: -20px !important;
  top: 50% !important;
  transform: translateY(-50%) !important;
  width: 8px !important;
  height: 8px !important;
  background: #b8d9a8 !important;
  border-radius: 50% !important;
  box-shadow: 0 0 10px #b8d9a8, 0 0 20px rgba(184, 217, 168, 0.6) !important;
  /* 动画2：小球上下跳动，像森林中的萤火虫 */
  animation: firefly 3s ease-in-out infinite !important;
}

/* 动态流光：在文字上滑过的光影效果 */
.system-title::after {
  content: '碳排放核算与管理系统' !important;
  position: absolute !important;
  left: 0 !important;
  top: 0 !important;
  width: 100% !important;
  height: 100% !important;
  -webkit-background-clip: text !important;
  background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
  color: transparent !important;
  /* 使用高亮流光渐变 */
  background: linear-gradient(120deg, transparent 0%, transparent 40%, rgba(255,255,255,0.8) 50%, transparent 60%, transparent 100%) !important;
  background-size: 200% 100% !important;
  /* 动画3：流光循环 */
  animation: shine 6s infinite linear !important;
  pointer-events: none !important;
}

/* BJFU文字发光动画 */
@keyframes textGlow {
  0%, 100% {
    text-shadow: 
      0 2px 4px rgba(0, 0, 0, 0.3),
      0 4px 8px rgba(0, 0, 0, 0.2),
      0 0 10px rgba(255, 255, 255, 0.1) !important;
  }
  50% {
    text-shadow: 
      0 2px 4px rgba(0, 0, 0, 0.3),
      0 4px 8px rgba(0, 0, 0, 0.2),
      0 0 20px rgba(255, 255, 255, 0.2),
      0 0 30px rgba(255, 255, 255, 0.1) !important;
  }
}

/* --- 新增动画关键帧 --- */

/* 呼吸浮动动画 */
@keyframes forestBreath {
  0%, 100% {
    transform: translateY(0) scale(1) !important;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3)) !important;
  }
  50% {
    transform: translateY(-3px) scale(1.01) !important;
    /* 呼吸时发光增强 */
    filter: drop-shadow(0 4px 8px rgba(184, 217, 168, 0.4)) !important;
  }
}

/* 萤火虫跳动动画 */
@keyframes firefly {
  0%, 100% { 
    transform: translateY(-50%) scale(1) !important; 
    opacity: 0.8 !important; 
  }
  50% { 
    transform: translateY(-100%) scale(1.2) !important; 
    opacity: 1 !important; 
  }
}

/* 文字流光动画 */
@keyframes shine {
  0% { 
    background-position: 200% 0 !important; 
  }
  100% { 
    background-position: -200% 0 !important; 
  }
}

/* 悬停交互：鼠标移上去时绿色加深，模拟生命力增强 */
.system-title:hover {
  transition: all 0.5s ease !important;
  filter: drop-shadow(0 0 12px rgba(184, 217, 168, 0.8)) !important;
}


</style>
<style>
/* 全局样式：强制覆盖 Element UI 默认样式，防止菜单项激活时抖动 */
/* 使用最高优先级的选择器覆盖 Element UI 的默认样式 */
.el-menu--horizontal .el-menu-item,
.el-menu--horizontal .el-menu-item.is-active {
  border-bottom-width: 0px !important;
  border-bottom-style: solid !important;
  box-sizing: border-box !important;
  height: 80px !important;
  line-height: 80px !important;
  margin: 0 !important;
  padding: 0 15px !important;
  vertical-align: top !important;
}

.el-menu--horizontal .el-menu-item:not(.is-active) {
  border-bottom-color: transparent !important;
}

.el-menu--horizontal .el-menu-item.is-active {
  border-bottom-color: #b8d9a8 !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
}

.user-dropdown-menu {
  min-width: 120px !important;
  margin-top: 5px !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 16px rgba(45, 80, 22, 0.15) !important;
  border: 1px solid #c8e0c0 !important;
  overflow: hidden;
}

.user-dropdown-menu .el-dropdown-menu__item {
  text-align: center;
  padding: 12px 20px;
  color: #1a3d0d !important;
  transition: all 0.3s ease;
}

.user-dropdown-menu .el-dropdown-menu__item:hover {
  background: #e8f5e3 !important;
  color: #2d5016 !important;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0 !important; /* 头像不允许收缩 */
  margin-left: 15px !important; /* 减小左边距 */
  height: 80px;
  width: 50px;
  min-width: 50px !important; /* 确保最小宽度 */
}

.avatar .el-dropdown {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
}

.user-img-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  width: 40px !important;
  height: 40px !important;
  min-width: 40px !important;
  min-height: 40px !important;
  max-width: 40px !important;
  max-height: 40px !important;
  line-height: 40px;
  vertical-align: middle;
  border-radius: 50% !important;
  overflow: hidden;
  box-sizing: border-box;
  padding: 0;
  margin: 0;
  background: rgba(255, 255, 255, 0.2) !important;
  border: 2px solid rgba(255, 255, 255, 0.3) !important;
  transition: all 0.3s ease;
}

.user-img-icon:hover {
  background: rgba(255, 255, 255, 0.3) !important;
  border-color: rgba(255, 255, 255, 0.5) !important;
  transform: scale(1.05);
}

.user-img-icon i {
  font-size: 24px;
  color: white;
  display: inline-block;
  line-height: 1;
  vertical-align: middle;
  margin: 0;
  padding: 0;
}

.el-menu--horizontal>.el-submenu .el-submenu__title {
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 20px;
  display: inline-block;
}

/* 使用更具体的选择器，确保覆盖所有菜单项，包括带header-icon类的 */
.el-menu--horizontal>.el-menu-item,
.tan-el-menu .el-menu-item,
div.right ul li.el-menu-item,
.el-menu-item.header-icon,
.el-menu-item.el-icon-s-home,
.el-menu-item.el-icon-s-order {
  height: 80px !important;
  line-height: 80px !important;
  /* 关键：始终保留一个透明边框，避免激活时布局变化 */
  border-bottom: 3px solid transparent !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
  margin: 0 !important;
  padding: 0 12px !important; /* 减小内边距，使菜单更紧凑 */
  box-sizing: border-box !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  position: relative !important;
  vertical-align: top !important; /* 确保垂直对齐 */
  /* 确保所有状态下的高度一致 */
  min-height: 80px !important;
  max-height: 80px !important;
}

.el-submenu__title i {
  color: rgba(255, 255, 255, 0.9) !important;
}

.el-submenu__title {
  margin-right: 0px;
  padding: 0 8px;
  font-size: 20px !important;
}

.tan-el-menu {
  width: auto !important; /* 改为自动宽度 */
  max-width: none !important; /* 移除最大宽度限制 */
  white-space: nowrap;
  background: transparent !important;
  overflow: visible !important; /* 允许溢出可见 */
  border-bottom: none !important;
  flex-shrink: 0 !important; /* 导航菜单不允许收缩 */
  min-width: 0 !important;
  margin-left: auto !important; /* 自动左边距，推动菜单靠右 */
}

.tan-el-menu .el-menu-item,
.tan-el-menu .el-submenu__title {
  color: rgba(255, 255, 255, 0.9) !important;
  transition: background-color 0.3s ease, color 0.3s ease, border-bottom-color 0.3s ease !important; /* 只过渡颜色，不过渡布局属性 */
  /* 始终保留透明边框，避免激活时布局变化 */
  border-bottom: 3px solid transparent !important;
  margin: 0 !important;
  padding: 0 12px !important; /* 减小内边距，使菜单更紧凑 */
  height: 80px !important; /* 与header高度一致 */
  line-height: 80px !important; /* 垂直居中 */
  display: flex !important;
  align-items: center !important;
  box-sizing: border-box !important; /* 确保边框包含在高度内 */
}

.tan-el-menu .el-menu-item:hover,
.tan-el-menu .el-submenu__title:hover {
  color: white !important;
  background: rgba(255, 255, 255, 0.1) !important;
}

/* 确保所有普通菜单项（无下拉）的选中效果一致 */
.el-menu--horizontal>.el-menu-item:not(.el-submenu__title) {
  /* 始终保留透明边框，避免激活时布局变化 */
  border-bottom: 3px solid transparent !important;
  border-top: none !important;
}

.el-menu--horizontal>.el-menu-item:not(.el-submenu__title).is-active {
  /* 只改变边框颜色，不改变宽度，避免布局变化 */
  border-bottom: 3px solid #b8d9a8 !important;
  border-top: none !important;
  position: relative !important;
  /* 确保激活时位置不变 */
  top: 0 !important;
  margin-top: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  height: 80px !important;
  line-height: 80px !important;
  transform: none !important;
  box-sizing: border-box !important;
}

.el-menu--horizontal>.el-menu-item:not(.el-submenu__title).is-active::after {
  display: none !important;
}

.tan-el-menu .el-menu-item.is-active {
  color: white !important;
  background: rgba(255, 255, 255, 0.15) !important;
  /* 只改变边框颜色，不改变宽度，避免布局变化 */
  border-bottom: 3px solid #b8d9a8 !important;
  position: relative !important;
}

/* 移除伪元素，直接使用边框 */
.tan-el-menu .el-menu-item.is-active::after {
  display: none !important;
}

/* 下拉菜单样式 - 森林系主题 */
.el-menu--popup {
  background: white !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 16px rgba(45, 80, 22, 0.15) !important;
  border: 1px solid #c8e0c0 !important;
  padding: 8px 0 !important;
}

.el-menu--popup .el-menu-item {
  color: #1a3d0d !important;
  padding: 10px 20px !important;
  line-height: 1.5 !important;
  height: auto !important;
  min-height: 40px !important;
  transition: all 0.3s ease;
  display: flex !important;
  align-items: center !important;
}

.el-menu--popup .el-menu-item:hover {
  background: #e8f5e3 !important;
  color: #2d5016 !important;
}

.el-menu--popup .el-menu-item.is-active {
  background: #e8f5e3 !important;
  color: #2d5016 !important;
  border-left: 3px solid #4a7c3a;
}

.el-menu--popup .el-menu-item i {
  color: #4a7c3a !important;
  margin-right: 8px;
  font-size: 18px !important;
  line-height: 1 !important;
  vertical-align: middle !important;
}

.el-menu--popup .el-menu-item span {
  line-height: 1.5 !important;
  vertical-align: middle !important;
}

.tan-el-menu .header-icon {
  color: rgba(255, 255, 255, 0.9) !important;
  margin-right: 6px;
}

.tan-el-menu .el-submenu__title i {
  color: rgba(255, 255, 255, 0.9) !important;
}

.el-menu-item-group__title {
  width: 0px;
  height: 0px;
  margin: 0;
  padding: 0;
  display: none !important;
}

.el-menu-item-group {
  padding: 0 !important;
  margin: 0 !important;
}

.el-menu--popup-bottom-start {
  margin: 0;
}

.el-menu.el-menu--horizontal {
  height: 80px !important;
  border: 0 !important;
  display: flex !important;
  align-items: center !important;
  /* 确保菜单容器不会影响子元素位置 */
  position: relative !important;
  gap: 0 !important; /* 移除菜单项之间的间距 */
}

/* 确保菜单项在容器中正确对齐，并减少间距 */
.el-menu.el-menu--horizontal > li {
  vertical-align: top !important;
  display: inline-flex !important;
  align-items: center !important;
  margin: 0 !important; /* 移除外边距 */
}

.el-menu--horizontal>.el-submenu .el-submenu__title {
  height: 80px;
  line-height: 80px;
}

/* 使用最具体的选择器，确保覆盖所有激活状态的菜单项，包括带header-icon类的 */
.el-menu--horizontal>.el-menu-item.is-active,
.tan-el-menu .el-menu-item.is-active,
div.right ul li.el-menu-item.is-active,
.el-menu-item.header-icon.is-active,
.el-menu-item.el-icon-s-home.is-active,
.el-menu-item.el-icon-s-order.is-active,
#app > div > div.header > div > div.right > ul > li.el-menu-item.is-active,
#app > div > div.header > div > div.right > ul > li.el-menu-item.header-icon.is-active,
#app > div > div.header > div > div.right > ul > li.el-menu-item.el-icon-s-home.is-active {
  /* 关键：保持相同的边框宽度，只改变颜色，避免布局变化 */
  border-bottom: 3px solid #b8d9a8 !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
  position: relative !important;
  color: white !important;
  background: rgba(255, 255, 255, 0.15) !important;
  /* 确保激活时位置不变 */
  top: 0 !important;
  margin: 0 !important;
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  padding: 0 15px !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  height: 80px !important; /* 固定高度 */
  line-height: 80px !important; /* 固定行高 */
  min-height: 80px !important;
  max-height: 80px !important;
  transform: none !important; /* 禁止任何变换 */
  box-sizing: border-box !important; /* 确保边框包含在高度内 */
  /* 确保没有其他影响布局的属性 */
  overflow: visible !important;
  vertical-align: top !important;
}

/* 移除伪元素，直接使用边框 */
.el-menu--horizontal>.el-menu-item.is-active::after,
.tan-el-menu .el-menu-item.is-active::after,
div.right ul li.el-menu-item.is-active::after,
.el-menu-item.header-icon.is-active::after,
.el-menu-item.el-icon-s-home.is-active::after {
  display: none !important;
}
</style>

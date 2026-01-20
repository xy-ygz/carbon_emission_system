<template>
  <div class="total">
    <div class="header">
      <div class="carbonleft">
        <h1 style="font-style: oblique;">北京林业大学碳排放核算与管理系统</h1>
      </div>
      <div class="adminright" >
        <el-menu router class="tan-el-menu" style="background-color: steelblue" mode="horizontal">
          <el-menu-item index="/TanHome/adminPage" class="header-icon el-icon-s-home" style="font-size: 23px;color: #353A39">
            首页
            <div v-show="defaultActive == '/TanHome/adminPage' "></div></el-menu-item>
          <!-- <el-menu-item index="/Tan/TanData" class="header-icon el-icon-edit" style="font-size: 23px;color: #353A39">数据输入
          </el-menu-item> -->
          <el-submenu index="1">
            <template slot="title">
              <i class="header-icon el-icon-edit"></i>
              <span>数据输入</span>
            </template>
            <el-menu-item-group>
              <el-menu-item class="el-icon-location" index="/TanHome/ManageCarbon"
                            style="font-size: 20px;  display: block; padding-left: 5%;">&nbsp;碳排放输入</el-menu-item>
            </el-menu-item-group>
          </el-submenu>
          <!-- <el-menu-item @click="jump(true)" class="header-icon el-icon-s-flag"
            style="font-size: 23px;color: #353A39">能耗监测</el-menu-item> -->
          <el-submenu index="2">
            <template slot="title">
              <i class="header-icon el-icon-s-flag"></i>
              <span>能耗监测</span>
            </template>
            <el-menu-item-group>
              <el-menu-item @click="jump(true)" class="el-icon-location"
                            style="font-size: 20px;  display: block; padding-left: 5%;">&nbsp;&nbsp;单体监测</el-menu-item>
              <el-menu-item @click="jump(true)" class="el-icon-location"
                            style="font-size: 20px;  display: block; padding-left: 5%;">&nbsp;&nbsp;能源审计</el-menu-item>
              <el-menu-item @click="jump(true)" class="el-icon-location"
                            style="font-size: 20px;  display: block; padding-left: 5%; width: 240px;">&nbsp;&nbsp;能耗碳排放量对比</el-menu-item>
            </el-menu-item-group>
          </el-submenu>
          <!-- <el-menu-item @click="jump(true)" class="header-icon el-icon-s-data"
            style="font-size: 23px;color: #353A39">碳排放计算与减碳分析</el-menu-item> -->
          <el-submenu index="3">
            <template slot="title">
              <i class="header-icon el-icon-s-data"></i>
              <span>碳排放计算与减碳分析</span>
            </template>
            <el-menu-item-group>
              <el-menu-item @click="jump(true)" class="el-icon-location"
                            style="font-size: 20px;  display: block; padding-left: 5%;">&nbsp;&nbsp;流动趋势</el-menu-item>
              <el-menu-item @click="jump(true)" class="el-icon-location"
                            style="font-size: 20px;  display: block; padding-left: 5%;">&nbsp;&nbsp;减碳分析</el-menu-item>
            </el-menu-item-group>
          </el-submenu>

          <el-menu-item @click="jump(true)" class="header-icon el-icon-s-order"
                        style="font-size: 23px;color: #353A39">报告生成</el-menu-item>
        </el-menu>
      </div>
      <div class="guanliyuan">
        <el-dropdown @command="handleCommand" menu-align='start' class="float-right">
          <h3 style="color: #eeeeee">管理员端</h3>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="signout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </div>
    <div class="child">
      <router-view></router-view>
    </div>
  </div>
</template>
<script>
import { logoutApi } from '../../api/user'
import tokenManager from '../../utils/tokenManager'

export default {
  computed:{
    defaultActive(){
      let path = this.$route.path;
      if(path.indexOf("adminPage") !== -1){
        if(path.indexOf("adminPage") !== -1){
          return '/TanHome/adminPage';
        }else{
          return '/TanHome/adminPage';
        }
      }else{
        console.log("path" + path);
        return path;
      }
    }
  },
  methods: {
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
        this.$router.push({ path: '/Tan/TanPage' })
      } else if (command == 'backtoindex') {
        this.$router.push({ path: '/TanHome/adminPage' })
      }
    }
  }
}
// admin-col-box-menu: overflow:auto ;
</script>

<style scoped>
.tan-el-menu-item.is-active{
  font-weight: bold;
  background: steelblue;
}
.tan-el-menu-item:hover{
  background: steelblue;
}
.admin-el-menu-item{
  color: black !important;
  background-color: steelblue;
  margin-top: 0px;
  height: 50px;
}
.total {
  padding: 0;
  width: 100%;
  height: 100%;
}

.header {
  position: fixed;
  background-color: steelblue;
  height:80px;
  width:100%;
  /* display: inline-block; */
  top: 0px;
  z-index: 9;
  /* text-align: left; */
}
.adminright,.guanliyuan{
  display: inline-block;
}
.guanliyuan{
  margin-top: 18px;
  vertical-align: top;
  color: #eeeeee;
}
.carbonleft{
  display: inline-block;
  margin-right: 10px;
}
.adminright {
  height: 63px;
  /* margin-top: 17px; */
  padding-top: 17px;
  vertical-align: top;
  text-align: right;
  display: inline-block;
  background-color: steelblue;
}

.child {
  margin-top: 60px;
  text-align: center;
  padding: 10px;
  width: 100%;
  height: 100%;
}
</style>
<style>

.el-menu--horizontal>.el-submenu .el-submenu__title {
  color: #353A39 !important;
  font-size: 23px;

}
.el-menu--horizontal>.el-menu-item{
  height: 63px;
}
.el-submenu__title i {
  color: #353A39;
}
.el-submenu__title{
  margin-right: 0px;
  padding: 0 10px;
}
.tan-el-menu{
  background-color: steelblue !important;
}
.el-menu-item-group__title{
  width: 0px;
  height: 0px;
  margin: 0;
  padding: 0;
}

.el-menu--popup-bottom-start {
  margin: 0;
}
.el-menu.el-menu--horizontal{
  height: 60px;
  border: 0;
}

.el-menu--horizontal>.el-submenu .el-submenu__title{
  height: 65px;
}
</style>

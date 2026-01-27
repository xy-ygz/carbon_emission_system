<template>
<div id="app" class="fillcontain">
    <!-- <img src="./assets/logo.png"> -->
<!--    <router-view v-if="routerAlive"></router-view>-->
    <router-view/>
  </div>
</template>

<script>
export default {
  name: 'App',
  provide() {
    return {
      reload: this.reload,
    }
  },

  data() {
    return { routerAlive: true }
  },

  methods: {
  }
}
</script>

<style>
@import './assets/css/common.css';
@import './assets/css/forest-theme.css';
@import './assets/css/element-forest-theme.css';

html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  overflow-x: hidden; /* 只隐藏横向滚动条 */
  overflow-y: auto; /* 允许垂直滚动 */
}

#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #1a3d0d;
  background-color: #f0f7ed;
  width: 100%;
  min-height: 100%; /* 改为 min-height，允许内容超出时滚动 */
  overflow-x: hidden; /* 只隐藏横向滚动条 */
  overflow-y: auto; /* 允许垂直滚动 */
  box-sizing: border-box;
  position: relative;
}

.fillcontain {
  position: relative;
  width: 100%;
  min-height: 100%; /* 改为 min-height，允许内容超出时滚动 */
  overflow-x: hidden; /* 只隐藏横向滚动条 */
  overflow-y: auto; /* 允许垂直滚动 */
}
</style>

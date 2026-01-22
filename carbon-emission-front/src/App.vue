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

#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #1a3d0d;
  background-color: #f0f7ed;
  width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
  position: relative;
}

.fillcontain {
  position: relative;
  width: 100%;
  height: 100%;
  overflow-x: hidden;
}
</style>

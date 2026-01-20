// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.

import Vue from 'vue'
import App from './App'
import VueRouter from "vue-router";
import ElementUI from 'element-ui';
ElementUI.Dialog.props.lockScroll.default = false;
import axios from "axios";
import 'element-ui/lib/theme-chalk/index.css';
import router from './router'
import './assets/css/customize.css'
import './assets/css/common.css'
import './assets/css/forest-theme.css'
import './assets/css/element-forest-theme.css'
import * as echarts from 'echarts'
import tokenManager from './utils/tokenManager'

Vue.prototype.$echarts = echarts
Vue.config.productionTip = false
import moment from "moment"
Vue.use(ElementUI);
Vue.use(VueRouter);

tokenManager.setupMultiTabSync();

Vue.filter('formatDate',function (value) {
  return moment(value).format('YYYY-MM-DD HH:mm:ss')
})
/* eslint-disable no-new */
new Vue({
  el: '#app',
  router,
  components: { App },
  template: '<App/>',
  beforeCreate() {
    Vue.prototype.$bus = this
  }
})

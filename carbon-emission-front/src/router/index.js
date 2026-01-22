import Vue from "vue";
import Router from "vue-router";
import HelloWorld from "../components/HelloWorld";
import Login from "../page/Login";

import TanTotal from "../components/TanTotal.vue";
import TanPage from "../page/tan/TanPage";
import TanContrast from "../page/tan/TanContrast";
import TanAudit from "../page/tan/TanAudit";
import TanMonitor from "../page/tan/TanMonitor";
import ExchangeSetting from "../page/admin/ExchangeSetting";
import ManageCarbon from "../page/admin/ManageCarbon";
import ManagePlace from "../page/admin/ManagePlace";
import ManageSchool from "../page/admin/ManageSchool";
import ManageUser from "../page/admin/ManageUser";
import ManageRole from "../page/admin/ManageRole";
import ManagePermission from "../page/admin/ManagePermission";
import TanResult from "../page/tan/TanResult";
import TanAnalyse from "../page/tan/TanAnalyse";
import TanExport from "../page/tan/TanExport";
import { Message } from "element-ui";
Vue.prototype.$message = Message;
Vue.use(Router);

const router = new Router({
  routes: [
    {
      path: "/",
      redirect: "/Tan/TanPage"
    },
    {
      path: "/Login",
      redirect: "/Tan/TanLogin"
    },
    {
      path: "/Tan",
      name: "Tan",
      component: TanTotal,
      redirect: "/Tan/TanPage",
      children: [
        {
          path: "TanPage",
          name: "TanPage",
          component: TanPage
        },
        {
          path: "TanLogin",
          name: "TanLogin",
          component: Login
        },
        {
          path: "ManageSchool",
          name: "ManageSchool",
          component: ManageSchool,
          meta: {
            authRequired: true
            // 只需要登录即可访问，增删改操作由后端@PreAuthorize控制
          }
        },
        {
          path: "ManageCarbon",
          name: "ManageCarbon",
          component: ManageCarbon,
          meta: {
            authRequired: true
            // 只需要登录即可访问，增删改操作由后端@PreAuthorize控制
          }
        },
        {
          path: "ExchangeSetting",
          name: "ExchangeSetting",
          component: ExchangeSetting,
          meta: {
            authRequired: true
            // 只需要登录即可访问，增删改操作由后端@PreAuthorize控制
          }
        },
        {
          path: "ManagePlace",
          name: "ManagePlace",
          component: ManagePlace,
          meta: {
            authRequired: true
            // 只需要登录即可访问，增删改操作由后端@PreAuthorize控制
          }
        },
        {
          path: "TanContrast",
          name: "TanContrast",
          component: TanContrast,
          meta: {
            authRequired: true
          }
        },
        {
          path: "TanAudit",
          name: "TanAudit",
          component: TanAudit,
          meta: {
            authRequired: true
          }
        },
        {
          path: "TanMonitor",
          name: "TanMonitor",
          component: TanMonitor,
          meta: {
            authRequired: true
          }
        },
        {
          path: "TanResult",
          name: "TanResult",
          component: TanResult,
          meta: {
            authRequired: true
          }
        },
        {
          path: "TanAnalyse",
          name: "TanAnalyse",
          component: TanAnalyse,
          meta: {
            authRequired: true
          }
        },
        {
          path: "TanExport",
          name: "TanExport",
          component: TanExport,
          meta: {
            authRequired: true
          }
        },
        {
          path: "ManageUser",
          name: "ManageUser",
          component: ManageUser,
          meta: {
            authRequired: true,
            requireRole: "admin"
          }
        },
        {
          path: "ManageRole",
          name: "ManageRole",
          component: ManageRole,
          meta: {
            authRequired: true,
            requireRole: "admin"
          }
        },
        {
          path: "ManagePermission",
          name: "ManagePermission",
          component: ManagePermission,
          meta: {
            authRequired: true,
            requireRole: "admin"
          }
        }
      ]
    }
  ]
});

const originalPush = Router.prototype.push;
Router.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err);
};
// 路由前置守卫：权限校验
router.beforeEach(async (to, from, next) => {
  const { authRequired, requireRole } = to.meta;
  
  // 如果访问登录页，直接放行
  if (to.name === "TanLogin") {
    next();
    return;
  }
  
  // 如果需要登录验证
  if (authRequired === true) {
    const token = localStorage.getItem("token");
    const auth_role = localStorage.getItem("auth");
    
    // 检查是否有token（登录状态）
    if (!token) {
      // 未登录，跳转到登录页（在路由跳转前拦截）
      Vue.prototype.$message.warning("请先完成登录！");
      next({
        name: "TanLogin",
        params: {
          curRoute: to.path
        }
      });
      return;
    }
    
    // 如果指定了特定角色要求，检查角色
    if (requireRole) {
      if (auth_role === requireRole) {
        next();
      } else {
        Vue.prototype.$message.warning("权限不足，请先完成登录！");
        next({
          name: "TanLogin",
          params: {
            curRoute: to.path
          }
        });
      }
    } else {
      // 只需要登录即可，有token就放行
      next();
    }
    return;
  }
  
  //无效路由，直接跳回首页
  if (to.matched.length === 0) {
    next({
      path: "/Tan/TanPage"
    });
    return;
  }
  
  // 其他情况直接放行
  next();
});

router.afterEach((to, from) => {
  // 跳转之后滚动条回到顶部
  window.scrollTo(0, 0);
});

export default router;

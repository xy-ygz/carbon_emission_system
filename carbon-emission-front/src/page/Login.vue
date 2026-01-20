<template>
  <div class="beijing">
    <button
      @click="denglu()"
      style="
        font-size: 23px;
        border-style: none;
        background: rgba(0, 0, 0, 0);
        margin-right: 80px;
      "
    ></button>
    <div class="box-card">
      <el-card class="form_container">
        <template #header>
          <div class="card-title">欢迎登录北京林业大学碳排放核算与管理系统</div>
        </template>
        <el-form ref="user" :model="user" :rules="rules">
          <el-form-item prop="username" style="margin-bottom: 5%">
            <el-input
              v-model="user.username"
              placeholder="用户名"
              type="text"
            />
          </el-form-item>
          <el-form-item prop="password" style="margin-bottom: 5%">
            <el-input
              v-model="user.password"
              placeholder="密码"
              type="password"
              @keyup.enter.native="onSubmit('user')"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              class="button-long"
              type="primary"
              @click="onSubmit()"
              :loading="loading"
              round
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import { login } from "../api/user";
import background from "../pic/background.png";
import tokenManager from "../utils/tokenManager";

//消息订阅与发布
import pubsub from "pubsub-js";

export default {
  name: "Login",
  data() {
    return {
      loginForm: false,
      user: {
        username: "user",
        password: "123456",
      },
      rules: {
        username: [
          { required: true, message: "请输入用户名", trigger: "blur" },
        ],
        password: [{ required: true, message: "请输入密码", trigger: "blur" }],
      },
      loading: false,
      isSubmitting: false,
    };
  },

  created() {
    CSSLayerBlockRule;
    document.onkeydown = (e) => {
      e = window.event || e;

      let that = this;
      if (this.$route.path === "/Tan/TanLogin" && e.keyCode === 13 && !this.isSubmitting && !this.loading) {
        this.onSubmit();
      }
    };
  },
  beforeDestroy() {
    pubsub.unsubscribe(this.pubId);
  },
  methods: {
    denglu() {
      this.loginForm = true;
    },
    onSubmit() {
      if (this.isSubmitting || this.loading) {
        return;
      }
      
      this.isSubmitting = true;
      this.loading = true;
      
      login({
        username: this.user.username,
        password: this.user.password
      })
        .then((res) => {
          if (res.data.code == 200) {
            let type = res.data.data.type;
            localStorage.setItem("id", res.data.data.id);
            
            const { token, refreshToken, expiresIn } = res.data.data;
            tokenManager.setTokens(token, refreshToken, expiresIn);
            
            this.$message({
              message: "您好！" + res.data.data.name + ", 欢迎登录系统",
              type: "success",
            });

            localStorage.setItem("auth", "admin");
            if (this.$route.params.curRoute == null) {
              this.$router.push({ path: "/Tan/TanPage" });
            } else {
              this.$router.push({ path: this.$route.params.curRoute });
            }

            this.$bus.$emit("updateLoginStatus", "admin");
          } else {
            // 优先显示description，如果没有则显示message
            const errorMsg = res.data.description || res.data.message || '登录失败';
            this.$message({
              message: errorMsg,
            });
            this.loading = false;
            this.isSubmitting = false;
          }
        })
        .catch(() => {
          this.loading = false;
          this.isSubmitting = false;
        })
        .finally(() => {
        });
    },
  },
};
</script>

<style lang="css" scoped>
div {
  margin: 0;
  padding: 0;
}

.beijing {
  width: 100%;
  min-height: calc(100vh - 80px); /* 确保至少占满剩余视口高度 */
  height: auto; /* 改为 auto，让内容自然撑开 */
  padding-bottom: 40px; /* 添加底部内边距，确保按钮可见 */

  background: url(../assets/bg.jpg);
  background-size: cover; /* 改为 cover，更好地适应屏幕 */
  background-position: center; /* 居中背景图 */
  background-repeat: no-repeat; /* 不重复 */
}

.form_container {
  height: auto; /* 改为 auto，让内容自然撑开 */
  min-height: 265px; /* 设置最小高度 */
  position: relative; /* 改为 relative，避免绝对定位导致的问题 */
  margin: 80px auto 40px; /* 添加上下边距，确保可见 */
  background: #fff;
  width: 430px;
  max-width: 90%; /* 添加最大宽度，响应式 */
  border-radius: 5px;
  padding: 45px;
  text-align: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1); /* 添加阴影，提升视觉效果 */
}
</style>

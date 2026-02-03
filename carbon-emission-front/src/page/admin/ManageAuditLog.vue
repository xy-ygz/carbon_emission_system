<template>
  <div class="main-box-admin">
    <el-breadcrumb separator="/" style="font-size: 18px; margin-bottom: 20px;">
      <el-breadcrumb-item :to="{ path: '/Tan/TanPage' }">
        <i class="el-icon-s-home"></i> 首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>日志审计</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="forest-card" style="padding: 20px; margin-bottom: 20px;">
      <!-- 条件筛选 -->
      <div class="flex-box-header-new">
        <div class="energyleft">
          <el-form>
            <el-tag style="font-size: 13px;">用户ID：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchUserId" placeholder="请输入用户ID" size="small" style="width:150px" type="number"></el-input>
            </el-form-item>
            <el-tag style="font-size: 13px; margin-left: 10px;">用户名：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchUsername" placeholder="请输入用户名" size="small" style="width:150px"></el-input>
            </el-form-item>
            <el-tag style="font-size: 13px; margin-left: 10px;">接口名：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchUrl" placeholder="请输入接口URL" size="small" style="width:200px"></el-input>
            </el-form-item>
            <el-tag style="font-size: 13px; margin-left: 10px;">开始日期：</el-tag>
            <el-form-item style="display: inline-block">
              <el-date-picker
                v-model="startDate"
                type="date"
                placeholder="选择开始日期"
                size="small"
                style="width:150px"
                value-format="yyyy-MM-dd"
                format="yyyy-MM-dd">
              </el-date-picker>
            </el-form-item>
            <el-tag style="font-size: 13px; margin-left: 10px;">结束日期：</el-tag>
            <el-form-item style="display: inline-block">
              <el-date-picker
                v-model="endDate"
                type="date"
                placeholder="选择结束日期"
                size="small"
                style="width:150px"
                value-format="yyyy-MM-dd"
                format="yyyy-MM-dd">
              </el-date-picker>
            </el-form-item>
            <el-button class="form-item-inline" size="small" @click="getLogList" plain
              icon="el-icon-search">查询</el-button>
            <el-button class="form-item-inline" type="success" size="small" @click="resetSearch" plain
              icon="el-icon-refresh">重置</el-button>
          </el-form>
        </div>
      </div>
      
      <!-- 日志列表 -->
      <div class="table_container" style="width: 100%; overflow-x: auto; overflow-y: visible;">
        <el-table :data="tableData" :key="tableKey" style="width: 100%;font-size: 15px;" size="small" class="forest-table">
          <el-table-column label="序号" min-width="80" align="center">
            <template slot-scope="scope">
              {{ (scope.$index + 1) + (current - 1) * limit }}
            </template>
          </el-table-column>
          <el-table-column prop="userId" label="用户ID" min-width="100" align="center"></el-table-column>
          <el-table-column prop="username" label="用户名" min-width="120" align="center"></el-table-column>
          <el-table-column prop="ip" label="请求IP" min-width="130" align="center"></el-table-column>
          <el-table-column prop="url" label="请求URL" min-width="250" align="center" show-overflow-tooltip></el-table-column>
          <el-table-column prop="method" label="请求方法" min-width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="getMethodTagType(scope.row.method)" size="small">
                {{ scope.row.method }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="params" label="请求参数" min-width="200" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <span v-if="scope.row.params">{{ scope.row.params.length > 50 ? scope.row.params.substring(0, 50) + '...' : scope.row.params }}</span>
              <span v-else style="color: #999;">无</span>
            </template>
          </el-table-column>
          <el-table-column prop="statusCode" label="状态码" min-width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="getStatusCodeTagType(scope.row.statusCode)" size="small">
                {{ scope.row.statusCode }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="responseTime" label="响应时间(ms)" min-width="120" align="center">
            <template slot-scope="scope">
              <el-tag :type="getResponseTimeTagType(scope.row.responseTime)" size="small">
                {{ scope.row.responseTime }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="请求时间" min-width="180" align="center"></el-table-column>
        </el-table>
        <div style="width: 100%; text-align:center; margin:20px;padding-right: 50px;">
          <el-pagination background @current-change="handleCurrentChange" :page-size="limit" :current-page.sync="current"
            layout="total, prev, pager, next" :total="total" v-if="pageshow">
          </el-pagination>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getLogList } from "../../api/auditLog";

export default {
  data() {
    return {
      tableData: [],
      current: 1,
      size: 10,
      limit: 10,
      total: 0,
      pageshow: true,
      tableKey: 0,
      
      // 搜索条件
      searchUserId: null,
      searchUsername: '',
      searchUrl: '',
      startDate: null,
      endDate: null
    }
  },
  mounted: function () {
    this.current = 1
    this.getLogList()
  },
  activated() {
    // 组件激活时刷新数据
    this.getLogList()
  },
  methods: {
    // 获取日志列表
    getLogList() {
      const params = {
        current: this.current,
        size: this.limit
      }
      
      if (this.searchUserId) {
        params.userId = this.searchUserId
      }
      if (this.searchUsername) {
        params.username = this.searchUsername
      }
      if (this.searchUrl) {
        params.url = this.searchUrl
      }
      if (this.startDate) {
        params.startDate = this.startDate
      }
      if (this.endDate) {
        params.endDate = this.endDate
      }
      
      getLogList(params).then(res => {
        if (res.data.code == 200) {
          const result = res.data.data
          this.tableData = result.records || []
          this.total = result.total || 0
          this.pageshow = this.total > 0
          
          // 强制表格重新渲染
          this.$nextTick(() => {
            this.tableKey++
          })
        } else {
          this.$message.error(res.data.description || res.data.message || "获取日志列表失败！");
        }
      }).catch((error) => {
        this.$message.error("获取日志列表失败！");
        console.error(error);
      });
    },
    
    // 重置搜索条件
    resetSearch() {
      this.searchUserId = null
      this.searchUsername = ''
      this.searchUrl = ''
      this.startDate = null
      this.endDate = null
      this.current = 1
      this.getLogList()
    },
    
    // 分页变化
    handleCurrentChange(val) {
      this.current = val
      this.getLogList()
    },
    
    // 获取请求方法的标签类型
    getMethodTagType(method) {
      if (!method) return 'info'
      const upperMethod = method.toUpperCase()
      switch (upperMethod) {
        case 'GET':
          return 'success'
        case 'POST':
          return 'primary'
        case 'PUT':
          return 'warning'
        case 'DELETE':
          return 'danger'
        default:
          return 'info'
      }
    },
    
    // 获取状态码的标签类型
    getStatusCodeTagType(statusCode) {
      if (!statusCode) return 'info'
      if (statusCode >= 200 && statusCode < 300) {
        return 'success'
      } else if (statusCode >= 300 && statusCode < 400) {
        return 'warning'
      } else if (statusCode >= 400 && statusCode < 500) {
        return 'warning'
      } else if (statusCode >= 500) {
        return 'danger'
      }
      return 'info'
    },
    
    // 获取响应时间的标签类型
    getResponseTimeTagType(responseTime) {
      if (!responseTime) return 'info'
      if (responseTime < 100) {
        return 'success'
      } else if (responseTime < 500) {
        return 'warning'
      } else {
        return 'danger'
      }
    }
  }
}
</script>

<style scoped>
.main-box-admin {
  padding: 20px;
}

.forest-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.flex-box-header-new {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.energyleft {
  flex: 1;
}

.form-item-inline {
  margin-left: 10px;
}

.table_container {
  margin-top: 20px;
}
</style>

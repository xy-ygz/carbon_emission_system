<template>
  <div class="main-box-admin">
    <el-breadcrumb separator="/" style="font-size: 18px; margin-bottom: 20px;">
      <el-breadcrumb-item :to="{ path: '/Tan/ManageCarbon' }">
        <i class="el-icon-s-home"></i> 数据输入管理首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>碳排放转化系数</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="forest-card" style="padding: 20px; margin-bottom: 20px;">
      <div class="flex-box-header-new">
        <div class="energyleft">
          <el-form>
            <el-tag style="font-size: 13px;">输入分类名称：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchObjectCategory" placeholder="请输入分类名称" size="small" style="width:200px"></el-input>
            </el-form-item>
            <el-button class="form-item-inline" size="small" @click="getExchangeSettings" plain
              icon="el-icon-search">查询</el-button>
            <el-button class="form-item-inline" type="success" size="small" @click="getAllEs()" plain
              icon="el-icon-search">显示全部</el-button>
          </el-form>
        </div>
        <div style="margin-left: 50px;margin-bottom: 21px;">
          <el-form>
            <el-button class="form-item-inline" type="primary" size="small" @click="showAddDialog">新增分类转换</el-button>
          </el-form>
        </div>
      </div>
      <div class="table_container" style="width: 100%; overflow-x: auto; overflow-y: visible;">
        <el-table :data="tableData" style="width: 100%;font-size: 15px;" size="small" class="forest-table">
          <el-table-column label="序号" min-width="100" align="center">
            <template slot-scope="scope">
              {{ (scope.$index + 1) + (current - 1) * limit }}
            </template>
          </el-table-column>
          <el-table-column prop="objectCategory" label="分类" min-width="200" align="center" show-overflow-tooltip></el-table-column>
          <el-table-column prop="exchangeCoefficient" label="碳排放转化系数" min-width="180" align="center"></el-table-column>
          <el-table-column prop="unit" label="单位" min-width="120" align="center"></el-table-column>
          <el-table-column label="操作" min-width="200" fixed="right" align="center">
            <template slot-scope="scope">
              <div class="updBtn" style="display: inline-block;"><el-button @click="showUpdateDialog(scope.row)"
                  type="warning" plain size="small" icon="el-icon-edit">修改</el-button></div>
              <div class="delBtn" style="display: inline-block;">
                <el-button @click="deleteExchangeSettingById(scope.row)" type="danger" plain size="small"
                  icon="el-icon-delete">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <!-- 换页菜单 -->
        <div style="width: 100%; text-align:center; margin:20px;padding-right: 50px;">
          <el-pagination background @current-change="handleCurrentChange" :page-size="limit" :current-page.sync="current"
            layout="total, prev, pager, next" :total="total" v-if="pageshow">
          </el-pagination>
        </div>
      </div>
      <div>
        <el-dialog title="碳排放转化系数添加" :visible.sync="addShow" :modal="false" top="15%" width="40%">
          <el-form :model="addExchangeSetting" ref="ruleForm" label-width="180px">
            <el-form-item label="分类名称" prop="objectCategory">
              <el-input v-model="addExchangeSetting.objectCategory" style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="碳排放转化系数" prop="exchangeCoefficient">
              <el-input v-model="addExchangeSetting.exchangeCoefficient" @keyup.native="inputJudge1(1)"
                style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="单位" prop="unit">
              <el-input v-model="addExchangeSetting.unit" style="width: 300px" @keyup.native="inputJudge2(1)"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="insertExchangeSetting">确认添加</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>
      </div>
      <div class="upload-image">
        <el-dialog title="碳排放转化系数修改" :visible.sync="updateShow" :modal="false" top="15%" width="40%">
          <el-form :model="updateExchangeSetting" label-width="180px">
            <el-form-item label="分类名称" prop="objectCategory">
              <el-input v-model="updateExchangeSetting.objectCategory" style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="碳排放转化系数" prop="exchangeCoefficient">
              <el-input v-model="updateExchangeSetting.exchangeCoefficient" @keyup.native="inputJudge1(2)"
                style="width: 300px"> </el-input>
            </el-form-item>
            <el-form-item label=" 单位" prop="unit">
              <el-input v-model="updateExchangeSetting.unit" @keyup.native="inputJudge2(2)"
                style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updateExchangeSettingById">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>
      </div>
    </div>
  </div>
</template>
<script>
import axios from "axios";
import { publicNetworkIpAndPort } from "../../api/globalVar";
import { getAllExchangeSetting, getExchangeSetting, addExchangeSetting, updateExchangeSetting, deleteExchangeSetting } from "../../api/exchangeSetting";
export default {
  data() {
    return {
      tableData: [],
      current: 1,
      size: 10,
      limit: 10,
      total: 0,
      pageshow: true,
      addShow: false,
      updateShow: false,

      addExchangeSetting: {},
      updateExchangeSetting: {},

      searchObjectCategory: '',
    }
  },
  mounted: function () {
    this.currentPage = 1
    this.getExchangeSettings()
  },
  activated() {
    this.getAllEmission()
  },

  methods: {
    inputJudge1(val) {
      if (val == 1 && (this.addExchangeSetting != null && this.addExchangeSetting.exchangeCoefficient != null)) {
        this.addExchangeSetting.exchangeCoefficient = this.addExchangeSetting.exchangeCoefficient.replace(/[^\.\d]/g, "");
      }
      else if (this.updateExchangeSetting != null && this.updateExchangeSetting.exchangeCoefficient != null) {
        this.updateExchangeSetting.exchangeCoefficient = this.updateExchangeSetting.exchangeCoefficient.replace(/[^\.\d]/g, "");
      }
    },
    inputJudge2(val) {
      // 允许：字母、数字、斜杠、常用单位符号（上标²³、下标₀₁₂₃₄₅₆₇₈₉等）
      // 使用负向字符类，只过滤掉不允许的字符，保留所有允许的字符
      // 允许的字符：a-zA-Z0-9/、上标²³¹⁰⁴⁵⁶⁷⁸⁹、下标₀₁₂₃₄₅₆₇₈₉、其他常用符号·×
      if (val == 1 && (this.addExchangeSetting != null && this.addExchangeSetting.unit != null)) {
        // 保留所有允许的字符：字母、数字、斜杠、上标、下标、常用符号
        this.addExchangeSetting.unit = this.addExchangeSetting.unit.replace(/[^a-zA-Z0-9\/²³¹⁰⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉·×]/g, '');
      }
      else if (this.updateExchangeSetting != null && this.updateExchangeSetting.unit != null) {
        // 保留所有允许的字符：字母、数字、斜杠、上标、下标、常用符号
        this.updateExchangeSetting.unit = this.updateExchangeSetting.unit.replace(/[^a-zA-Z0-9\/²³¹⁰⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉·×]/g, '');
      }
    },
    getAllEs() {
      this.searchObjectCategory = ""
      this.getExchangeSettings();
    },
    getExchangeSettings() {
      getAllExchangeSetting({ current: this.current, size: this.size, objectCategory: this.searchObjectCategory }).then(res => {
        this.tableData = res.data.records
        this.total = res.data.total;
      })
    },
    handleCurrentChange(val) {
      this.current = val
      this.getExchangeSettings()
    },
    showAddDialog() {
      this.addShow = true;
      this.addExchangeSetting = {}
      this.$nextTick(() => {
        setTimeout(() => {
          const firstInput = this.$el.querySelector('.el-dialog__body .el-input__inner');
          if (firstInput) {
            firstInput.focus();
          } else {
            const dialogBody = this.$el.querySelector('.el-dialog__body');
            if (dialogBody) {
              dialogBody.setAttribute('tabindex', '-1');
              dialogBody.focus();
            }
          }
        }, 200);
      });
    },
    showUpdateDialog(row) {
      this.updateShow = true;
      this.updateExchangeSetting = row
    },
    updateExchangeSettingById() {
      console.log(this.updateExchangeSetting);
      updateExchangeSetting(this.updateExchangeSetting).then(res => {
        if (res.data.code == 200) {
          this.$message.success("修改成功！")
          this.getExchangeSettings();
        }
        else {
          this.$message.error("输入内容有误！")
          this.getExchangeSettings();
        }
      }).catch(() => {
        this.$message.error("修改失败！")
      }).finally(() => {
        this.updateShow = false;
      });
    },
    insertExchangeSetting() {
      addExchangeSetting(this.addExchangeSetting).then(res => {
        if (res.data.code == 200) {
          this.$message.success("新增成功！")
          this.getExchangeSettings()
        }
        else {
          this.$message.error("输入内容有误！")
        }
      }).catch(() => {
        this.$message.error("新增失败！")
      }).finally(() => {
        this.addShow = false;
      });
    },
    deleteExchangeSettingById(row) {
      this.$confirm('此操作将永久删除该文件, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteExchangeSetting(row).then(res => {
          if (res.data.code == 200) {
            this.$message.success("删除成功！")
            this.getExchangeSettings()
          }
          else {
            this.$message.error("输入内容有误！")
          }
        }).catch(() => {
          this.$message.error("删除失败！")
        }).finally(() => { });
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消删除'
        });
      })
    },
    getFileType(file) {
      if (file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') {
        return true;
      } else {
        this.$message({
          message: "上传文件只能是xlsx格式!",
          type: "warning",
        });
        return false;
      }
    },
  }
}

</script>
<style>
/* 统一所有输入组件的高度 */
.el-input__inner {
  height: 32.5px;
}

/* 确保所有按钮高度与输入框一致 */
::v-deep .energyleft .el-button--small,
::v-deep .energyright .el-button--small {
  height: 32.5px;
  padding: 9px 15px;
  line-height: 1;
}

.cell {
  text-align: center;
}

.energyleft,
.energyright {
  display: inline-block;
}

.energyright {
  vertical-align: middle !important;
}

.el-table__body tr:hover>td {
  background-color: #e8f5e3 !important;
}

.form-item-inline {
  font-size: 13px;
}

.el-table__fixed {
  height: 100% !important;
}

.main-box-admin {
  width: 94%;
  margin: 0 auto;
  text-align: left;
  padding: 5px; /* 从3%减少到5px，减少页面顶部间距 */
}

.flex-box-header-new {
  margin-left: 20px;
  margin-right: 15px;
  margin-top: 15px;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: start;
}
</style>

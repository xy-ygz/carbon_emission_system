<template>
  <div class="main-box-admin">
    <el-breadcrumb separator="/" style="font-size: 18px; margin-bottom: 20px;">
      <el-breadcrumb-item :to="{ path: '/Tan/ManageCarbon' }">
        <i class="el-icon-s-home"></i> 数据输入管理首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>排放地点</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="forest-card" style="padding: 20px; margin-bottom: 20px;">
      <div class="flex-box-header-new">
        <div class="energyleft">
          <el-form>
            <el-tag style="font-size: 13px;">地点名称：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchPlaceName" placeholder="请输入地点名称" size="small" style="width:200px"></el-input>
            </el-form-item>
            <el-button class="form-item-inline" size="small" @click="getPlaceList" plain
              icon="el-icon-search">查询</el-button>
            <el-button class="form-item-inline" type="success" size="small" @click="getAllPlaceList" plain
              icon="el-icon-search">显示全部</el-button>
          </el-form>
        </div>
        <div style="margin-left: 50px;margin-bottom: 21px;">
          <el-form>
            <el-button class="form-item-inline" type="primary" size="small" @click="showAddDialog">新增地点</el-button>
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
          <el-table-column prop="name" label="地点名称" min-width="200" align="center" show-overflow-tooltip></el-table-column>
          <el-table-column prop="population" label="流动人数" min-width="150" align="center"></el-table-column>
          <el-table-column prop="area" label="占地面积(m²)" min-width="180" align="center"></el-table-column>
          <el-table-column label="操作" min-width="200" fixed="right" align="center">
            <template slot-scope="scope">
              <div class="updBtn" style="display: inline-block;">
                <el-button @click="showUpdateDialog(scope.row)" type="warning" plain size="small" icon="el-icon-edit">修改</el-button>
              </div>
              <div class="delBtn" style="display: inline-block;">
                <el-button @click="deletePlaceInfoById(scope.row)" type="danger" plain size="small"
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
        <el-dialog title="地点信息添加" :visible.sync="addShow" :modal="false" top="15%" width="40%">
          <el-form :model="addPlaceData" ref="ruleForm" label-width="180px">
            <el-form-item label="地点名称" prop="name">
              <el-input v-model="addPlaceData.name" style="width: 300px" placeholder="请输入地点名称"></el-input>
            </el-form-item>
            <el-form-item label="流动人数" prop="population">
              <el-input v-model="addPlaceData.population" style="width: 300px" placeholder="请输入流动人数" @keyup.native="inputJudgePopulation(1)"></el-input>
            </el-form-item>
            <el-form-item label="占地面积(m²)" prop="area">
              <el-input v-model="addPlaceData.area" style="width: 300px" placeholder="请输入占地面积" @keyup.native="inputJudgeArea(1)"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="insertPlaceData">确认添加</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>
      </div>
      <div class="upload-image">
        <el-dialog title="地点信息修改" :visible.sync="updateShow" :modal="false" top="15%" width="40%">
          <el-form :model="updatePlaceData" label-width="180px">
            <el-form-item label="地点名称" prop="name">
              <el-input v-model="updatePlaceData.name" style="width: 300px" placeholder="请输入地点名称"></el-input>
            </el-form-item>
            <el-form-item label="流动人数" prop="population">
              <el-input v-model="updatePlaceData.population" style="width: 300px" placeholder="请输入流动人数" @keyup.native="inputJudgePopulation(2)"></el-input>
            </el-form-item>
            <el-form-item label="占地面积(m²)" prop="area">
              <el-input v-model="updatePlaceData.area" style="width: 300px" placeholder="请输入占地面积" @keyup.native="inputJudgeArea(2)"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updatePlaceDataById">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>
      </div>
    </div>
  </div>
</template>
<script>
import { getAllPlaceInfo, addPlaceInfo, updatePlaceInfo, deletePlaceInfo } from "../../api/placeInfo";
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

      addPlaceData: {},
      updatePlaceData: {},

      searchPlaceName: '',
    }
  },
  mounted: function () {
    this.currentPage = 1
    this.getPlaceList()
  },
  activated() {
    this.getPlaceList()
  },

  methods: {
    inputJudgePopulation(val) {
      if (val == 1 && (this.addPlaceData != null && this.addPlaceData.population != null)) {
        this.addPlaceData.population = this.addPlaceData.population.toString().replace(/[^\d]/g, "");
      }
      else if (this.updatePlaceData != null && this.updatePlaceData.population != null) {
        this.updatePlaceData.population = this.updatePlaceData.population.toString().replace(/[^\d]/g, "");
      }
    },
    inputJudgeArea(val) {
      if (val == 1 && (this.addPlaceData != null && this.addPlaceData.area != null)) {
        this.addPlaceData.area = this.addPlaceData.area.toString().replace(/[^\.\d]/g, "");
      }
      else if (this.updatePlaceData != null && this.updatePlaceData.area != null) {
        this.updatePlaceData.area = this.updatePlaceData.area.toString().replace(/[^\.\d]/g, "");
      }
    },
    getAllPlaceList() {
      this.searchPlaceName = ""
      this.getPlaceList();
    },
    getPlaceList() {
      getAllPlaceInfo({ current: this.current, size: this.size, name: this.searchPlaceName }).then(res => {
        this.tableData = res.data.records || res.data;
        this.total = res.data.total || (res.data.length || 0);
      }).catch(() => {
        this.$message.error("获取地点列表失败！");
      });
    },
    handleCurrentChange(val) {
      this.current = val
      this.getPlaceList()
    },
    showAddDialog() {
      this.addShow = true;
      this.addPlaceData = {}
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
      this.updatePlaceData = { ...row }
    },
    updatePlaceDataById() {
      if (!this.updatePlaceData.name || this.updatePlaceData.population === null || this.updatePlaceData.population === '' || this.updatePlaceData.area === null || this.updatePlaceData.area === '') {
        this.$message.error("请填写完整的地点信息");
        return;
      }
      // 转换数据类型
      const placeData = {
        id: this.updatePlaceData.id,
        name: this.updatePlaceData.name,
        population: parseInt(this.updatePlaceData.population) || 0,
        area: parseFloat(this.updatePlaceData.area) || 0
      };
      updatePlaceInfo(placeData).then(res => {
        if (res.data.code == 200) {
          this.$message.success("修改成功！")
          this.getPlaceList();
        }
        else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message || "修改失败！")
        }
      }).catch(() => {
        this.$message.error("修改失败！")
      }).finally(() => {
        this.updateShow = false;
      });
    },
    insertPlaceData() {
      if (!this.addPlaceData.name || !this.addPlaceData.population || !this.addPlaceData.area) {
        this.$message.error("请填写完整的地点信息");
        return;
      }
      // 转换数据类型
      const placeData = {
        name: this.addPlaceData.name,
        population: parseInt(this.addPlaceData.population) || 0,
        area: parseFloat(this.addPlaceData.area) || 0
      };
      addPlaceInfo(placeData).then(res => {
        if (res.data.code == 200) {
          this.$message.success("新增成功！")
          this.getPlaceList()
        }
        else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message || "新增失败！")
        }
      }).catch(() => {
        this.$message.error("新增失败！")
      }).finally(() => {
        this.addShow = false;
      });
    },
    deletePlaceInfoById(row) {
      this.$confirm('此操作将永久删除该地点, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deletePlaceInfo({ placeInfoId: row.id }).then(res => {
          if (res.data.code == 200) {
            this.$message.success("删除成功！")
            this.getPlaceList()
          }
          else {
            // 优先显示description，如果没有则显示message
            this.$message.error(res.data.description || res.data.message || "删除失败！")
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
  }
}
</script>

<style scoped>
.cell {
  text-align: center;
}

.energyleft,
.energyright {
  display: inline-block;
}

.energyright {
  vertical-align: top !important;
  margin-bottom: 19px;
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
  margin: 20px auto 0;
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


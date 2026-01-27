<template>
  <div class="main-box-admin">
    <el-breadcrumb separator="/" style="font-size: 18px; margin-bottom: 20px;">
      <el-breadcrumb-item :to="{ path: '/Tan/ManageCarbon' }">
        <i class="el-icon-s-home"></i> 数据输入管理首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>碳排放记录</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="forest-card" style="padding: 20px; margin-bottom: 20px;">
      <div class="flex-box-header">
        <div class="energyleft">
          <el-form>
            <el-tag style="font-size: 13px;">输入名称：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="name" placeholder="请输入名称" size="small" style="width:200px"></el-input>
              <el-date-picker v-model="year" type="year" value-format='yyyy' placeholder="选择年" size="small">
              </el-date-picker>
              <!-- <el-input v-model="month" placeholder="选择月" size="small" style="width:200px"></el-input> -->
              <el-select v-model="month" placeholder="选择月" size="small">
                <el-option v-for="item in 12" :key="item" :label="item" :value="item">
                </el-option>
              </el-select>
            </el-form-item>
            <el-button class="form-item-inline" size="small" @click="getEmission" plain
              icon="el-icon-search">查询</el-button>
            <el-button class="form-item-inline" type="success" size="small" @click="getAllEmissionClean" plain
              icon="el-icon-search">显示全部</el-button>
          </el-form>
        </div>
        <div class="energyright">
          <el-form>
            <el-button class="form-item-inline" type="primary" size="small" @click="insertEmission">新增碳排放记录</el-button>
            <el-button class="form-item-inline" type="primary" size="small" @click="getexcelEmission"
              style="margin: 0;">导入碳排放记录</el-button>
          </el-form>
        </div>
      </div>
      <div class="table_container" style="width: 100%; overflow-x: auto; overflow-y: visible;">
        <el-table :data="tableData" style="width: 100%;font-size: 15px; " size="small" class="forest-table">
          <el-table-column label="序号" min-width="100" align="center">
            <template slot-scope="scope">
              {{ (scope.$index + 1) + (current - 1) * limit }}
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="150" align="center" show-overflow-tooltip></el-table-column>
          <el-table-column prop="category" label="分类" min-width="150" align="center" show-overflow-tooltip></el-table-column>
          <el-table-column prop="consumption" label="消耗量(份数)" min-width="140" align="center"></el-table-column>
          <el-table-column prop="purpose" label="用途" min-width="120" align="center" show-overflow-tooltip></el-table-column>
          <el-table-column prop="emissionType" label="排放类型" min-width="120" align="center">
            <template slot-scope="scope">
              {{ scope.row.emissionType === '0' ? '直接排放' : scope.row.emissionType === '1' ? '间接排放' : '其他排放' }}
            </template>
          </el-table-column>
          <!--          <el-table-column prop="emissionPurpose" label="排放用途"></el-table-column>-->
          <!--          <el-table-column prop="amount" label="碳排放量"></el-table-column>-->
          <el-table-column prop="place" label="排放地点" min-width="150" align="center" show-overflow-tooltip></el-table-column>
          <el-table-column prop="year" label="输入时间（年）" min-width="140" align="center"></el-table-column>
          <el-table-column prop="month" label="输入时间（月）" min-width="140" align="center"></el-table-column>
          <el-table-column label="操作" min-width="200" fixed="right" align="center">
            <template slot-scope="scope">
              <div class="updBtn" style="display: inline-block;"><el-button @click="updateEmission(scope.row)"
                  type="warning" plain size="small" icon="el-icon-edit">修改</el-button></div>
              <div class="delBtn" style="display: inline-block;"><el-button @click="deleteEmission(scope.row)"
                  type="danger" plain size="small" icon="el-icon-delete">删除</el-button></div>
            </template>
          </el-table-column>
        </el-table>
        <!-- 换页菜单 -->
        <div style="text-align:center; margin:20px;padding-right: 50px;">
          <el-pagination background @current-change="handleCurrentChange" :page-size="limit" :current-page.sync="current"
            layout="total, prev, pager, next" :total="total" v-if="pageshow">
          </el-pagination>
        </div>
      </div>
      <div class="upload-image">
        <el-dialog title="碳排放记录添加" :visible.sync="emissionInsertForm" :modal="false" width="40%">
          <el-form :model="newEmissionData" ref="ruleForm" label-width="180px">
            <el-form-item label="名称" prop="name">
              <el-input v-model="newEmissionData.name" style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="分类" prop="category">
              <el-select v-model="newEmissionData.category" placeholder="请选择">
                <el-option v-for="item in categoryList" :key="item.id" :label="item.objectCategory"
                  :value="item.objectCategory">
                </el-option>
              </el-select>

            </el-form-item>
            <el-form-item label="消耗量(份数)" prop="consumption">
              <el-input v-model="newEmissionData.consumption" @keyup.native="inputJudgeConsumption(1)" style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="用途" prop="purpose">
              <el-input v-model="newEmissionData.purpose" style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="排放类型">
              <el-select v-model="newEmissionData.emissionType" placeholder="请选择排放类型">
                <el-option label="直接排放" value="0"></el-option>
                <el-option label="间接排放" value="1"></el-option>
                <el-option label="其他排放" value="2"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="排放地点" prop="place">
              <el-select v-model="newEmissionData.place" placeholder="请选择">
                <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="输入时间（年）" prop="year">
              <el-date-picker v-model="newEmissionData.year" type="year" value-format='yyyy' placeholder="选择年">
              </el-date-picker>
            </el-form-item>
            <el-form-item label="输入时间（月）" prop="month">
              <el-select v-model="newEmissionData.month" placeholder="选择月">
                <el-option v-for="item in 12" :key="item" :label="item" :value="item">
                </el-option>
              </el-select>
              <!-- <el-input v-model="newEmissionData.month" style="width: 300px"></el-input> -->
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="insertEmissionData">确认添加</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>
      </div>
      <div class="upload-image">
        <el-dialog title="碳排放记录修改" :visible.sync="emissionUpdateForm" :modal="false" width="40%">
          <el-form :model="updateEmissionData" ref="ruleForm" label-width="180px">
            <el-form-item label="名称" prop="name">
              <el-input v-model="updateEmissionData.name" style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="分类" prop="category">
              <el-input v-model="updateEmissionData.category" style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="消耗量(份数)" prop="consumption">
              <el-input v-model="updateEmissionData.consumption" @keyup.native="inputJudgeConsumption(2)" style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="用途" prop="purpose">
              <el-input v-model="updateEmissionData.purpose" style="width: 300px"></el-input>
            </el-form-item>
            <el-form-item label="排放类型">
              <el-select v-model="updateEmissionData.emissionType" placeholder="请选择课程类型">
                <el-option label="直接排放" value="0"></el-option>
                <el-option label="间接排放" value="1"></el-option>
                <el-option label="其他排放" value="2"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="排放地点" prop="place">
              <el-select v-model="updateEmissionData.place" placeholder="请选择地点">
                <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="输入时间（年）" prop="year">
              <el-date-picker v-model="updateEmissionData.year" type="year" value-format='yyyy' placeholder="选择年">
              </el-date-picker>
            </el-form-item>
            <el-form-item label="输入时间（月）" prop="month">
              <!-- <el-input v-model="updateEmissionData.month" style="width: 300px"></el-input> -->
              <el-select v-model="updateEmissionData.month" placeholder="选择月">
                <el-option v-for="item in 12" :key="item" :label="item" :value="item">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="modifyEmissionData">立即修改</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>
      </div>
      <div class="upload-image" style="width: 300px">
        <el-dialog class="self_define" title="导入碳排放记录" :close-on-click-modal='false' :visible.sync="emissionAddForm" :modal="false"
          width="40%">
          <el-form :model="fileRes" ref="ruleForm" label-width="100px">
            <el-form-item label="" ref="uploadElement" prop="photoUrl" style="margin: 0 auto">
              <el-upload class="upload-demo" ref="upload" drag :action="UploadEnergyUrl()" multiple :auto-upload="false"
                       :on-success="handleSuccessEnergy" :on-error="uploadEnergyError" :before-upload="getFileType" :limit="5"
                       :file-list="fileList" name="file" :headers="uploadHeaders">
                <i class="el-icon-upload"></i>
                <div slot="tip" class="el-upload__tip" style="width: 100%; word-break:break-all;">
                  注意：一次只能上传一个.xlsx 文件，传入的xlsx文件第一行必须为为列表头：<p style="font-weight: bold;">(名称，分类，消耗量，用途，排放类型，地点，年份，月份)</p>
                </div>
                <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
              </el-upload>
            </el-form-item>
          </el-form>
          <div slot="footer" class="dialog-footer">
            <el-button type="primary" @click="addNewEmission">确定</el-button>
          </div>
        </el-dialog>
      </div>
    </div>
  </div>
</template>
<script>
import request from "../../utils/request";
import { getAllEmission, searchCarbonByName } from "../../api/carbonEmission";
import { getPlace } from "../../api/placeInfo";
import { getAllCategory } from "../../api/exchangeSetting";
export default {
  data() {
    return {
      categoryList: [],
      options: [],
      current: 1,
      size: 10,
      cur: 1,
      fileRes: {},
      year: '',
      newEmissionData: {},
      updateEmissionData: {},
      fileList: [],
      tableData: [],
      emissionInsertForm: false,
      name: '',
      month: '',
      emissionAddForm: false,
      emissionUpdateForm: false,
             limit: 10,
             total: 0,
             pageshow: true,
           }
         },
         computed: {
           // 上传文件的请求头，包含 JWT Token
           uploadHeaders() {
             const token = localStorage.getItem('token');
             return token ? { 'Authorization': 'Bearer ' + token } : {};
           }
         },
  mounted: function () {
    this.getBuildingName()
    this.getAllEmission()
    this.currentPage = 1
  },
  activated() {
    this.getAllEmission()
  },

  methods: {
    inputJudgeConsumption(val) {
      if (val == 1 && (this.newEmissionData != null && this.newEmissionData.consumption != null)) {
        this.newEmissionData.consumption = this.newEmissionData.consumption.replace(/[^\.\d]/g, "");
      }
      else if (this.updateEmissionData != null && this.updateEmissionData.consumption != null) {
        this.updateEmissionData.consumption = this.updateEmissionData.consumption.replace(/[^\.\d]/g, "");
      }
    },
    getCategeoryList() {
      getAllCategory().then(res => {
        // console.log(res.data);
        this.categoryList = res.data.data
      }).catch(() => {
        this.$message.error("获取分类失败！")
      }).finally(() => {
      });
    },
    //拿到数据库地点表中所有地点名字
    getBuildingName() {
      this.options = []; // 清空选项列表
      getPlace().then(res => {
        if (res.data.code == 200) {
          this.carbonInformation = res.data.data
          for (var i = 0; i < this.carbonInformation.length; i++) {
            //lable和value全给地点名，el-select的v-model只能接收value
            this.options.push({ label: this.carbonInformation[i].name, value: this.carbonInformation[i].name });
          }
        }
      })
    },
    getEmission() {
      this.current = 1;//cur_page 当前页
      this.getAllEmission();//获取数据
      this.pageshow = false;//让分页隐藏
      this.$nextTick(() => {//重新渲染分页
        this.pageshow = true;

      });
    },
    getAllEmissionClean() {
      this.name = '';
      this.year = '';
      this.month = '';
      this.current = 1;//cur_page 当前页
      this.getAllEmission();//获取数据
      this.pageshow = false;//让分页隐藏
      this.$nextTick(() => {//重新渲染分页
        this.pageshow = true;

      });
      // console.log("rhis.current", this.current)
    },
    updateEmission(row) {
      //debugger
      // console.log("row", row);
      this.updateEmissionData = row;
      //if(this.updateEmissionData.emissionYear==2023) {
      //  this.updateEmissionData.emissionYear = "2023";
      //}
      this.updateEmissionData.year = this.updateEmissionData.year.toString()
      // console.log(this.updateEmissionData.year)
      this.emissionUpdateForm = true;
    },
    modifyEmissionData() {
      // console.log("updateEmissionData",this.updateEmissionData)
      if (this.updateEmissionData["name"] === "" || this.updateEmissionData["category"] === '' || this.updateEmissionData["consumption"] === '' || this.updateEmissionData["emissionType"] === '') {
        this.$message.error("请将表单信息填写完整");
        return;
      }

      if (this.updateEmissionData.emissionType == "直接排放") {
        this.updateEmissionData.emissionType = "0";
      }
      if (this.updateEmissionData.emissionType == "间接排放") {
        this.updateEmissionData.emissionType = "1";
      }
      if (this.updateEmissionData.emissionType == "其他排放") {
        this.updateEmissionData.emissionType = "2";
      }
      // console.log("this.updateEmissionData.emissionType", this.updateEmissionData.emissionType)
      request.post("/api/carbonEmission/updateCarbonEmission", this.updateEmissionData).then((res) => {
        //debugger
        // console.log("修改能耗啦")
        // console.log("res.data", res.data)
        if (res.data.code == 200) {
          this.$message.success(res.data.data);
        } else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message);
        }
      }).catch(() => {

      }).finally(() => {
        this.getAllEmission();
        this.emissionUpdateForm = false;
        this.updateEmissionData = {};

      });

    },
    insertEmissionData() {
      if (this.newEmissionData["name"] === "" || this.newEmissionData["category"] === '' || this.newEmissionData["consumption"] === '' || this.newEmissionData["emissionType"] === '') {
        this.$message.error("请填写完整的能耗信息");
        return;
      } else {
        //this.$refs.upload.submit();
        this.handleEmission();
      }
    },

    handleEmission() {
      // console.log("this.newEmissionData", this.newEmissionData)
      request.post("/api/carbonEmission/addCarbonEmission", this.newEmissionData).then((res) => {
        //debugger
        // console.log("res.data", res.data)
        if (res.data.code == 200) {
          this.$message.success(res.data.data);
        } else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message);
        }
      }).catch(() => {

      }).finally(() => {
        this.getAllEmission();
        this.emissionInsertForm = false;
        this.newEmissionData = {};
      });//会把返回的结果放在.then里。
    },
    insertEmission() {
      this.emissionInsertForm = true;
      this.getCategeoryList();
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
    deleteEmission(row) {
      // console.log(row.id)
      var carbonEmissionId = row.id
      // console.log("carbonEmissionId", carbonEmissionId)
      this.$confirm("此操作永久删除当前信息，是否继续？", "提示", { type: "info" }).then(() => {
        request.get("/api/carbonEmission/deleteCarbonEmissionById", { params: { carbonEmissionId: carbonEmissionId } }).then((res) => {
          if (res.data.code === 200) {
            this.$message.success("删除成功")
            // 为了在删除最后一页的最后一条数据时能成功跳转回最后一页的上一页（pageSize=1时不生效）
            let totalPage = Math.ceil((this.total - 1) / this.size)
            let currentPage = this.current > totalPage ? totalPage : this.current
            this.current = currentPage < 1 ? 1 : currentPage
          } else {
            // 优先显示description，如果没有则显示message
            this.$message.error(res.data.description || res.data.message)
          }
        }).catch(() => {
        }).finally(() => {
          this.tableData.length = this.tableData.length - 1;
          if (this.tableData.length === 0) {
            this.cur = this.cur - 1;
            if (this.cur === 0) this.cur = 1;
          }
          this.getAllEmission();
        });
      }).catch(() => {
        this.$message.info("取消操作");
      });
    },
    handleCurrentChange(val) {
      this.current = val
      this.getAllEmission()
    },
    getFileType(file) {
      // console.log("filetype")
      // console.log("文件类型")
      // console.log(file.type)
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
    handleSuccessEnergy(res) {
      //debugger
      // console.log("zhaopian")
      // console.log(localStorage.getItem("userId"))
      // console.log("zhaopianxinxi")
      // console.log("res", res)
      if (res.code === 200) {
        this.$refs.upload.clearFiles();
        this.$message.success(res.data)
        this.emissionAddForm = false;
        this.fileRes = {};
        this.getAllEmission();
      } else {
        this.$refs.upload.clearFiles();
        // 优先显示description，如果没有则显示message
        this.$message.error(res.description || res.message);
        this.emissionAddForm = false;
        this.fileRes = {};
      }
    },
    uploadEnergyError() {
      this.$message.error("请选择.xlsx进行上传");
    },
    UploadEnergyUrl: function () {
      // el-upload 需要完整的 URL
      // Token 通过 :headers 属性传递（在 computed 中定义）
      const baseURL = process.env.NODE_ENV === 'production' 
        ? 'http://101.200.39.170:8080' 
        : 'http://localhost:8080';
      return baseURL + "/api/carbonEmission/excelCarbonEmission";
    },
    addNewEmission() {
      var files = this.$refs.upload.uploadFiles
      if (files.length == 0) {
        this.$message.error("请选择上传的文件");
        return;
      }
      this.$refs.upload.submit();
    },
    getAllEmission() {
      // console.log(" this.current", this.current)
      // debugger
      getAllEmission({ current: this.current, size: this.size, name: this.name, year: this.year, month: this.month }).then(res => {
        // debugger
        // console.log("能耗列表res")
        // console.log(res)
        this.tableData = res.data.records
        this.total = res.data.total;
        for (var i = 0; i < this.total; i++) {
          if (this.tableData[i] != null && this.tableData[i].emissionType == "0") {
            this.tableData[i].emissionType = "直接排放";
          }
          if (this.tableData[i] != null && this.tableData[i].emissionType == "1") {
            this.tableData[i].emissionType = "间接排放";
          }
          if (this.tableData[i] != null && this.tableData[i].emissionType == "2") {
            this.tableData[i].emissionType = "其他排放";
          }
        }

        this.tableData.emissionType = '直接排放'
        // console.log(res.data.total);
      })
    },
    getexcelEmission() {
      //debugger
      this.emissionAddForm = true
    },
  }
}

</script>
<style scoped>
/* 统一所有输入组件的高度 */
.el-input__inner {
  height: 32.5px;
}

/* 确保日期选择器高度一致 */
::v-deep .energyleft .el-date-picker .el-input__inner {
  height: 32.5px;
}

/* 确保下拉框高度一致 */
::v-deep .energyleft .el-select .el-input__inner {
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

.flex-box-header {
  margin-left: 20px;
  margin-right: 15px;
  margin-top: 15px;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap; /* 允许换行 */
  gap: 15px; /* 添加间距 */
}

.energyleft {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0; /* 允许收缩 */
}

.energyleft .el-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.energyleft .el-form-item {
  margin-bottom: 0;
  margin-right: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.energyleft .el-form-item .el-input,
.energyleft .el-form-item .el-date-picker,
.energyleft .el-form-item .el-select {
  flex-shrink: 0;
}

.energyright {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.energyright .el-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .flex-box-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
  
  .energyleft {
    width: 100%;
  }
  
  .energyright {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .flex-box-header {
    margin-left: 10px;
    margin-right: 10px;
  }
  
  .energyleft .el-form-item {
    width: 100%;
  }
  
  .energyleft .el-form-item .el-input,
  .energyleft .el-form-item .el-date-picker,
  .energyleft .el-form-item .el-select {
    width: 100%;
    max-width: 200px;
  }
}
</style>

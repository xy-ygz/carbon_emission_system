<template>
  <div class="carbon-carbon">
    <div class="main-box-carbon">
      <!-- 筛选条件区域 -->
      <div class="forest-card filter-section">
        <div class="forest-card-header">
          <i class="el-icon-search"></i>
          <span>查询条件</span>
        </div>
        <div class="filter-content">
          <el-form :inline="true" class="filter-form">
            <el-form-item label="选择年份：">
              <el-date-picker v-model="year" type="year" value-format='yyyy' placeholder="选择年" size="small" class="filter-input">
              </el-date-picker>
            </el-form-item>
            <el-form-item label="选择月份：">
              <el-select v-model="month" placeholder="选择月" size="small" class="filter-input">
                <el-option v-for="item in 12" :key="item" :label="`${item}月`" :value="item">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button size="small" @click="searchBuilding()" type="primary" icon="el-icon-search" class="forest-btn">查询</el-button>
            </el-form-item>
            <el-form-item label="选择地点：">
              <el-select v-model="value1" multiple @remove-tag="removeTag" placeholder="请选择地点" size="small" class="filter-input" style="width: 200px;">
                <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item"
                  @click.native="getBuildings(item)">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button size="small" @click="resetFilters()" icon="el-icon-refresh" class="reset-btn">重置</el-button>
            </el-form-item>
            <el-form-item>
              <el-button size="small" @click="exportData()" icon="el-icon-download" class="export-btn">导出数据</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 统计概览区域 -->
      <div class="summary-section">
        <div class="summary-grid">
          <div class="forest-card summary-card">
            <div class="summary-header">
              <i class="el-icon-office-building"></i>
              <span>审计地点数</span>
            </div>
            <div class="summary-value">{{ totalBuildings }}</div>
            <div class="summary-desc">栋</div>
          </div>
          <div class="forest-card summary-card">
            <div class="summary-header">
              <i class="el-icon-lightning"></i>
              <span>总电耗</span>
            </div>
            <div class="summary-value">{{ totalPowerConsumption | formatNumber }}</div>
            <div class="summary-desc">kWh</div>
          </div>
          <div class="forest-card summary-card">
            <div class="summary-header">
              <i class="el-icon-cloudy"></i>
              <span>总碳排放</span>
            </div>
            <div class="summary-value">{{ totalEmission | formatNumber }}</div>
            <div class="summary-desc">kg CO₂</div>
          </div>
          <div class="forest-card summary-card">
            <div class="summary-header">
              <i class="el-icon-price-tag"></i>
              <span>平均单位面积排放</span>
            </div>
            <div class="summary-value">{{ avgEmissionPerArea | formatNumber }}</div>
            <div class="summary-desc">kg/m²</div>
          </div>
        </div>
      </div>

      <!-- 数据展示区域 -->
      <div class="data-section">
        <div class="data-container">
          <!-- 数据表格 -->
          <div class="forest-card table-card">
            <div class="forest-card-header">
              <i class="el-icon-s-grid"></i>
              <span>详细审计数据</span>
              <div class="table-info">
                <span class="info-text">{{ carbonInformationIn.year }}年{{ carbonInformationIn.month ? carbonInformationIn.month + '月' : '' }}审计结果</span>
              </div>
            </div>
            <div class="table-container">
              <el-table :data="tableData" class="forest-table" size="small" stripe>
                <el-table-column prop="placeName" label="地点名称" min-width="150" align="center">
                  <template slot-scope="scope">
                    <div class="building-name">
                      <i class="el-icon-office-building"></i>
                      {{ scope.row.placeName }}
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="powerConsumption" label="电耗 (kWh)" min-width="120" align="center" sortable>
                  <template slot-scope="scope">
                    <span class="number-cell">{{ scope.row.powerConsumption | formatNumber }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="emissionAmount" label="碳排放量 (kg)" min-width="140" align="center" sortable>
                  <template slot-scope="scope">
                    <span class="number-cell">{{ scope.row.emissionAmount | formatNumber }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="placePerArea" label="单位面积排放 (kg/m²)" min-width="160" align="center" sortable>
                  <template slot-scope="scope">
                    <span class="number-cell">{{ scope.row.placePerArea | formatNumber }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="排放等级" min-width="100" align="center">
                  <template slot-scope="scope">
                    <el-tag :type="getEmissionLevel(scope.row.emissionAmount).type" size="small">
                      {{ getEmissionLevel(scope.row.emissionAmount).level }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="能效评级" min-width="100" align="center">
                  <template slot-scope="scope">
                    <el-tag :type="getEfficiencyLevel(scope.row.placePerArea).type" size="small">
                      {{ getEfficiencyLevel(scope.row.placePerArea).level }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <!-- 分页 -->
            <div class="pagination-container">
              <el-pagination
                background
                @current-change="handleCurrentChange"
                :page-size="limit"
                :current-page="currentPage"
                layout="total, prev, pager, next, jumper"
                :total="total"
                class="pagination">
              </el-pagination>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import MyEcharts from "../../components/Assessment";
import TanTotal from "../../components/TanTotal";
import { getCarbonLine, getCarbonTable } from "../../api/carbonEmission";
import axios from "axios";
import { publicNetworkIpAndPort } from "../../api/globalVar";
export default {
  components: { TanTotal, MyEcharts },
  name: "",
  filters: {
    formatNumber(value) {
      if (!value) return '0';
      return new Intl.NumberFormat('zh-CN').format(value);
    }
  },
  data() {
    return {
      showValue: true,
      options: [],
      value1: [],
      buidingSet: new Set(),
      limit: 10,
      currentPage: 1,
      total: 0,
      carbonInformationIn: {
        year: '',
        month: '',
      },
      xValue: [],
      yValue: [],
      xxValue: [],
      yyValue: [],
      charts1: "charts1",
      tableData: [],
      year: '',
      month: '',
      buill: '',
      buildingbui: '',
      // 统计数据
      totalBuildings: 0,
      totalPowerConsumption: 0,
      totalEmission: 0,
      avgEmissionPerArea: 0,
    };
  },
  mounted: function () {
    this.getCarbonTable()
    this.getCarbonBuiding()
    // 监听窗口大小变化，重新渲染图表
    window.addEventListener('resize', this.handleResize);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    resetFilters() {
      this.year = '';
      this.month = '';
      this.value1 = [];
      this.buidingSet.clear();
      this.currentPage = 1;
      this.getCarbonTable();
    },
    exportData() {
      if (this.tableData.length === 0) {
        this.$message.warning('没有数据可导出');
        return;
      }

      const csvContent = this.convertToCSV(this.tableData);
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', `能源审计数据_${this.carbonInformationIn.year}${this.carbonInformationIn.month ? '_' + this.carbonInformationIn.month + '月' : ''}.csv`);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    },
    convertToCSV(data) {
      if (data.length === 0) return '';

      const headers = ['地点名称', '电耗 (kWh)', '碳排放量 (kg)', '单位面积排放 (kg/m²)', '排放等级', '能效评级'];
      let csv = headers.join(',') + '\n';

      data.forEach(row => {
        const values = [
          row.placeName,
          row.powerConsumption,
          row.emissionAmount,
          row.placePerArea,
          this.getEmissionLevel(row.emissionAmount).level,
          this.getEfficiencyLevel(row.placePerArea).level
        ];
        csv += values.join(',') + '\n';
      });

      return csv;
    },
    getEmissionLevel(emission) {
      if (emission >= 10000) return { level: '高排放', type: 'danger' };
      if (emission >= 5000) return { level: '中高排放', type: 'warning' };
      if (emission >= 1000) return { level: '中等排放', type: '' };
      return { level: '低排放', type: 'success' };
    },
    getEfficiencyLevel(emissionPerArea) {
      if (emissionPerArea >= 50) return { level: '低效', type: 'danger' };
      if (emissionPerArea >= 25) return { level: '中效', type: 'warning' };
      if (emissionPerArea >= 10) return { level: '较高', type: '' };
      return { level: '高效', type: 'success' };
    },
    calculateStatistics() {
      if (!this.tableData || this.tableData.length === 0) {
        this.totalBuildings = 0;
        this.totalPowerConsumption = 0;
        this.totalEmission = 0;
        this.avgEmissionPerArea = 0;
        return;
      }

      this.totalBuildings = this.tableData.length;
      this.totalPowerConsumption = this.tableData.reduce((sum, item) => sum + (item.powerConsumption || 0), 0);
      this.totalEmission = this.tableData.reduce((sum, item) => sum + (item.emissionAmount || 0), 0);
      this.avgEmissionPerArea = this.totalEmission / this.totalBuildings;
    },
    handleResize() {
      // 窗口大小改变时的处理逻辑（如果需要的话）
    },
    removeTag(tag) {
      this.buidingSet.delete(tag.label)//从楼宇缓存中删除掉该tag
      // console.log("this.buidingSet:", this.buidingSet)
    },
    getCarbonBuiding() {
      getCarbonLine({ building: this.building }).then(res => {
        if (res.data.code == 200) {
          this.carbonInformation = res.data.data
          var buildingsName = new Set();
          for (var i = 0; i < this.carbonInformation.length; i++) {
            buildingsName.add(this.carbonInformation[i].building);
          }
          // console.log("buildingsName",buildingsName);
          let setTestValues = buildingsName.values()
          // console.log("setTestValues",setTestValues)
          //let temp1 = setTestValues.next().value;
          var bui = []
          var n = buildingsName.size
          // bui.push(temp1)
          for (var i = 0; i < n; i++) {
            let temp1 = setTestValues.next().value
            bui.push(temp1);
          }
          // console.log("bui",bui)
          this.buildingbui = bui.toString()
          // console.log("this.buildingbui",this.buildingbui)
          let k = 0;
          for (let item of buildingsName) {
            this.options.push({ label: item, value: k });
            k++;
          }
        }
      })
    },
    getBuildings(building) {
      // console.log("building.label", building.label)
      //如果当前lable已经被添加过了，说明点过一次了，这是第二次的"取消lable"点击
      if (this.buidingSet.has(building.label)) {
        this.buidingSet.delete(building.label)
        //第一次点击，说明是添加lable
      } else {
        this.buidingSet.add(building.label)
      }
      // console.log("this.buidingSet", this.buidingSet)
    },
    searchBuilding() {
      // console.log("this.month",this.month)
      // console.log("this.year",this.year)
      //当有年份没有月份的时候，月份消失
      if ((this.year != null && this.year != '') && this.month == '') {
        this.showValue = false;
      } else {
        this.showValue = true;
      }
      // console.log("this.showValue",this.showValue)
      let setTestValues = this.buidingSet.values()
      // console.log("setTestValues",setTestValues)
      //let temp1 = setTestValues.next().value;
      var bui = []
      var n = this.buidingSet.size
      // bui.push(temp1)
      for (var i = 0; i < n; i++) {
        let temp1 = setTestValues.next().value
        bui.push(temp1);
      }
      // console.log("bui",bui)
      if (bui != '') {
        this.building = bui.toString()
      }
      // console.log("building",this.building)
      ////debugger
      //if (this.buill == '') {
      //  this.searchYearTable()
      //}else {
      //debugger
      getCarbonTable({ year: this.year, month: this.month, buildings: this.building }).then(res => {
        //debugger
        if (res.data.code == 200) {
          // console.log("能耗列表res")
          // console.log(res)
          //this.options = []
          var xValueTemp = [];
          for (var i = 0; i < res.data.data.length; i++) {
            if (res.data.data[i].subPlaceEmissionVos && res.data.data[i].subPlaceEmissionVos.length > 0) {
              for (var j = 0; j < res.data.data[i].subPlaceEmissionVos.length; j++) {
                xValueTemp.push(res.data.data[i].subPlaceEmissionVos[j].placeName);
              }
            }
          }
          //for (var i = 0; i < xValueTemp.length; i++)
          //  if (xValueTemp[i] != xValueTemp[i + 1]) {
          //    this.options.push({label: xValueTemp[i], value: i});
          //  }
          if (res.data.data && res.data.data.length > 0 && res.data.data[0].subPlaceEmissionVos && res.data.data[0].subPlaceEmissionVos.length > 0) {
            this.tableData = res.data.data[0].subPlaceEmissionVos
            // 如果用户选择了年份或月份，显示用户选择的；如果用户没选择，显示后端返回的实际年份和月份
            this.carbonInformationIn.year = (this.year && this.year != '') ? this.year : res.data.data[0].emissionYear;
            this.carbonInformationIn.month = (this.month && this.month != '') ? this.month : res.data.data[0].month;
            this.total = res.data.data[0].subPlaceEmissionVos.length;
            this.calculateStatistics();
          } else {
            this.tableData = [];
            this.total = 0;
            this.calculateStatistics();
            if (res.data.data && res.data.data.length > 0) {
              // 如果用户选择了年份或月份，显示用户选择的；如果用户没选择，显示后端返回的实际年份和月份
              this.carbonInformationIn.year = (this.year && this.year != '') ? this.year : (res.data.data[0].emissionYear || '');
              this.carbonInformationIn.month = (this.month && this.month != '') ? this.month : (res.data.data[0].month || '');
            }
          }
          // console.log(res.data.total);
        } else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message);
        }
      }).catch((error) => {
        // 检查是否是40101错误（已在request.js中处理）
        if (!error.response || !error.response.data || error.response.data.code !== 40101) {
          console.error("获取数据失败:", error);
        }
        this.tableData = [];
        this.total = 0;
        this.calculateStatistics();
      }).finally(() => {
        bui = [];
        // console.log("finbui",bui)
        this.building = this.buildingbui
        // console.log("this.buildingbui",this.buildingbui)
        // console.log("this.building",this.building)
      });
      //}
    },
    getCarbonTable() {
      // 初始调用时不传year和month参数（让后端回退查找）
      getCarbonTable({ buildings: this.building }).then(res => {
        ////debugger
        if (res.data.code == 200) {
          // console.log("能耗列表res")
          // console.log(res)
          //this.options=[]
          var xValueTemp = [];
          if (res.data.data && res.data.data.length > 0 && res.data.data[0].subPlaceEmissionVos) {
            for (var j = 0; j < res.data.data[0].subPlaceEmissionVos.length; j++) {
              xValueTemp.push(res.data.data[0].subPlaceEmissionVos[j].placeName);
            }
          }
          // console.log("xValueTemp",xValueTemp)
          //for(var i=0;i<xValueTemp.length;i++)
          //  if(xValueTemp[i]!=xValueTemp[i+1]) {
          //    this.options.push({label: xValueTemp[i], value: i});
          //  }
          if (res.data.data && res.data.data.length > 0 && res.data.data[0].subPlaceEmissionVos && res.data.data[0].subPlaceEmissionVos.length > 0) {
            this.tableData = res.data.data[0].subPlaceEmissionVos
            // 如果用户选择了年份或月份，显示用户选择的；如果用户没选择，显示后端返回的实际年份和月份
            this.carbonInformationIn.year = (this.year && this.year != '') ? this.year : res.data.data[0].emissionYear;
            this.carbonInformationIn.month = (this.month && this.month != '') ? this.month : res.data.data[0].month;
            this.total = res.data.data[0].subPlaceEmissionVos.length;
            this.calculateStatistics();
          } else {
            this.tableData = [];
            this.total = 0;
            this.calculateStatistics();
            if (res.data.data && res.data.data.length > 0) {
              // 如果用户选择了年份或月份，显示用户选择的；如果用户没选择，显示后端返回的实际年份和月份
              this.carbonInformationIn.year = (this.year && this.year != '') ? this.year : (res.data.data[0].emissionYear || '');
              this.carbonInformationIn.month = (this.month && this.month != '') ? this.month : (res.data.data[0].month || '');
            }
          }
          // console.log(res.data.total);
        } else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message);
        }
      })
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.getCarbonTable();
    },
  },



}
</script>
<style scoped>
/* 整体布局 */
.carbon-carbon {
  width: 100%;
  background-color: var(--forest-bg-primary);
  min-height: calc(100vh - 100px);
}

.main-box-carbon {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  background-color: transparent;
}

/* 整体布局 */
.carbon-carbon {
  width: 100%;
  background-color: var(--forest-bg-primary);
  min-height: calc(100vh - 100px);
}

/* 统计概览区域 */
.summary-section {
  margin-bottom: 20px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.summary-card {
  background: var(--forest-bg-card);
  border: 1px solid var(--forest-border-light);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: all 0.3s ease;
  box-shadow: var(--forest-shadow-sm);
}

.summary-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--forest-shadow-md);
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--forest-secondary);
}

.summary-header i {
  font-size: 20px;
}

.summary-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--forest-primary);
  margin-bottom: 4px;
}

.summary-desc {
  font-size: 12px;
  color: var(--forest-text-secondary);
}

/* 筛选条件区域 */
.filter-section {
  margin-bottom: 20px;
}

.filter-content {
  padding: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: center;
}

.filter-input {
  width: 120px;
}

.reset-btn {
  background-color: var(--forest-text-disabled);
  border-color: var(--forest-text-disabled);
  color: white;
}

.reset-btn:hover {
  background-color: var(--forest-text-secondary);
  border-color: var(--forest-text-secondary);
}

.export-btn {
  background-color: var(--forest-success);
  border-color: var(--forest-success);
  color: white;
}

.export-btn:hover {
  background-color: #73d13d;
  border-color: #73d13d;
}

/* 数据展示区域 */
.data-section {
  margin-bottom: 20px;
}

.data-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}


/* 表格区域 */
.table-card {
  width: 100%;
  height: fit-content;
}

.table-info {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex: 1;
}

.info-text {
  font-size: 14px;
  color: var(--forest-text-secondary);
  background: var(--forest-bg-hover);
  padding: 4px 12px;
  border-radius: 16px;
}

.table-container {
  padding: 20px;
}

.building-name {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.building-name i {
  color: var(--forest-secondary);
}

.number-cell {
  font-family: 'Monaco', 'Consolas', monospace;
  font-weight: 500;
  text-align: right;
  padding-right: 10px;
}


/* 表格样式修复 */
.el-table.forest-table .el-table__header-wrapper {
  background: linear-gradient(135deg, #4a7c3a 0%, #2d5016 100%) !important;
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
}

.el-table.forest-table .el-table__header th {
  background: transparent !important;
  color: white !important;
  font-weight: 600 !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2) !important;
  padding: 12px 0 !important;
  text-align: center !important;
}

/* 标签样式优化 */
.el-tag {
  font-size: 12px;
  padding: 2px 6px;
}

/* 分页样式 */
.pagination-container {
  padding: 0 20px 20px;
  display: flex;
  justify-content: center;
}

.pagination {
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-input {
    width: 100%;
  }

}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .main-box-carbon {
    padding: 10px;
  }

  .filter-form {
    gap: 10px;
  }


  .table-container {
    max-height: 350px;
  }

  .data-container {
    gap: 15px;
  }
}

/* 滚动条样式 */
.table-container::-webkit-scrollbar {
  width: 6px;
}

.table-container::-webkit-scrollbar-track {
  background: var(--forest-bg-hover);
  border-radius: 3px;
}

.table-container::-webkit-scrollbar-thumb {
  background: var(--forest-light);
  border-radius: 3px;
}

.table-container::-webkit-scrollbar-thumb:hover {
  background: var(--forest-secondary);
}
</style>

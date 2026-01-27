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
                <el-option v-for="item in 12" :key="item" :label="item" :value="item">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="选择地点：">
              <el-select v-model="value1" multiple @remove-tag="removeTag" placeholder="请选择地点" size="small" class="filter-input">
                <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item"
                  @click.native="getBuildings(item)">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="查询类型：">
              <el-select v-model="area" placeholder="请选择" size="small" class="filter-input">
                <el-option label="建筑碳排放量" value="0"></el-option>
                <el-option label="建筑单位面积碳排量" value="1"></el-option>
              </el-select>
            </el-form-item>
            <!-- 按钮组 - 合并到一个表单项中 -->
            <el-form-item class="button-group">
              <el-button size="small" @click="searchArea()" type="primary" icon="el-icon-search" class="forest-btn">查询</el-button>
              <el-button size="small" @click="exportChart()" icon="el-icon-download">导出图表</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- KPI 统计卡片区域 -->
      <div class="kpi-section" v-if="showStats">
        <div class="kpi-grid">
          <div class="forest-card kpi-card">
            <div class="kpi-header">
              <i class="el-icon-office-building"></i>
              <span>对比地点数</span>
            </div>
            <div class="kpi-value">{{ buildingCount }}</div>
            <div class="kpi-desc">栋</div>
          </div>
          <div class="forest-card kpi-card">
            <div class="kpi-header">
              <i class="el-icon-trending-up"></i>
              <span>总碳排放量</span>
            </div>
            <div class="kpi-value">{{ totalEmission | formatNumber }} {{ area === '1' ? 'kgCO₂/m²' : 'kg' }}</div>
            <div class="kpi-desc">累计排放</div>
          </div>
          <div class="forest-card kpi-card">
            <div class="kpi-header">
              <i class="el-icon-price-tag"></i>
              <span>平均排放量</span>
            </div>
            <div class="kpi-value">{{ avgEmission | formatNumber }} {{ area === '1' ? 'kgCO₂/m²' : 'kg' }}</div>
            <div class="kpi-desc">每栋平均</div>
          </div>
          <div class="forest-card kpi-card">
            <div class="kpi-header">
              <i class="el-icon-warning"></i>
              <span>最高排放地点</span>
            </div>
            <div class="kpi-value">{{ maxEmissionBuilding }}</div>
            <div class="kpi-desc">{{ maxEmissionValue | formatNumber }} {{ area === '1' ? 'kgCO₂/m²' : 'kg' }}</div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="forest-card chart-section">
        <div class="forest-card-header">
          <i class="el-icon-s-data"></i>
          <span>建筑电力CO₂排放对比</span>
          <div class="chart-info">
            <span class="info-text">{{ carbonInformationIn.year }}年{{ carbonInformationIn.month ? carbonInformationIn.month + '月' : '' }}建筑电力碳排放对比情况</span>
          </div>
        </div>
        <div class="chart-container">
          <div id="contrastBarChart" class="chart-canvas"></div>
        </div>
        <div class="chart-legend" v-if="buildingName && buildingName.length > 0">
          <div class="legend-items">
            <div v-for="(building, index) in buildingName.slice(0, 10)" :key="index" class="legend-item">
              <span class="legend-color" :style="{ backgroundColor: getColor(index) }"></span>
              <span class="legend-text">{{ building }}</span>
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
import { getCarbonBuilding, getCarbonLine } from "../../api/carbonEmission";
import axios from "axios";
import { publicNetworkIpAndPort } from "../../api/globalVar";
export default {
  components: { TanTotal, MyEcharts },
  name: "",
  filters: {
    formatNumber(value) {
      if (!value && value !== 0) return '0';
      return new Intl.NumberFormat('zh-CN').format(value);
    }
  },
  data() {
    return {
      title: '碳排放量(kg)',
      showValue: true,
      showStats: false,
      options: [],
      buidingSet: new Set(),
      value1: [],
      carbonInformationIn: {
        year: '',
        month: '',
      },
      xValue: [],
      yValue: [],
      xxValue: [],
      yyValue: [],
      year: '',
      month: '',
      buill: '',
      buildingName: [],
      buildingbui: '',
      area: '0', // 默认值为"建筑碳排放量"
      // KPI统计数据
      buildingCount: 0,
      totalEmission: 0,
      avgEmission: 0,
      maxEmissionBuilding: '',
      maxEmissionValue: 0,
    };
  },
  mounted: function () {
    this.getCarbonBar()
    this.getBuilding()
  },
  beforeDestroy() {
    if (this.myChart4) {
      this.myChart4.dispose();
      this.myChart4 = null;
    }
  },
  methods: {
    getColor(index) {
      const colorPalette = [
        '#4a7c3a', '#6b9f5a', '#8fc47e', '#52c41a', '#73d13d',
        '#389e0d', '#237804', '#135200', '#92c5de', '#0570b0'
      ];
      return colorPalette[index % colorPalette.length];
    },
    calculateStats() {
      if (!this.xxValue || !this.yyValue || this.xxValue.length === 0) {
        this.showStats = false;
        return;
      }

      this.showStats = true;
      this.buildingCount = this.xxValue.length;

      // 计算总排放量
      this.totalEmission = this.yyValue.reduce((sum, value) => sum + (parseFloat(value) || 0), 0);

      // 计算平均排放量
      this.avgEmission = this.buildingCount > 0 ? this.totalEmission / this.buildingCount : 0;

      // 找到最高排放的楼宇
      let maxIndex = 0;
      let maxValue = 0;
      this.yyValue.forEach((value, index) => {
        const numValue = parseFloat(value) || 0;
        if (numValue > maxValue) {
          maxValue = numValue;
          maxIndex = index;
        }
      });

      this.maxEmissionBuilding = this.xxValue[maxIndex] || '';
      this.maxEmissionValue = maxValue;
    },
    exportChart() {
      if (this.myChart4) {
        const link = document.createElement('a');
        const fileName = `建筑电力碳排放对比图_${this.carbonInformationIn.year || '最新'}${this.carbonInformationIn.month ? this.carbonInformationIn.month + '月' : ''}.png`;
        link.download = fileName;
        link.href = this.myChart4.getDataURL();
        link.click();
      } else {
        this.$message.warning('图表还未加载完成，请稍后再试');
      }
    },
    removeTag(tag) {
      this.buidingSet.delete(tag.label)//从楼宇缓存中删除掉该tag
      // console.log("this.buidingSet:", this.buidingSet)
    },
    getBuilding() {
      getCarbonLine({ building: this.building }).then(res => {
        if (res.data.code == 200) {
          this.carbonInformation = res.data.data
          var buildingsName = new Set();
          for (var i = 0; i < this.carbonInformation.length; i++) {
            buildingsName.add(this.carbonInformation[i].building);
          }
          // console.log("buildingsName", buildingsName);
          let setTestValues = buildingsName.values()
          // console.log("setTestValues", setTestValues)
          //let temp1 = setTestValues.next().value;
          var bui = []
          var n = buildingsName.size
          // bui.push(temp1)
          for (var i = 0; i < n; i++) {
            let temp1 = setTestValues.next().value
            bui.push(temp1);
          }
          // console.log("bui", bui)
          this.buildingbui = bui.toString()
          // console.log("this.buildingbui", this.buildingbui)
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
    searchArea() {
      //当有年份没有月份的时候，月份消失
      if ((this.year != null && this.year != '') && this.month == '') {
        this.showValue = false;
      } else {
        this.showValue = true;
      }
      // console.log("this.area", this.area)
      let a = false;

      if (this.area == 1) {
        this.title = '单位面积碳排放量(kgCO₂/m²)';
        a = true;
      } else {
        this.title = '碳排放量(kg)';
      }
      // console.log("this.title.length:", this.title.length)
      // console.log("a", a)
      let setTestValues = this.buidingSet.values()
      // console.log("setTestValues", setTestValues)
      //let temp1 = setTestValues.next().value;
      var bui = []
      var n = this.buidingSet.size
      // bui.push(temp1)
      for (var i = 0; i < n; i++) {
        let temp1 = setTestValues.next().value
        bui.push(temp1);
      }
      // console.log("bui", bui)
      if (bui != '') {
        this.building = bui.toString()
      }
      // console.log("building", this.building)
      // console.log("this.buill", this.buill)
      // console.log("this.year", this.year)
      // console.log("this.month", this.month)
      // console.log("this.area", this.area)
      //debugger
      // 如果用户选择了年份或月份，传对应参数；如果没选择，不传参数（让后端回退查找）
      const params = { buildings: this.building, area: a };
      if (this.year && this.year != '') {
        params.year = this.year;
      }
      if (this.month && this.month != '') {
        params.month = this.month;
      }
      getCarbonBuilding(params).then(res => {
        //debugger
        if (res.data.code == 200) {
          // console.log(res.data)
          // console.log(res.data.data.length)
          // 如果用户选择了年份或月份，显示用户选择的；如果用户没选择，显示后端返回的实际年份和月份
          if (res.data.data && res.data.data.length > 0) {
            this.carbonInformationIn = res.data.data[0];
            // 如果用户选择了年份或月份，优先使用用户的选择
            if (this.year && this.year != '') {
              this.carbonInformationIn.year = this.year;
            }
            if (this.month && this.month != '') {
              this.carbonInformationIn.month = this.month;
            }
          }
          // console.log("this.carbonInformation", this.carbonInformationIn)
          // console.log("this.carbonInformation.year", this.carbonInformationIn.year)
          var xValueTemp = [];
          var yValueTemp = [];
          for (var i = 0; i < res.data.data.length; i++) {
            if (res.data.data[i].subCarbonBuildingBarVos && res.data.data[i].subCarbonBuildingBarVos.length > 0) {
              for (var j = 0; j < res.data.data[i].subCarbonBuildingBarVos.length; j++) {
                xValueTemp.push(res.data.data[i].subCarbonBuildingBarVos[j].building);
                yValueTemp.push(res.data.data[i].subCarbonBuildingBarVos[j].amount);
              }
            }
          }
          this.xxValue = xValueTemp;
          this.yyValue = yValueTemp;
          this.buildingName = xValueTemp; // 保存楼宇名称用于图例
          //this.$refs.voteGraph.drawLine(xValueTemp, yValueTemp);
          // console.log(this.xxValue)
          // console.log(this.yyValue)
          this.calculateStats(); // 计算统计数据
          const chartDom = document.getElementById('contrastBarChart');
          if (chartDom) {
            this.myChart4 = this.$echarts.init(chartDom)
            this.myChart4.setOption({
            grid: {
              left: '2%',
              right: '3%',
              bottom: '1%',
              top: '10%',
              containLabel: true
            },
            tooltip: {
              trigger: 'item',
              formatter: (params) => {
                // // console.log("params",params);
                //当this.title == 碳排放量(kg)时，悬浮单位为(kg)，当== 单位面积碳排放量(kgCO₂/m²)时，单位为(kgCO₂/m²)
                let y = this.title.length == 8 ? this.title.substring(4) : this.title.substring(8);
                let t = this.title.length == 8 ? this.title.substring(0, 4) : this.title.substring(0, 8);
                // return params.seriesName+'<br/>' + params.marker + params.name + ':' +params.value + y +'<br/>';
                return t + '<br/>' + params.marker + params.name + ':' + parseFloat(params.value).toFixed(2) + y + '<br/>';
              }
            },
            legend: {
              show: false  // 不显示内置图例，我们使用外部图例
            },
            xAxis: {
              type: 'category',
              data: this.xxValue,
              axisLine: {
                lineStyle: {
                  color: '#c8e0c0'
                }
              },
              axisLabel: {
                color: '#4a7c3a',
                fontSize: 11,
                margin: 12
              }
            },
            yAxis: {
              type: 'value',
              name: this.area === '1' ? 'kgCO₂/m²' : 'kg',
              nameTextStyle: {
                color: '#4a7c3a'
              },
              axisLine: {
                lineStyle: {
                  color: '#c8e0c0'
                }
              },
              axisLabel: {
                color: '#4a7c3a',
                fontSize: 11,
                margin: 2
              }
            },
            series: [{
              name: '碳排放量',
              type: 'bar',
              barWidth: 30,
              data: this.yyValue,
              itemStyle: {
                normal: {
                  color: function (params) {
                    //首先定义一个数组
                    var colorList = [
                      '#C33531', '#EFE42A', '#64BD3D', '#EE9201', '#29AAE3',
                      '#B74AE5', '#0AAF9F', '#E89589'
                    ];
                    return colorList[params.dataIndex]
                  },
                  //以下为是否显示
                  label: {
                    show: false
                  }
                }
              }
            }]
          })
          }
        } else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message);
        }
      }).catch(() => {
      }).finally(() => {
        bui = [];
        // console.log("finbui", bui)
        this.building = this.buildingbui
        // console.log("this.buildingbui", this.buildingbui)
        // console.log("this.building", this.building)
      });

    },
    getCarbonBar() {
      //debugger
      // 初始调用时不传year和month参数（让后端回退查找）
      getCarbonBuilding({ buildings: this.building }).then(res => {
        //debugger
        if (res.data.code == 200) {
          // console.log(res.data)
          // console.log(res.data.data.length)
          //this.carbonInformation=res.data.data[0]
          // 如果用户选择了年份或月份，显示用户选择的；如果用户没选择，显示后端返回的实际年份和月份
          if (res.data.data && res.data.data.length > 0) {
            const backendYear = res.data.data[0].year;
            const backendMonth = res.data.data[0].month;
            
            // 回显年份和月份到选择器
            if (!this.year || this.year === '') {
              this.year = backendYear ? backendYear.toString() : '';
            }
            if (!this.month || this.month === '') {
              this.month = backendMonth || '';
            }
            
            this.carbonInformationIn.year = (this.year && this.year != '') ? this.year : backendYear;
            this.carbonInformationIn.month = (this.month && this.month != '') ? this.month : backendMonth;
          }
          // console.log("this.carbonInformation.year", this.carbonInformationIn.year)
          // console.log("this.carbonInformation.month", this.carbonInformationIn.month)
          var xValueTemp = [];
          var yValueTemp = [];
          for (var i = 0; i < res.data.data.length; i++) {
            if (res.data.data[i].subCarbonBuildingBarVos && res.data.data[i].subCarbonBuildingBarVos.length > 0) {
              for (var j = 0; j < res.data.data[i].subCarbonBuildingBarVos.length; j++) {
                xValueTemp.push(res.data.data[i].subCarbonBuildingBarVos[j].building);
                yValueTemp.push(res.data.data[i].subCarbonBuildingBarVos[j].amount);
              }
            }
          }
          this.xxValue = xValueTemp;
          this.yyValue = yValueTemp;
          this.buildingName = xValueTemp; // 保存楼宇名称用于图例
          //this.$refs.voteGraph.drawLine(xValueTemp, yValueTemp);
          // console.log(this.xxValue)
          // console.log(this.yyValue)
          this.calculateStats(); // 计算统计数据
          const chartDom = document.getElementById('contrastBarChart');
          if (chartDom) {
            this.myChart4 = this.$echarts.init(chartDom)
            this.myChart4.setOption({
            grid: {
              left: '2%',
              right: '3%',
              bottom: '1%',
              top: '10%',
              containLabel: true
            },
            tooltip: {
              trigger: 'item',
              formatter: (params) => {
                // // console.log("params",params);
                //当this.title == 碳排放量(kg)时，悬浮单位为(kg)，当== 单位面积碳排放量(kgCO₂/m²)时，单位为(kgCO₂/m²)
                let y = this.title.length == 8 ? this.title.substring(4) : this.title.substring(8);
                let t = this.title.length == 8 ? this.title.substring(0, 4) : this.title.substring(0, 8);
                // return params.seriesName+'<br/>' + params.marker + params.name + ':' +params.value + y +'<br/>';
                return t + '<br/>' + params.marker + params.name + ':' + parseFloat(params.value).toFixed(2) + y + '<br/>';
              }
            },
            legend: {
              show: false  // 不显示内置图例，我们使用外部图例
            },
            xAxis: {
              type: 'category',
              data: this.xxValue,
              axisLine: {
                lineStyle: {
                  color: '#c8e0c0'
                }
              },
              axisLabel: {
                color: '#4a7c3a',
                fontSize: 11,
                margin: 12
              }
            },
            yAxis: {
              type: 'value',
              name: this.area === '1' ? 'kgCO₂/m²' : 'kg',
              nameTextStyle: {
                color: '#4a7c3a'
              },
              axisLine: {
                lineStyle: {
                  color: '#c8e0c0'
                }
              },
              axisLabel: {
                color: '#4a7c3a',
                fontSize: 11,
                margin: 2
              }
            },
            series: [{
              name: '碳排放量',
              type: 'bar',
              barWidth: 30,
              data: this.yyValue,
              itemStyle: {
                normal: {
                  color: function (params) {
                    //首先定义一个数组
                    var colorList = [
                      '#C33531', '#EFE42A', '#64BD3D', '#EE9201', '#29AAE3',
                      '#B74AE5', '#0AAF9F', '#E89589'
                    ];
                    return colorList[params.dataIndex]
                  },
                  //以下为是否显示
                  label: {
                    show: false
                  }
                }
              }
            }]
          })
          }
        } else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message);
        }
      }).catch(() => {
      }).finally(() => {
      });

    }

  },



}
</script>
<style scoped>
.el-input__inner {
  height: 32.5px;
}

.flex-box-headerll {
  margin-left: 20px;
  margin-right: 15px;
  margin-top: 15px;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.buildingleft,
.buildingright {
  display: inline-block;
}

/* KPI 统计卡片区域 */
.kpi-section {
  margin-bottom: 20px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.kpi-card {
  background: var(--forest-bg-card);
  border-radius: 12px;
  padding: 20px;
  box-shadow: var(--forest-shadow-sm);
  border: 1px solid var(--forest-border-light);
  transition: all 0.3s ease;
}

.kpi-card:hover {
  box-shadow: var(--forest-shadow-md);
  transform: translateY(-2px);
}

.kpi-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--forest-secondary);
}

.kpi-header i {
  font-size: 18px;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--forest-primary);
  margin-bottom: 4px;
}

.kpi-desc {
  font-size: 12px;
  color: var(--forest-text-secondary);
  margin-top: 4px;
}

/* 筛选条件区域 */
.filter-section {
  margin-bottom: 20px;
}

.filter-content {
  padding: 15px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  align-items: center;
  justify-content: flex-start;
  margin: 0;
}

/* 表单项容器样式 - 减小上下间距 */
::v-deep .el-form-item {
  margin-bottom: 0 !important;
  margin-right: 0 !important;
  padding: 0;
  height: auto;
}

.filter-input {
  width: 110px;
  min-width: 100px;
}

.forest-btn {
  background: linear-gradient(135deg, #4a7c3a 0%, #2d5016 100%);
  border-color: #4a7c3a;
  color: white;
}

.forest-btn:hover {
  background: linear-gradient(135deg, #6b9f5a 0%, #4a7c3a 100%);
  border-color: #6b9f5a;
}

/* 按钮组样式 - 确保按钮在一行内排列 */
.button-group {
  display: flex;
  gap: 15px; /* 保持原有的间距 */
  align-items: center;
  margin-left: 0; /* 向左对齐 */
  padding: 0;
  white-space: nowrap; /* 防止换行 */
}

/* 按钮组内部的表单项样式 */
.button-group ::v-deep .el-form-item__content {
  padding: 0;
  margin: 0;
  display: flex;
  gap: 15px; /* 保持原有的间距 */
  white-space: nowrap; /* 防止换行 */
}

/* 单个按钮样式 - 保持原有的间距 */
.button-group .el-button {
  margin: 0 !important;
  height: 32px;
  padding: 0 15px; /* 保持原有的内边距 */
  font-size: 14px;
}

/* 图表区域 */
.chart-section {
  margin-bottom: 20px;
}

.chart-info {
  flex: 1;
  text-align: right;
}

.info-text {
  font-size: 14px;
  color: var(--forest-text-secondary);
}

.chart-container {
  padding: 10px 20px 5px 20px;
  height: 380px;
  width: 100%;
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

/* 图表图例 */
.chart-legend {
  padding: 0 10px 10px;
  border-top: 1px solid var(--forest-border-light);
  margin-top: 10px;
}

.legend-items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-text {
  font-size: 12px;
  color: var(--forest-text-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .legend-items {
    flex-direction: column;
    gap: 6px;
  }

  .chart-container {
    height: 320px;
  }

  .filter-form {
    gap: 8px;
  }
}

.carbon-carbon {
  width: 100%;
  background-color: var(--forest-bg-primary);
  min-height: auto;
}

.main-box-carbon {
  max-width: 1400px;
  margin: 0 auto;
  /* padding: 20px; */
  background-color: transparent;
}
</style>

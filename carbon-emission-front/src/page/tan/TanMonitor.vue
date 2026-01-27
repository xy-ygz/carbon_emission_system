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
              <el-button size="small" @click="searchBuilding()" type="primary" icon="el-icon-search" class="forest-btn">查询</el-button>
              <el-button size="small" @click="resetFilters()" icon="el-icon-refresh" class="reset-btn">重置</el-button>
              <el-button size="small" @click="exportChart()" icon="el-icon-download">导出图表</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- KPI 指标卡片区域 -->
      <div class="kpi-section" v-if="showValue">
        <div class="kpi-grid">
          <div class="forest-card kpi-card">
            <div class="kpi-header">
              <i class="el-icon-s-data"></i>
              <span>年度总排放</span>
            </div>
            <div class="kpi-value">{{ totalEmission | formatNumber }} kg</div>
            <div class="kpi-trend" :class="emissionTrend">
              <i :class="trendIcon"></i>
              <span>{{ trendText }}</span>
            </div>
          </div>
          <div class="forest-card kpi-card">
            <div class="kpi-header">
              <i class="el-icon-office-building"></i>
              <span>监测地点数</span>
            </div>
            <div class="kpi-value">{{ buildingCount }}</div>
            <div class="kpi-desc">栋</div>
          </div>
          <div class="forest-card kpi-card">
            <div class="kpi-header">
              <i class="el-icon-trending-up"></i>
              <span>平均月排放</span>
            </div>
            <div class="kpi-value">{{ avgMonthlyEmission | formatNumber }} kg</div>
            <div class="kpi-desc">每月平均</div>
          </div>
          <div class="forest-card kpi-card">
            <div class="kpi-header">
              <i class="el-icon-warning"></i>
              <span>峰值月份</span>
            </div>
            <div class="kpi-value">{{ peakMonth }}</div>
            <div class="kpi-desc">排放最高月</div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="forest-card chart-section" v-show="true">
        <div class="forest-card-header">
          <i class="el-icon-s-data"></i>
          <span>{{ showValue ? schoolInformation.year + '年' : '最新' }} 单体建筑CO₂排放监测</span>
          <div class="chart-controls">
            <el-button size="mini" @click="toggleChartType()" :type="chartType === 'line' ? 'primary' : 'default'">
              <i :class="chartType === 'line' ? 'el-icon-s-data' : 'el-icon-s-grid'"></i>
              {{ chartType === 'line' ? '折线图' : '面积图' }}
            </el-button>
          </div>
        </div>
        <div class="chart-container">
          <div id="monitorBarChart" class="chart-canvas"></div>
        </div>
        <div class="chart-legend" v-if="carbonInformation.length > 0">
          <div class="legend-items">
            <div v-for="(building, index) in legendData" :key="index" class="legend-item">
              <span class="legend-color" :style="{ backgroundColor: building.color }"></span>
              <span class="legend-text">{{ building.name }}</span>
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
import { getCarbonLine } from "../../api/carbonEmission";
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
      elementHeight: 30,
      title: '',
      showValue: false,
      buidingSet: new Set(),
      buildingTable: '',
      options: [],
      value1: [],
      myChart4: '',
      chartType: 'line', // 'line' 或 'area'
      schoolInformation: {
        year: '',
      },
      carbonInformation: [],
      buildingName: [],
      year: '',
      building: '',
      emissionAmount: '',
      month: '',
      buildingNameName: "",
      buildingClass: [],
      mylabel: '',
      buill: '',
      area: '0', // 默认值为"建筑碳排放量"
      buildingbui: '',
      // KPI 数据
      totalEmission: 0,
      buildingCount: 0,
      avgMonthlyEmission: 0,
      peakMonth: '-',
      emissionTrend: '',
      trendIcon: '',
      trendText: '',
      // 图表图例数据
      legendData: [],
      // 表格数据
      tableData: [],
    };
  },
  mounted: function () {
    this.getCarbonBar();
    this.getBuilding();
    // 监听窗口大小变化，重新渲染图表
    window.addEventListener('resize', this.handleResize);

    // 添加延迟渲染，确保DOM完全加载
    setTimeout(() => {
      if (this.carbonInformation && this.carbonInformation.length > 0) {
        this.renderChart();
      }
    }, 1000);
  },
  updated() {
    // 当组件更新时，如果有数据变化，重新渲染图表
    if (this.carbonInformation && this.carbonInformation.length > 0 && !this.myChart4) {
      this.$nextTick(() => {
        this.renderChart();
      });
    }
  },
  beforeDestroy() {
    // 清理图表实例
    if (this.myChart4) {
      this.myChart4.dispose();
    }
    window.removeEventListener('resize', this.handleResize);
  },
  //监听选择的年份，当年份为空的时候，左上角不显示'年'
  watch: {
    year: {
      handler(newVal, oldVal) {
        if (newVal == null) {
          this.showValue = false;
          this.year = '';
        }
        // // console.log('newVal', newVal);
        // // console.log('oldVal', oldVal);
      }
    }

  },
  methods: {
    handleResize() {
      if (this.myChart4) {
        this.myChart4.resize();
      }
    },
    resetFilters() {
      this.year = '';
      this.value1 = [];
      this.buidingSet.clear();
      this.area = '0'; // 重置为默认值"建筑碳排放量"
      this.showValue = false;
      this.getBuilding();
      this.getCarbonBar();
    },
    exportChart() {
      if (this.myChart4) {
        const link = document.createElement('a');
        link.download = `单体建筑排放监测图_${this.schoolInformation.year || '最新'}.png`;
        link.href = this.myChart4.getDataURL();
        link.click();
      }
    },
    toggleChartType() {
      this.chartType = this.chartType === 'line' ? 'area' : 'line';
      this.renderChart();
    },
    calculateKPIs() {
      if (!this.carbonInformation || this.carbonInformation.length === 0) return;

      let total = 0;
      let buildingCount = this.carbonInformation.length;
      let monthlyTotals = new Array(12).fill(0);

      this.carbonInformation.forEach(building => {
        if (building.emissionMonthAmount) {
          building.emissionMonthAmount.forEach(monthData => {
            total += monthData.emissionAmount || 0;
            monthlyTotals[monthData.month - 1] += monthData.emissionAmount || 0;
          });
        }
      });

      this.totalEmission = total;
      this.buildingCount = buildingCount;
      this.avgMonthlyEmission = total / 12;

      // 找到峰值月份
      let maxEmission = Math.max(...monthlyTotals);
      this.peakMonth = monthlyTotals.indexOf(maxEmission) + 1 + '月';

      // 计算趋势（这里简化处理，实际可以计算环比增长率）
      this.emissionTrend = 'stable';
      this.trendIcon = 'el-icon-minus';
      this.trendText = '持平';
    },
    prepareTableData() {
      if (!this.carbonInformation || this.carbonInformation.length === 0) {
        this.tableData = [];
        return;
      }

      this.tableData = this.carbonInformation.map(building => {
        let row = { buildingName: building.building };
        let total = 0;
        let months = ['jan', 'feb', 'mar', 'apr', 'may', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec'];

        months.forEach((month, index) => {
          let monthData = building.emissionMonthAmount && building.emissionMonthAmount.find(m => m.month === index + 1);
          row[month] = monthData ? monthData.emissionAmount : 0;
          total += row[month];
        });

        row.total = total;
        return row;
      });
    },
    renderChart() {
      if (!this.carbonInformation || this.carbonInformation.length === 0) {
        return;
      }

      // 使用$nextTick确保DOM完全渲染
      this.$nextTick(() => {
        // 准备图例数据
        const colorPalette = [
          '#4a7c3a', '#6b9f5a', '#8fc47e', '#52c41a', '#73d13d',
          '#389e0d', '#237804', '#135200', '#92c5de', '#0570b0'
        ];

        this.legendData = this.carbonInformation.map((building, index) => ({
          name: building.building,
          color: colorPalette[index % colorPalette.length]
        }));

        // 准备系列数据
        let mySeries = [];
        this.carbonInformation.forEach((building, buildingIndex) => {
          let seriesObj = {
            name: building.building,
            type: this.chartType,
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              width: 2,
              color: colorPalette[buildingIndex % colorPalette.length]
            },
            itemStyle: {
              color: colorPalette[buildingIndex % colorPalette.length]
            },
            areaStyle: this.chartType === 'area' ? {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [{
                  offset: 0, color: colorPalette[buildingIndex % colorPalette.length] + '40'
                }, {
                  offset: 1, color: colorPalette[buildingIndex % colorPalette.length] + '10'
                }]
              }
            } : null,
            data: []
          };

          // 处理每月数据
          for (let month = 1; month <= 12; month++) {
            let monthData = building.emissionMonthAmount && building.emissionMonthAmount.find(m => m.month === month);
            let value = monthData ? monthData.emissionAmount : 0;
            seriesObj.data.push(value);
          }

          mySeries.push(seriesObj);
        });

        // 获取或创建图表实例
        const chartContainer = document.getElementById('monitorBarChart');
        if (!chartContainer) {
          return;
        }

        // 清理之前的实例
        if (this.myChart4) {
          this.myChart4.dispose();
        }

        this.myChart4 = this.$echarts.init(chartContainer);

      const option = {
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#4a7c3a',
          textStyle: {
            color: '#1a3d0d'
          },
          formatter: function(params) {
            let result = `<div style="font-weight: bold; color: #2d5016;">${params[0].name}</div>`;
            params.forEach(param => {
              result += `<div style="display: flex; align-items: center; margin: 4px 0;">
                <span style="display: inline-block; width: 12px; height: 12px; background-color: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>
                ${param.seriesName}: ${new Intl.NumberFormat('zh-CN').format(param.value)} kg
              </div>`;
            });
            return result;
          }
        },
        legend: {
          data: this.carbonInformation.map(b => b.building),
          top: 25,
          textStyle: {
            color: '#4a7c3a'
          }
        },
        grid: {
          left: '2%',
          right: '3%',
          bottom: '1%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
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
            margin: 2,
            formatter: function(value) {
              return new Intl.NumberFormat('zh-CN', {
                notation: 'compact',
                maximumFractionDigits: 1
              }).format(value);
            }
          },
          splitLine: {
            lineStyle: {
              color: '#e8f5e3',
              type: 'dashed'
            }
          }
        },
        series: mySeries,
        dataZoom: [{
          type: 'slider',
          show: true,
          xAxisIndex: [0],
          start: 0,
          end: 100,
          height: 20,
          bottom: 10,
          handleStyle: {
            color: '#4a7c3a'
          },
          textStyle: {
            color: '#4a7c3a'
          }
        }]
        };

        this.myChart4.setOption(option, true);
      });
    },
    getBuilding() {
      getCarbonLine({ buildings: this.building }).then(res => {
        if (res.data.code == 200) {
          this.carbonInformation = res.data.data
          var buildingsNameme = new Set();
          for (var i = 0; i < this.carbonInformation.length; i++) {
            buildingsNameme.add(this.carbonInformation[i].building);
          }
          // console.log("buildingsName", buildingsNameme);
          let setTestValues = buildingsNameme.values()
          // console.log("setTestValues", setTestValues)
          //let temp1 = setTestValues.next().value;
          var bui = []
          var n = buildingsNameme.size
          // bui.push(temp1)
          for (var i = 0; i < n; i++) {
            let temp1 = setTestValues.next().value
            bui.push(temp1);
          }
          // console.log("bui", bui)
          this.buildingbui = bui.toString()
          // console.log("this.buildingbui", this.buildingbui)
        }
      })
    },
    //叉掉多选楼宇tag时
    removeTag(tag) {
      this.buidingSet.delete(tag.label)//从楼宇缓存中删除掉该tag
      // console.log("this.buidingSet:", this.buidingSet)
    },
    getBuildings(building) {
      // var bui=[]
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
      // console.log("searchBuilding")
      // console.log("this.year:", this.year)
      if (this.year == '')
        this.showValue = false;
      else//当查询具体年份的时候才显示出来左上角
        this.showValue = true;
      // 使用后端返回的实际年份，而不是用户选择的年份
      // this.schoolInformation.year = this.year; // 注释掉，改为使用后端返回的年份
      let setTestValues = this.buidingSet.values()
      // console.log("setTestValues", setTestValues)
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
      } else {
        this.building = this.buildingbui
      }
      // console.log("bui", this.buill)
      // console.log("this.area", this.area)
      let a = false;

      if (this.area == 1) {
        this.title = '';
        a = true;
      } else {
        this.title = '';
      }
      // debugger
      // 如果用户选择了年份，传year参数；如果没选择，不传year参数（让后端回退查找）
      const params = { buildings: this.building, area: a };
      if (this.year && this.year != '') {
        params.year = this.year;
      }
      getCarbonLine(params).then(res => {
          if (res.data.code == 200) {
          this.carbonInformation = res.data.data

          // 如果用户选择了年份，显示用户选择的；如果用户没选择，显示后端返回的实际年份
          if (this.year && this.year != '') {
            this.schoolInformation.year = this.year; // 用户选择的年份
            this.showValue = true;
          } else if (this.carbonInformation && this.carbonInformation.length > 0) {
            // 回显后端返回的年份
            const backendYear = this.carbonInformation[0].year;
            this.schoolInformation.year = backendYear;
            this.year = backendYear ? backendYear.toString() : '';
            this.showValue = true;
          } else {
            this.showValue = false;
          }

          this.calculateKPIs();
          this.prepareTableData();
          this.renderChart();
        }
      }).catch(() => {

      }).finally(() => {
        bui = [];
        this.building = this.buildingbui
      });


    },
    getCarbonBar() {
      // 初始调用时不传year参数（让后端回退查找）
      getCarbonLine({ buildings: this.building }).then(res => {
        if (this.year == '')
          this.showValue = false;
        else
          this.showValue = true;
        if (res.data.code == 200) {
          this.carbonInformation = res.data.data

          // 如果用户选择了年份，显示用户选择的；如果用户没选择，显示后端返回的实际年份
          if (this.year && this.year != '') {
            this.schoolInformation.year = this.year; // 用户选择的年份
            this.showValue = true;
          } else if (this.carbonInformation && this.carbonInformation.length > 0) {
            // 回显后端返回的年份
            const backendYear = this.carbonInformation[0].year;
            this.schoolInformation.year = backendYear;
            this.year = backendYear ? backendYear.toString() : '';
            this.showValue = true;
          } else {
            this.showValue = false;
          }

          // 准备楼宇选项
          let buildingsName = new Set();
          this.carbonInformation.forEach(building => {
            buildingsName.add(building.building);
          });

          this.options = [];
          let k = 0;
          for (let item of buildingsName) {
            this.options.push({ label: item, value: k });
            k++;
          }

          this.calculateKPIs();
          this.prepareTableData();
          this.renderChart();
        } else {
          console.error("API返回错误:", res.data.message)
        }
      }).catch(error => {
        console.error("getCarbonBar 请求失败:", error)
      })
    }

  },



}
</script>
<style scoped>
/* 整体布局 */
.carbon-carbon {
  width: 100%;
  background-color: var(--forest-bg-primary);
  min-height: auto; /* 移除固定最小高度，防止不必要的滚动条 */
}

.main-box-carbon {
  max-width: 1400px;
  margin: 0 auto;
  background-color: transparent;
}

/* KPI 指标卡片区域 */
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
  border: 1px solid var(--forest-border-light);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: all 0.3s ease;
  box-shadow: var(--forest-shadow-sm);
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--forest-shadow-md);
}

.kpi-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--forest-secondary);
}

.kpi-header i {
  font-size: 20px;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--forest-primary);
  margin-bottom: 4px;
}

.kpi-trend {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  color: white;
}

.kpi-trend.positive {
  background-color: var(--forest-success);
}

.kpi-trend.negative {
  background-color: var(--forest-error);
}

.kpi-trend.stable {
  background-color: var(--forest-warning);
  color: var(--forest-text-primary);
}

.kpi-desc {
  font-size: 12px;
  color: var(--forest-text-secondary);
  margin-top: 4px;
}

/* 筛选条件区域 */
.filter-section {
  margin-bottom: 15px;
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

/* 输入框样式 */
.filter-input {
  width: 140px;
  min-width: 120px;
}

/* 重置按钮样式 */
.reset-btn {
  background-color: var(--forest-text-disabled);
  border-color: var(--forest-text-disabled);
  color: white;
}

.reset-btn:hover {
  background-color: var(--forest-text-secondary);
  border-color: var(--forest-text-secondary);
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

.chart-controls {
  display: flex;
  gap: 10px;
}

.chart-container {
  padding: 10px 20px 5px 20px;
  height: 350px;
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



.building-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.building-name i {
  color: var(--forest-secondary);
}

.number-cell {
  font-family: 'Monaco', 'Consolas', monospace;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-form {
    gap: 10px;
  }

  .filter-input {
    width: 130px;
  }
  
  /* 按钮组始终在一行，向左对齐 */
  .button-group {
    margin-left: 0;
    white-space: nowrap;
  }
}

@media (max-width: 992px) {
  .filter-form {
    gap: 10px;
    justify-content: flex-start;
  }
  
  ::v-deep .el-form-item {
    margin-right: 0;
  }
  
  .filter-input {
    width: 120px;
  }
  
  /* 按钮组在中等屏幕上向左对齐 */
  .button-group {
    margin-left: 0;
    flex: none;
    white-space: nowrap;
  }
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .main-box-carbon {
    padding: 10px;
  }

  .filter-section {
    margin-bottom: 15px;
  }
  
  .filter-content {
    padding: 15px;
  }

  .filter-form {
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
  }
  
  /* 表单项在小屏幕上更紧凑 - 三列布局 */
  ::v-deep .el-form-item {
    flex: 1 1 calc(33.333% - 7px);
    min-width: calc(33.333% - 7px);
    margin-right: 0;
  }
  
  /* 按钮组在小屏幕上保持一行，向左对齐 */
  .button-group {
    flex: 1 1 100%;
    min-width: 100%;
    justify-content: flex-start;
    margin-left: 0;
    margin-top: 5px;
    white-space: nowrap;
  }
  
  /* 按钮在小屏幕上保持原有间距 */
  .button-group .el-button {
    margin: 0 15px 0 0 !important;
  }
  
  /* 输入框在小屏幕上占满宽度 */
  ::v-deep .el-form-item__content {
    width: 100%;
  }
  
  .filter-input {
    width: 100%;
    min-width: auto;
  }

  .chart-container {
    height: 300px;
  }

  .legend-items {
    flex-direction: column;
    gap: 6px;
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
}

@media (max-width: 480px) {
  /* 在超小屏幕上，所有表单项垂直排列 */
  .filter-form {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  
  ::v-deep .el-form-item {
    flex: 1 1 100%;
    min-width: 100%;
    margin-right: 0;
  }
  
  /* 标签在超小屏幕上固定宽度 */
  ::v-deep .el-form-item__label {
    width: 80px;
    font-size: 13px;
  }
  
  .filter-input {
    flex: 1;
    width: auto;
  }
  
  /* 按钮组在超小屏幕上保持一行，向左对齐 */
  .button-group {
    flex-direction: row;
    justify-content: flex-start;
    margin-left: 0;
    flex: 1 1 100%;
    white-space: nowrap;
  }
  
  /* 按钮在超小屏幕上保持原有间距 */
  .button-group .el-button {
    margin: 0 15px 0 0 !important;
    flex: none;
  }
}

</style>

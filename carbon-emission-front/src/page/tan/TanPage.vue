<template>
  <div class="index-main-box-carbon">
    <div class="indextop">
      <div class="left">
        <div class="forest-card">
          <div class="forest-card-header">
              <i class="el-icon-location-information"></i>
              <span>全校概况</span>
          </div>
          <div class="forest-card-body">
            <div class="leftl">
              <el-descriptions class="xiaoyuangai" title=""
                :column="1" size="medium" border 
                :contentStyle="{color: '#1a3d0d', fontSize: 'var(--forest-main-font-size)'}" 
                :labelStyle="{color: '#4a7c3a', fontWeight: 600, fontSize: 'var(--forest-main-font-size)'}">
                <el-descriptions-item>
                  <template slot="label">
                    <i class="el-icon-school"></i>
                    学校名称
                  </template>
                  北京林业大学
                </el-descriptions-item>
                <el-descriptions-item>
                  <template slot="label">
                    <i class="el-icon-user"></i>
                    学校总人数
                  </template>
                  {{ formatNumber(schoolInformation.totalNumber) }}
                </el-descriptions-item>
                <el-descriptions-item>
                  <template slot="label">
                    <i class="el-icon-user-solid"></i>
                    在校学生人数
                  </template>
                  {{ formatNumber(schoolInformation.studentNumber) }}
                </el-descriptions-item>
                <el-descriptions-item>
                  <template slot="label">
                    <i class="el-icon-s-custom"></i>
                    在校教师人数
                  </template>
                  {{ formatNumber(schoolInformation.teacherNumber) }}
                </el-descriptions-item>
                <el-descriptions-item>
                  <template slot="label">
                    <i class="el-icon-s-home"></i>
                    校园总面积(㎡)
                  </template>
                  {{ formatNumber(schoolInformation.totalArea) }}
                </el-descriptions-item>
                <el-descriptions-item>
                  <template slot="label">
                    <i class="el-icon-location-outline"></i>
                    校园绿化总面积(㎡)
                  </template>
                  {{ formatNumber(schoolInformation.greenArea) }}
                </el-descriptions-item>
                <el-descriptions-item>
                  <template slot="label">
                    <i class="el-icon-office-building"></i>
                    校园建筑物面积(㎡)
                  </template>
                  {{ formatNumber(schoolInformation.buildingArea) }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
            <div class="xiaoyuantu">
              <div class="carbonbeijing" :style="{ backgroundImage: schoolImageStyle }"></div>
            </div>
          </div>
        </div>
      </div>
      <div class="carbonright forest-card">
        <div class="forest-card-header">
          <i class="el-icon-pie-chart"></i>
          <span>{{ curYear }}年北京林业大学碳排放构成</span>
        </div>
        <div class="carbonright-chart-wrap">
          <MyEcharts ref="assessmentGraph" :x="xValue" :y="yValue" :ids="charts1" :center="['40%', '50%']" :legendRight="'3%'" style="width: 100%; height: 100%;"></MyEcharts>
        </div>
      </div>
    </div>

    <div class="indexbottom">
      <div class="carbonon">
        <div class="forest-card">
          <div class="forest-card-header">
            <i class="el-icon-menu"></i>
            <span>{{ curYear }}总体年CO<sub>2</sub>排放量情况</span>
          </div>
          <div class="forest-card-body-table">
            <table class="table_two forest-table" border="1px">
              <thead height="50px" style="text-align: center;"> <!--表格的头部-->
                <th style="width: 40%;">指标</th>
                <!--表格头部的特殊单元格，默认有加粗效果-->
                <th style="width: 60%;">CO<sub>2</sub>排放量（kg）</th>
              </thead>
              <tbody style="text-align: center;">
                <!--tbody是一个语义化标签，没有任何作用，只是告诉你这部分是表格的身体-->
                <tr> <!--第一行的三个单元格： 单元格1  单元格2  单元格3-->
                  <td style="background-color: #f0f7ed;font-weight: 600;">总量</td>
                  <td>{{ formatNumber(parseFloat(carbonInformation.totalEmission).toFixed(2)) }}</td>
                </tr>
                <tr> <!--第二行的三个单元格：单元格4 单元格5 单元格6-->
                  <td style="background-color: #f0f7ed;font-weight: 600;">人均</td>
                  <td>{{ formatNumber(parseFloat(carbonInformation.personAverageEmission).toFixed(2)) }}</td>
                </tr>
                <tr>
                  <td style="background-color: #f0f7ed;font-weight: 600;">地均</td>
                  <td>{{ formatNumber(parseFloat(carbonInformation.groundAverageEmission).toFixed(2)) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="carbonup forest-card">
        <div class="forest-card-header">
          <i class="el-icon-s-data"></i>
          <span>{{ curYear }}年各排放源CO<sub>2</sub>排放量</span>
        </div>
        <div class="carbonup-chart-wrap">
          <div id="homeSpeciesBarChart"></div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import MyEcharts from "../../components/Assessment";
import TanTotal from "../../components/TanTotal";
import { getSchoolInfo } from "../../api/school";
import request from "../../utils/request";
export default {
  components: { TanTotal, MyEcharts },
  name: "",
  data() {
    return {
      curYear: 2023,
      charts: "",
      opinion: ["电力", "热力", "食物"],
      opinionData: [
        { value: 12, name: "电力", itemStyle: "#409EFF" },
        { value: 18, name: "热力", itemStyle: "#79d2c0" },
        { value: 18, name: "食物", itemStyle: "#f56c6c" }
      ],
      myChart4: null,
      opinion2: ['主楼', '实验楼', '学研大厦'],
      opinionData2: [
        { value: 85, itemStyle: '#1ab394' },
        { value: 15, itemStyle: 'red' },
        { value: 75, itemStyle: 'cadetblue' }
      ],
      schoolInformation: {
        studentNumber: '',
        teacherNumber: '',
        schoolName: '',
        totalNumber: '',
        totalArea: '',
        greenArea: '',
        buildingArea: '',
        imageUrl: '', // 学校图片地址
      },
      carbonInformation: {
        totalEmission: '',
        personAverageEmission: '',
        groundAverageEmission: '',
      },
      xValue: [],
      yValue: [],
      xxValue: [],
      yyValue: [],
      charts1: "charts1",
    };
  },
  computed: {
    // 计算学校图片样式，如果后端有图片地址则使用后端地址，否则使用前端静态资源
    schoolImageStyle() {
      if (this.schoolInformation.imageUrl) {
        return `url(${this.schoolInformation.imageUrl})`;
      } else {
        return 'url(../../pic/co25.jpg)';
      }
    }
  },
  mounted: function () {
    const currentDate = new Date();
    this.curYear = currentDate.getFullYear();

    this.getSchoolData()
    this.getPie()
    this.getCarbonBar()
    this.getCarbonData()
    
    // 页面加载完成后额外触发一次 resize，确保 flex 布局计算完成后图表能铺满
    this.$nextTick(() => {
      this.handleResize();
    });
  },
  activated() {
    // 针对 keep-alive 模式，回到页面时重新渲染并自适应
    this.handleResize();
  },
  beforeDestroy() {
    // 组件销毁前清理图表实例和监听器
    window.removeEventListener('resize', this.handleResize);
    if (this.myChart4) {
      this.myChart4.dispose();
      this.myChart4 = null;
    }
  },
  methods: {
    // 格式化数字，添加千位逗号分隔符
    formatNumber(value) {
      if (value === null || value === undefined || value === '') {
        return '';
      }
      // 将值转换为字符串
      const str = String(value);
      // 分离整数部分和小数部分
      const parts = str.split('.');
      // 对整数部分添加千位逗号
      parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
      // 返回格式化后的字符串
      return parts.join('.');
    },
    // 抽离统一的 resize 方法
    handleResize() {
      if (this.myChart4) {
        this.myChart4.resize();
      }
      // 如果子组件有 resize 方法也一并调用
      if (this.$refs.assessmentGraph && this.$refs.assessmentGraph.resize) {
        this.$refs.assessmentGraph.resize();
      }
    },
    getPie() {
      request.get("/api/carbonEmission/listSpeciesCarbon").then((res) => {
        if (res.data.code == 200) {
          // 兼容新旧两种返回格式
          let dataList = res.data.data.list || res.data.data;
          // 如果返回了实际使用的年份，更新显示的年份
          if (res.data.data.actualYear) {
            this.curYear = parseInt(res.data.data.actualYear)
          }
          // console.log(res.data)
          // console.log(dataList.length)
          for (var i = 0; i < dataList.length; i++) {
            var carbonpie = dataList[i];
            let objectCategory = carbonpie.objectCategory;
            this.xValue.push(objectCategory);
            this.yValue.push({ value: carbonpie.emissionAmount, name: objectCategory });
          }
          this.$refs.assessmentGraph.drawLine(this.yValue);
        }
      }).catch(() => {
      }).finally(() => {
      });
    },
    getCarbonData() {
      request.get("/api/carbonEmission/collegeCarbonEmission").then((res) => {
        if (res.data.code == 200) {
          this.carbonInformation = res.data.data
          // 如果返回了实际使用的年份，更新显示的年份
          if (res.data.data.actualYear) {
            this.curYear = parseInt(res.data.data.actualYear)
          }
        }
      }).catch(() => {
      }).finally(() => {
      });
    },
    getSchoolData() {
      // console.log("xiaoyuan")

      request.get("/api/school/info").then((res) => {

        // console.log(res.data.code)
        if (res.data.code == 200) {
          this.schoolInformation = res.data.data
          // 如果返回了实际使用的年份，更新显示的年份
          if (res.data.data.actualYear) {
            this.curYear = parseInt(res.data.data.actualYear)
          }
        }
      }).catch(() => {
      }).finally(() => {
      });

    },
    getCarbonBar() {
      request.get("/api/carbonEmission/listSpeciesCarbon").then((res) => {
        if (res.data.code == 200) {
          // 兼容新旧两种返回格式
          let dataList = res.data.data.list || res.data.data;
          // 如果返回了实际使用的年份，更新显示的年份
          if (res.data.data.actualYear) {
            this.curYear = parseInt(res.data.data.actualYear)
          }
          // console.log(res.data)
          // console.log(dataList.length)
          var xValueTemp = [];
          var yValueTemp = [];
          for (var i = 0; i < dataList.length; i++) {
            xValueTemp.push(dataList[i].objectCategory);
            yValueTemp.push(dataList[i].emissionAmount);
          }
          this.xxValue = xValueTemp;
          this.yyValue = yValueTemp;
          
          this.$nextTick(() => {
            // 获取 DOM 元素
            const chartDom = document.getElementById('homeSpeciesBarChart');
            if (!chartDom) {
              return;
            }
            
            // 核心修复：确保 DOM 已经有宽高，否则延迟初始化
            if (chartDom.clientWidth === 0 || chartDom.clientHeight === 0) {
              setTimeout(() => {
                this.initSpeciesBarChart(chartDom);
              }, 100);
            } else {
              this.initSpeciesBarChart(chartDom);
            }
          });
        }
      }).catch(() => {
      }).finally(() => {
      });

    },
    // 将初始化逻辑抽离，支持重试
    initSpeciesBarChart(chartDom) {
      if (!chartDom) return;
      
      // 先检查是否已有图表实例，如果有则先销毁
      if (this.myChart4) {
        this.myChart4.dispose();
      }
      
      // 初始化echarts实例
      this.myChart4 = this.$echarts.init(chartDom);
      this.myChart4.setOption({
        title: {
          text: 'CO₂排放量(kg)',
          left: 'left',
          top: '0%'
        },
        grid: {
          top: '20%',
          left: '3%',
          right: '3%',
          bottom: '3%',
          containLabel: true
        },
        tooltip: {
          trigger: 'item',
          formatter: (params) => {
            return '碳排放量' + '<br/>' + params.marker + params.name + ':' + parseFloat(params.value).toFixed(2) + '(kg)' + '<br/>';
          },
        },
        legend: {
          orient: 'vertical',
          x: 'right',
          y: 'top',
          data: ['CO₂排放量']
        },
        xAxis: {
          data: this.xxValue,
          axisLabel: {
            interval: 0
          },
        },
        yAxis: {},
        series: [{
          name: 'CO₂排放量',
          type: 'bar',
          barMaxWidth: '40%',
          data: this.yyValue,
          itemStyle: {
            color: function () {
              return "#" + Math.floor(Math.random() * 16777215).toString(16);
            }
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      })
      
      // 移除旧的监听并添加新的 resize 监听
      window.removeEventListener('resize', this.handleResize);
      window.addEventListener('resize', this.handleResize);
    }

  },



}
</script>
<style>
/* flex: 0 1 60%（基础 60%、可收缩、不放大），在 .indextop 的 flex 中正确占 60% 宽；
   min-width: 0 使 flex 子项能在有 gap 时按比例收缩，避免被内容撑满。 */
.left {
  position: relative;
  flex: 0 1 60%;
  min-width: 0;
  display: flex;
}

/* 让左侧卡片铺满 .left，且为纵向 flex，这样 .forest-card-body 的 flex: 1 才能占满 header 下方的剩余高度。 */
.left .forest-card {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* min-height: 0：flex 子项默认 min-height: auto 会按内容撑高，导致 flex: 1 无法正确拿到剩余高度；设为 0 才能让正文区占满 header 下方的空间。 */
.forest-card-body {
  display: flex;
  flex: 1; /* 智能占 “剩余空间” */
  min-height: 0;
  overflow: hidden;
}

.leftl {
  flex: 0 0 45%; /*学校描述占40%*/ 
  min-width: 0;
  overflow: hidden; /* 核心：防止描述内容溢出导致宽高失控 */
  display: flex;
  flex-direction: column;
}

.xiaoyuangai {
  /* padding-top: 20px; */
  padding: 10px 0 15px 20px;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  box-sizing: border-box; /* 确保 padding 不会撑破容器 */
}

/* 核心优化：强制 el-descriptions 内部组件完全填满或按比例缩放 */
.index-main-box-carbon .xiaoyuangai .el-descriptions__body {
  width: 91%; /* 与底部表格保持一致 */
  height: 93%; /* 与底部表格保持一致 */
  background: transparent;
  display: flex;
}

.index-main-box-carbon .xiaoyuangai .el-descriptions-table {
  flex: 1;
  height: 100% !important;
  width: 100%;
  table-layout: fixed;
}

.index-main-box-carbon .xiaoyuangai .el-descriptions-table tr {
  height: calc(100% / 7) !important;
}

/* 统一首页字体大小 & 垂直居中 */
.index-main-box-carbon .xiaoyuangai .el-descriptions-item__label,
.index-main-box-carbon .xiaoyuangai .el-descriptions-item__content {
  font-size: var(--forest-main-font-size) !important;
  vertical-align: middle;
}

.xiaoyuantu {
  flex: 0 0 55%;  /*图占60%*/ 
  min-width: 0;
  display: flex;
  align-items: stretch;
  justify-content: center;
  overflow: hidden;
}

.forest-card-body .carbonbeijing {
  width: 100%;
  height: 100%;
  max-width: 100%;
  min-height: 88%;
  margin: 20px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(45, 80, 22, 0.1);
  display: block;
}

.index-main-box-carbon .table_two {
  margin: 16px;
  border-collapse: collapse;
}

.carbonbeijing {
  position: relative;
  width: 400px;
  height: 300px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  max-width: calc(100% - 40px);
  max-height: calc(100% - 95px);
}

.carbon-carbon {
  width: 100%;
  background-size: 100% 100%;
}

.index-main-box-carbon {
  position: relative;
  background-color: var(--forest-bg-primary);
  width: 100%;
  margin: 0;
  height: 100%; /* 占据父级 .child 的 100% 高度 */
  min-height: 0;
  text-align: center;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 确保内部不溢出产生滚动条 */
  /* 抽取全局字体属性，确保全页比例协调 */
  --forest-main-font-size: 1.03rem;
}

/* 左 60% + 右 40%”未正确生效时会多出中间空白，改为 flex-start 让左右紧挨排布，align-items: stretch：让 .left 与 .carbonright 等高，右侧饼图卡片才能与左侧「全校概况」齐高。 */
.indextop {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: stretch;  /* 让所有 flex 子元素在交叉轴方向上自动拉伸，填满父容器的交叉轴空间，无需手动设置子元素的高度 / 宽度（取决于 flex 方向） */
  width: 80%;
  margin: 0 auto;
  gap: 15px;
  height: 50%; /* 占据父容器 60% 高度 */
}

/* 底部一行：左侧表格卡片 + 右侧柱状图 */
.indexbottom {
  display: flex;
  justify-content: center;
  align-items: stretch; /* 子元素齐高 */
  width: 80%;
  margin: 0 auto;
  margin-top: 20px;
  gap: 15px;
  height: 40%; /* 占据父容器 40% 高度 */
}

.forest-card {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(45, 80, 22, 0.1);
  border: 1px solid #c8e0c0;
  background: white;
  transition: box-shadow 0.3s ease;
}

.forest-card:hover {
  box-shadow: 0 4px 16px rgba(45, 80, 22, 0.15);
  /* 移除 transform 避免晃动 */
}

.forest-card .forest-card-header {
  overflow: hidden;
  border-radius: 12px 12px 0 0;
}

.forest-card-header {
  background: linear-gradient(135deg, #4a7c3a 0%, #2d5016 100%);
  color: white;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
}

.index-main-box-carbon .forest-table thead {
  background: linear-gradient(135deg, #4a7c3a 0%, #2d5016 100%);
  color: white;
}

.index-main-box-carbon .forest-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  height: 100%; /* 首页表格强制填满 */
}

.index-main-box-carbon .forest-table th {
  padding: 12px 8px;
  text-align: center;
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.2);
  word-wrap: break-word;
  font-size: var(--forest-main-font-size);
}

.index-main-box-carbon .forest-table td {
  padding: 12px 8px;
  text-align: center;
  border: 1px solid #c8e0c0;
  color: #1a3d0d;
  word-wrap: break-word;
}

.index-main-box-carbon .forest-table tbody tr:hover {
  background: #e8f5e3;
}

/* 饼图卡片 flex: 0 1 40% 在 flex 行里占 40%；display: flex; flex-direction: column（竖直方向排列） 让 .carbonright-chart-wrap 用 flex: 1 占满 header 下方的空间，饼图才有高度。 */
.carbonright {
  flex: 0 1 40%;
  min-width: 0;
  display: flex;
  flex-direction: column; 
}

/* flex: 1 占满 .carbonright 除 header 外的剩余高度；min-height: 0 重置 flex 元素的最小高度限制，允许高度压缩，默认 min-height: auto：flex 元素的最小高度不能小于其内容的高度； */
.carbonright-chart-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
}

/* ---------- 底部区域：左侧表格卡片与右侧柱状图并排 ---------- */
.carbonon {
  flex: 0 1 27%; /* 占据 30% 宽度 */
  min-width: 300px;
  display: flex;
  flex-direction: column;
}

.carbonon .forest-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.index-main-box-carbon .forest-card-body-table {
  flex: 1;
  min-height: 0;
  display: flex;
  justify-content: center;
  align-items: stretch; /* 让子元素（table）拉伸填满高度 */
  padding-top: 15px;
  overflow: hidden;
}

.index-main-box-carbon .forest-card-body-table .table_two {
  width: 91%;
  height: 93% !important; /* 强制表格填满容器，增加权重防止被全局覆盖 */
  margin: 0; /* 移除 margin 防止溢出 */
  table-layout: fixed;
}

.index-main-box-carbon .forest-card-body-table .table_two thead {
  height: 50px; /* 固定表头高度 */
}

.index-main-box-carbon .forest-card-body-table .table_two tbody {
  height: auto;
}

.index-main-box-carbon .forest-card-body-table .table_two tr {
  height: calc((100% - 50px) / 3) !important; /* 3 行均分剩余高度，强制执行 */
}

.index-main-box-carbon .forest-card-body-table .table_two th,
.index-main-box-carbon .forest-card-body-table .table_two td {
  padding: 0 10px;
  vertical-align: middle;
  font-size: var(--forest-main-font-size);
}

.carbonup {
  flex: 1; /* 占据剩余宽度 */
  min-width: 600px;
  display: flex;
  flex-direction: column;
}

.carbonup-chart-wrap {
  flex: 1;
  min-height: 0;
  padding: 15px 0px 5px 5px; /* 减少底部内边距 */
  box-sizing: border-box;
}

#homeSpeciesBarChart {
  width: 100%;
  height: 100%;
}
</style>
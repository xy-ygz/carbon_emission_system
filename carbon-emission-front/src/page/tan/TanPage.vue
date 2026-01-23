<template>
  <div class="index-main-box-carbon">
    <div class="indextop">
      <div class="left">
        <div class="l forest-card"
          style="width:850px;height:390px;margin-top:13px;position: relative;">
          <div class="forest-card-header">
            <i class="el-icon-location-information"></i>
            <span>全校概况</span>
          </div>
          <el-descriptions class="xiaoyuangai" style="margin-top:20px;margin-left:20px;width:350px;height:200px" title=""
            :column="1" size="medium" border :contentStyle="{color: '#1a3d0d'}" :labelStyle="{color: '#4a7c3a', fontWeight: 600}">
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
              {{ schoolInformation.totalNumber }}
            </el-descriptions-item>
            <el-descriptions-item>
              <template slot="label">
                <i class="el-icon-user-solid"></i>
                在校学生人数
              </template>
              {{ schoolInformation.studentNumber }}
            </el-descriptions-item>
            <el-descriptions-item>
              <template slot="label">
                <i class="el-icon-s-custom"></i>
                在校教师人数
              </template>
              {{ schoolInformation.teacherNumber }}
            </el-descriptions-item>
            <el-descriptions-item>
              <template slot="label">
                <i class="el-icon-s-home"></i>
                校园总面积
              </template>
              {{ schoolInformation.totalArea }}
            </el-descriptions-item>
            <el-descriptions-item>
              <template slot="label">
                <i class="el-icon-location-outline"></i>
                校园绿化总面积
              </template>
              {{ schoolInformation.greenArea }}
            </el-descriptions-item>
            <el-descriptions-item>
              <template slot="label">
                <i class="el-icon-office-building"></i>
                校园建筑物面积
              </template>
              {{ schoolInformation.buildingArea }}
            </el-descriptions-item>
          </el-descriptions>
          <div class="xiaoyuantu" style="overflow: hidden;">
            <div class="carbonbeijing" :style="{backgroundImage: schoolImageStyle, width: '400px', height: '300px', margin: '20px', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 2px 8px rgba(45, 80, 22, 0.1)', display: 'block'}"></div>
          </div>
        </div>
      </div>
      <div class="carbonright forest-card"
        style="margin-top:13px;height:390px;flex: 1;min-width: 500px;">
        <div class="forest-card-header">
          <i class="el-icon-pie-chart"></i>
          <span>{{ curYear }}年北京林业大学碳排放构成</span>
        </div>
        <div style="width: 100%;height: calc(100% - 50px);display: flex;justify-content: center;align-items: center;padding: 10px 20px 30px 20px;box-sizing: border-box;">
          <MyEcharts ref="assessmentGraph" :x='xValue' :y="yValue" :ids="charts1" style="width: 100%;height: 100%;"></MyEcharts>
        </div>
      </div>
    </div>

    <div class="indexbottom">
      <div class="carbonon">
        <div class="forest-card"
          style="width:400px;margin-top:15px;height:330px;">
          <div class="forest-card-header">
            <i class="el-icon-menu"></i>
            <span>{{ curYear }}总体年CO<sub>2</sub>排放量情况</span>
          </div>
          <div style="margin: 12px auto; width: calc(100% - 40px); display: flex; justify-content: center;">
            <table style="width: 100%; max-width: 280px; height: 200px;" class="table_two forest-table" border="1px">
              <thead height="50px" style="text-align: center;"> <!--表格的头部-->
                <th style="width: 40%;">指标</th>
                <!--表格头部的特殊单元格，默认有加粗效果-->
                <th style="width: 60%;">CO<sub>2</sub>排放量（kg）</th>
              </thead>
              <tbody style="text-align: center;">
                <!--tbody是一个语义化标签，没有任何作用，只是告诉你这部分是表格的身体-->
                <tr> <!--第一行的三个单元格： 单元格1  单元格2  单元格3-->
                  <td style="background-color: #f0f7ed;font-weight: 600;">总量</td>
                  <td>{{ parseFloat(carbonInformation.totalEmission).toFixed(2) }}</td>
                </tr>
                <tr> <!--第二行的三个单元格：单元格4 单元格5 单元格6-->
                  <td style="background-color: #f0f7ed;font-weight: 600;">人均</td>
                  <td>{{ parseFloat(carbonInformation.personAverageEmission).toFixed(2) }}</td>
                </tr>
                <tr>
                  <td style="background-color: #f0f7ed;font-weight: 600;">地均</td>
                  <td>{{ parseFloat(carbonInformation.groundAverageEmission).toFixed(2) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="carbonup forest-card"
        style="margin-top:15px;height:330px;flex: 1;min-width: 900px;">
        <div class="forest-card-header">
          <i class="el-icon-s-data"></i>
          <span>{{ curYear }}年各排放源CO<sub>2</sub>排放量</span>
        </div>
        <div id="myChart4" style="width:95%;height:290px;float:left;margin-left:40px;margin-top: 15px"></div>
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
  },
  beforeDestroy() {
    // 组件销毁前清理图表实例
    if (this.myChart4) {
      this.myChart4.dispose();
      this.myChart4 = null;
    }
  },
  methods: {
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
      // console.log("xiaoyuancarbon")
      // ////debugger
      request.get("/api/carbonEmission/collegeCarbonEmission").then((res) => {
        // ////debugger
        // console.log(res.data.code)
        // console.log(res.data)
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
          //this.$refs.voteGraph.drawLine(xValueTemp, yValueTemp);
          // console.log(this.xxValue)
          // console.log(this.yyValue)
          
          // 获取 DOM 元素
          const chartDom = document.getElementById('myChart4');
          if (!chartDom) {
            return;
          }
          
          // 先检查是否已有图表实例，如果有则先销毁
          if (this.myChart4) {
            this.myChart4.dispose();
          }
          
          // 初始化echarts实例
          this.myChart4 = this.$echarts.init(chartDom);
          this.myChart4.setOption({
            title: {
              text: 'CO₂排放量(kg)'
            },
            tooltip: {
              trigger: 'item',
              formatter: (params) => {
                // console.log(params);
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
              data: this.yyValue,
              itemStyle: {
                color: function () {
                  // 自定义颜色
                  // var colorList = ['#1ab394', '#FB4A55', 'cadetblue','#E6A23C','#409EFF','cadetblue','#409EFF','#E6A23C']
                  // return colorList[params.dataIndex]
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
        }
      }).catch(() => {
      }).finally(() => {
      });

    }

  },



}
</script>
<style>
.left {
  position: relative;
}

.table_two {
  margin: 16px;
  border-collapse: collapse;
}

.carbonbeijing {
  position: absolute;
  top: 55px;
  right: 20px;
  width: 400px;
  height: 300px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  max-width: calc(100% - 40px);
  max-height: calc(100% - 110px);
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
  min-height: calc(100vh - 100px);
  padding: 20px;
  text-align: left;
  margin-top: 0;
  margin-bottom: 0;
  box-sizing: border-box;
}

.indextop {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  gap: 15px;
}

.indexbottom {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  gap: 20px;
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
  font-size: 18px;
  font-weight: 600;
}

.forest-card-header i {
  font-size: 20px;
}

.forest-table thead {
  background: linear-gradient(135deg, #4a7c3a 0%, #2d5016 100%);
  color: white;
}

.forest-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.forest-table th {
  padding: 12px 8px;
  text-align: center;
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.2);
  word-wrap: break-word;
}

.forest-table td {
  padding: 12px 8px;
  text-align: center;
  border: 1px solid #c8e0c0;
  color: #1a3d0d;
  word-wrap: break-word;
}

.forest-table tbody tr:hover {
  background: #e8f5e3;
}

.left,
.carbonright {
  display: inline-block;
}

.carbonright {
  vertical-align: top;
}

.carbonon,
.carbonup {
  display: inline-block;
}

.carbonup {
  vertical-align: top;
}
</style>

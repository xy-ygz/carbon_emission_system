<template>
  <div class="carbon-carbon">
    <div class="main-box-carbon">
      <div class="left">
        <div class="l" style="width:750px;height:390px;border: #636F6C;margin-top:13px;margin-left: 30px;margin-right:40px;box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04);">
          <div style="width:750px;height:40px;background-color: steelblue">
            <span class="el-icon-location-information" style="color: #FFFFFF;font-size: 22px;margin-top: 8px;margin-left: 20px">全校概况</span>
          </div>
          <el-descriptions class="xiaoyuangai" style="margin-top:20px;margin-left:20px;width:270px;height:200px" title="" :column="1" size="medium" border >
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
          <div class="xiaoyuantu">
            <div class="carbonbeijing" style="width:400px;height:300px;margin:20px"></div>
          </div>
        </div>
      </div>
      <div class="carbonright" style="margin-top:13px;height:390px;box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04);">
        <div style="width:450px;height:40px;background-color: steelblue">
          <span class="el-icon-pie-chart" style="color: #FFFFFF;font-size: 22px;margin-top: 8px;margin-left: 20px">CO<sub>2</sub>排放量</span>
        </div>
        <div style="margin-left: 28px;margin-bottom: 50px">
          <MyEcharts ref="assessmentGraph" :x='xValue' :y="yValue" :ids="charts1"></MyEcharts>
        </div>
      </div>
      <div>
        <div class="carbonon">
          <div style="width:350px;margin-top:15px;height:330px;margin-left: 30px;box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04);">
            <div style="width:350px;height:40px;background-color: steelblue">
              <span class="el-icon-menu" style="color: #FFFFFF;font-size: 22px;margin-top: 8px;margin-left: 20px">CO<sub>2</sub>排放量</span>
            </div>
            <div style="margin-left: 17px;margin-top: 24px">
              <table  width="260px" height="200px" class="table_two" border="1px">
                <thead height="50px" style="text-align: center;background-color: whitesmoke">  <!--表格的头部-->
                <th>指标</th>
                <!--表格头部的特殊单元格，默认有加粗效果-->
                <th>CO<sub>2</sub>排放量（kg）</th>
                </thead>
                <tbody style="text-align: center;">
                <!--tbody是一个语义化标签，没有任何作用，只是告诉你这部分是表格的身体-->
                <tr>  <!--第一行的三个单元格： 单元格1  单元格2  单元格3-->
                  <td style="background-color: whitesmoke">总量</td>
                  <td>{{ carbonInformation.totalEmission}}</td>
                </tr>
                <tr> <!--第二行的三个单元格：单元格4 单元格5 单元格6-->
                  <td style="background-color: whitesmoke">人均</td>
                  <td>{{ carbonInformation.personAverageEmission}}</td>
                </tr>
                <tr>
                  <td style="background-color: whitesmoke">地均</td>
                  <td>{{ carbonInformation.groundAverageEmission}}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div class="carbonup" style="margin-top:15px;width:850px;height:330px;margin-left: 40px;box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04);">
          <div style="width:850px;height:40px;background-color: steelblue">
            <span class="el-icon-s-data" style="color: #FFFFFF;font-size: 22px;margin-top: 8px;margin-left: 20px">CO<sub>2</sub>排放量</span>
          </div>
          <div id="myChart4" style="width:95%;height:285px;float:left;margin-left:40px;margin-top: 20px"></div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import MyEcharts from "../../components/Assessment";
import { getSchoolInfo } from "../../api/school";
import request from "../../utils/request";
export default {
  components: {MyEcharts},
  name: "",
  data() {
    return {
      charts: "",
      opinion: ["电力", "热力","食物"],
      opinionData: [
        { value: 12, name: "电力", itemStyle: "#409EFF" },
        { value: 18, name: "热力", itemStyle: "#79d2c0" },
        { value: 18, name: "食物", itemStyle: "#f56c6c" }
      ],
      myChart4: '',
      opinion2: ['主楼', '实验楼','学研大厦'],
      opinionData2: [
        { value: 85, itemStyle: '#1ab394' },
        { value: 15, itemStyle: 'red' },
        { value:  75, itemStyle: 'cadetblue' }
      ],
      schoolInformation: {
        studentNumber:'',
        teacherNumber:'',
        schoolName:'',
        totalNumber: '',
        totalArea:'',
        greenArea:'',
        buildingArea:'',
      },
      carbonInformation: {
        totalEmission: '',
        personAverageEmission: '',
        groundAverageEmission: '',
      },
      xValue:[],
      yValue:[],
      xxValue:[],
      yyValue:[],
      charts1:"charts1",
    };
  },
  mounted:function() {

    this.getSchoolData()
    this.getPie()
    this.getCarbonBar()
    this.getCarbonData()
  },
  methods:{
    getPie(){
      request.get("/api/carbonEmission/listSpeciesCarbon").then((res) => {
        if (res.data.code == 200){
          console.log(res.data)
          console.log(res.data.data.length)
          for (var i = 0;i < res.data.data.length;i++) {
            var carbonpie = res.data.data[i];
            let objectCategory=carbonpie.objectCategory;
            this.xValue.push(objectCategory);
            this.yValue.push({value: carbonpie.emissionAmount, name: objectCategory});
          }
          this.$refs.assessmentGraph.drawLine(this.yValue);
        }
      }).catch(() => {
      }).finally(() => {
      });
    },
    getCarbonData() {
      console.log("xiaoyuancarbon")
      // //debugger
      request.get("/api/carbonEmission/collegeCarbonEmission").then((res) => {
        // //debugger
        console.log(res.data.code)
        console.log(res.data)
        if (res.data.code == 200) {
          this.carbonInformation = res.data.data
        }
      }).catch(() => {
      }).finally(() => {
      });
    },
    getSchoolData(){
      console.log("xiaoyuan")

      request.get("/api/school/info").then((res) => {

        console.log(res.data.code)
        if(res.data.code==200) {
          this.schoolInformation = res.data.data
        }
      }).catch(() => {
      }).finally(() => {
      });

    },
    getCarbonBar(){
      request.get("/api/carbonEmission/listSpeciesCarbon").then((res) => {
        if (res.data.code == 200) {
          console.log(res.data)
          console.log(res.data.data.length)
          var xValueTemp = [];
          var yValueTemp = [];
          for (var i = 0; i < res.data.data.length; i++) {
            xValueTemp.push(res.data.data[i].objectCategory);
            yValueTemp.push(res.data.data[i].emissionAmount);
          }
          this.xxValue = xValueTemp;
          this.yyValue = yValueTemp;
          //this.$refs.voteGraph.drawLine(xValueTemp, yValueTemp);
          console.log(this.xxValue)
          console.log(this.yyValue)
          this.myChart4 = this.$echarts.init(document.getElementById('myChart4'))
          this.myChart4.setOption({
            title: {
              text: 'CO₂排放量(kg)'
            },
            tooltip: {
              trigger: 'item',
              formatter: '{a} <br/>{b} : {c}'
            },
            legend: {
              orient: 'vertical',
              x: 'right',
              y: 'top',
              data: ['CO₂排放量']
            },
            xAxis: {
              data: this.xxValue
            },
            yAxis: {},
            series: [{
              name: 'CO₂排放量',
              type: 'bar',
              data: this.yyValue,
              itemStyle: {
                emphasis: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                },
                color: function () {
                  // 自定义颜色
                  // var colorList = ['#1ab394', '#FB4A55', 'cadetblue','#E6A23C','#409EFF','cadetblue','#409EFF','#E6A23C']
                  // return colorList[params.dataIndex]
                  return "#"+Math.floor(Math.random()*16777215).toString(16);
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
.left{
  position: relative;
}
.table_two {
  margin: 16px;
  border-collapse: collapse;
}
.carbonbeijing{
  position: absolute;
  top: 55px;
  right: 60px;
  width: 100%;
  height: 100%;
  background: url(../../pic/co25.jpg);
  background-size: 100% 100%;

}
.carbon-carbon{
  width: 100%;
  background-size: 100% 100%;
}
.main-box-carbon {
  position: relative;
  background-color: #FFFFFF;
  width:100%;
  height:100%;
  text-align: center;
  padding:10px;
  margin-top:15px;
  margin-bottom:100px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04);
  border-radius: 4px;
}
.left,.carbonright{
  display: inline-block;
}
.carbonright {
  vertical-align: top;
}
.carbonon,.carbonup{
  display: inline-block;
}

.carbonup {
  vertical-align: top;
}
</style>

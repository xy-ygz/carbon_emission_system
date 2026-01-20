<template>
  <div class="tanResult tanExport">
    <!-- 控制栏 -->
    <div class="export-controls">
      <div class="flex-box-headerr">
        <el-form :inline="true">
          <el-form-item label="选择年份：">
            <el-date-picker v-model="showYear" type="year" value-format='yyyy' 
              @change="getExportData" placeholder="选择年" size="small" style="width: 200px;">
            </el-date-picker>
          </el-form-item>
          <el-form-item>
            <el-button size="small" type="primary" icon="el-icon-download" @click="exportToPDF">导出PDF</el-button>
            <el-button size="small" type="success" icon="el-icon-document" @click="exportToWord">导出Word</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <!-- PDF预览容器 -->
    <div id="pdf-preview" class="pdf-preview-container">
    <div class="exportTitle">
      <span style="font-size: 35px;">北京林业大学{{ showYear || '--' }}年度碳排放报告</span>
      <div style="font-size: 30px;text-align: left; margin-left: 5%;margin-top: 1em;">
        前言
      </div>
      <div>
        <p>在我国碳达峰碳中和目标下，高等教育机构肩负引领社会可持续发展，在双碳战略中发挥先锋作用的使命。</p>
        <p>校园碳排放核算是实现校园碳中和进程的重要基础。北京林业大学自主积极参与，扎实推进校园碳中和目标的实现。</p>
        <p>本报告可作为“零碳校园评价”、“校园可持续发展力排名”、“绿色学校”、“绿色校园评价”等参评的基础资料。</p>
      </div>
      <div style="font-size: 30px;text-align: left; margin-left: 5%;">
        一、校园基本情况
      </div>
      <div>
        <p>{{ schoolInfo.schoolName || '北京林业大学' }}（Beijing Forestry University），位于北京市，教育部直属、教育部与国家林业和草原局、北京市人民政府共建的全国重点大学。</p>
        <p>
          {{
              showYear
            }}年，{{ schoolInfo.schoolName || '北京林业大学' }}共有师生{{ schoolInfo.totalNumber || '--' }}人（其中学生{{ schoolInfo.studentNumber || '--' }}人，教师{{ schoolInfo.teacherNumber || '--' }}人），占地面积{{ schoolInfo.totalArea || '--' }}平方米，建筑面积{{ schoolInfo.buildingArea || '--' }}平方米，绿化面积{{ schoolInfo.greenArea || '--' }}平方米。{{ schoolInfo.schoolName || '北京林业大学' }}人口密度大，科研、办公、生活等功能多样，包括教室、宿舍、食堂、实验室、办公楼和体育设施，拥有广泛的植被、绿地和树木，校园平面图见图1。
        </p>
      </div>
      <div style="display: flex; justify-content: center; align-items: center; margin: 20px 0;">
        <img src="../../pic/planeGraph.png" alt="图片加载中" style="max-width: 100%; height: auto; max-height: 400px;">
      </div>
      <div style="text-align: center; margin-bottom: 30px;">
        <span style="font-size: 22px;">图1 校园平面图</span>
      </div>
      <div style="font-size: 30px;text-align: left; margin-left: 5%;margin-top: 1em;">
        二、碳排放核算范围
      </div>
      <div>
        <p>
          根据《工业企业温室气体排放核算与报告指南》，本报告明确规定了直接排放(范围1)和间接排放(范围2)的排放要求。具体来说，天然气和化石燃料消耗被纳入范围1，而电力和热力被纳入范围2。为了计算其他碳排放量，本报告根据相关文献和校园的实际情况考虑了以下排放源：{{ emissionSources.length > 0 ? emissionSources.join('、') : defaultEmissionSources.join('、') }}。碳排放核算范围及核算清单见图2。
        </p>
      </div>
      <div style="margin-bottom: 1.5em;">
        <div id="inventoryChart" style="width: 100%; height: 500px; margin: 20px auto;"></div>
      </div>
      <span style="font-size: 22px">图2 校园碳排放核算范围及清单</span>
    </div>
    <div style="font-size: 30px;text-align: left; margin-left: 5%;margin-top: 1em;">
      三、碳排放流动趋势
    </div>
    <div style="font-size: 28px;text-align: left; margin-left: 5%;margin-top: 1em;">
      3.1 校园内资源、能源消耗和废弃物产生情况
    </div>
    <div>
      <p>碳排放核算结果
        根据校园碳排放核算范围及核算清单，{{ showYear }}年北京林业大学资源、能源消耗和废弃物产生情况见表1。
      </p>
    </div>
    <span style="font-size: 22px;">表1 {{ showYear }}年北京林业大学资源、能源消耗和废弃物产生情况</span>
    <div style="width: 70%; overflow: auto;margin: 20px auto;">
      <el-table :data="tableData1" border class="forest-table">
        <el-table-column prop="objectCategory" label="参数" min-width="200" align="center"></el-table-column>
        <el-table-column prop="emissionAmount" label="数值" min-width="150" align="center">
          <template slot-scope="scope">
            {{ ((scope.row.emissionAmount || 0) / 1000).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="单位" min-width="120" align="center">
          <template slot-scope="scope">
            {{ getUnit(scope.row.objectCategory) }}
          </template>
        </el-table-column>
      </el-table>
      <!-- 换页菜单 -->
      <div style="text-align:center; margin:20px;padding-right: 50px;">
        <el-pagination background @current-change="handleCurrentChange" :page-size="limit1"
          :current-page.sync="current1" layout="total, prev, pager, next" :total="total1">
        </el-pagination>
      </div>
    </div>

    <div style="font-size: 28px;text-align: left; margin-left: 5%;margin-top: 1em;">
      3.2 碳排放流动趋势
    </div>
    <div>
      <p>
        根据校园资源、能源消耗和废弃物产生情况，通过排放因子法，计算各排放源的排放量。{{ showYear
        }}年北京林业大学碳排放总体情况见表2，各排放源碳排放量见表3和图3，碳排放构成见图4，碳排放变化趋势（按排放类型/排放源）见图5。
      </p>
    </div>
    <div style="margin: 25px auto;">
      <span style="font-size: 22px;">表2 {{ showYear }}年北京林业大学总体碳排放情况</span>
    </div>
    <div style="margin: 25px auto;margin-top: 13px;margin-bottom: 60px;">
      <table width="500px" height="200px" class="table_two" border="1px" style="margin: 0 auto;">
        <thead height="50px" style="text-align: center;background-color: whitesmoke"> <!--表格的头部-->
          <th>指标</th>
          <!--表格头部的特殊单元格，默认有加粗效果-->
          <th>CO<sub>2</sub>排放量（tCO₂）</th>
        </thead>
        <tbody style="text-align: center;">
          <!--tbody是一个语义化标签，没有任何作用，只是告诉你这部分是表格的身体-->
          <tr> <!--第一行的三个单元格： 单元格1  单元格2  单元格3-->
            <td style="background-color: whitesmoke;">&nbsp;&nbsp;&nbsp;总量&nbsp;&nbsp;&nbsp;</td>
            <td>{{ ((parseFloat(carbonInformation.totalEmission || 0) / 1000).toFixed(2)) }}</td>
          </tr>
          <tr> <!--第二行的三个单元格：单元格4 单元格5 单元格6-->
            <td style="background-color: whitesmoke">&nbsp;&nbsp;&nbsp;人均碳排放量&nbsp;&nbsp;&nbsp;</td>
            <td>{{ ((parseFloat(carbonInformation.personAverageEmission || 0) / 1000).toFixed(2)) }}</td>
          </tr>
          <tr>
            <td style="background-color: whitesmoke;">&nbsp;&nbsp;&nbsp;地均碳排放量&nbsp;&nbsp;&nbsp;</td>
            <td>{{ ((parseFloat(carbonInformation.groundAverageEmission || 0) / 1000).toFixed(2)) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <span style="font-size: 22px;">表3 {{ showYear }}年北京林业大学各排放源碳排放情况</span>
    <div style="width: 70%; overflow: auto;margin: 20px auto;">
      <el-table :data="tableData3" border class="forest-table" :span-method="rowMergedMethod">
        <el-table-column prop="emissionType" label="排放类型" min-width="120" align="center"></el-table-column>
        <el-table-column prop="name" label="名称" min-width="200" align="center"></el-table-column>
        <el-table-column prop="emissionAmount" label="碳排放量/tCO2" min-width="150" align="center">
          <template slot-scope="scope">
            {{ ((scope.row.emissionAmount || 0)).toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>
      <!-- 换页菜单 -->
      <div style="text-align:center; margin:20px;padding-right: 50px;margin-bottom: 40px;">
        <el-pagination background @current-change="handleCurrentChange2" :page-size="limit2"
          :current-page.sync="current2" layout="total, prev, pager, next" :total="total2">
        </el-pagination>
      </div>
    </div>
    <div class="carbonup forest-card"
      style="margin-top:25px;width:100%;max-width:650px;height:420px;margin: 0 auto;">
      <div class="forest-card-header">
        <i class="el-icon-s-data"></i>
        <span>{{ showYear }}年各排放源CO<sub>2</sub>排放量</span>
      </div>
      <div id="myChart4" style="width:97%;height:360px;float:left;padding-left:20px;margin-top: 10px;"></div>
    </div>

    <div style="margin: 25px auto;margin-top: 20px;margin-bottom: 60px;">
      <span style="font-size: 22px;">图3 {{ showYear }}年北京林业大学各排放源碳排放情况</span>
    </div>

    <div class="bingtu forest-card" style="width: 100%; max-width: 600px; height: 480px; margin: 0 auto;">
      <div class="forest-card-header">
        <i class="el-icon-pie-chart"></i>
        <span>{{ showYear }}年CO<sub>2</sub>排放构成</span>
      </div>
      <div id="myChart1" style="text-align: center;width:100%;height:420px;">
      </div>
    </div>
    <div style="margin: 25px auto;margin-top: 20px;margin-bottom: 60px;">
      <span style="font-size: 22px;">图4 {{ showYear }}年北京林业大学碳排放构成</span>
    </div>

    <div class="duijizhu forest-card" style="width: 100%; max-width: 650px; height: 800px; margin: 0 auto;">
      <div class="forest-card-header">
        <i class="el-icon-s-data"></i>
        <span>{{ this.duijizhu.startYear }}年{{ this.duijizhu.startMonth }}月&nbsp;&nbsp;~&nbsp;&nbsp;{{ this.duijizhu.endYear }}年{{ this.duijizhu.endMonth }}月CO<sub>2</sub>排放趋势</span>
      </div>
      <div class="chooseBtn" style="width: 100%;margin-top: 15px;padding: 0 10px;box-sizing: border-box;">
        <div class="flex-box-headerr" style="margin-left:0">
          <el-form>
            <div style="display: flex;align-items: center;justify-content: space-between;flex-wrap: wrap;gap: 8px;">
              <div style="display: flex;align-items: center;flex-wrap: wrap;gap: 8px;">
                <el-tag size="small">起始时间：</el-tag>
                <el-date-picker style="width: 90px;" class="buildingleft" v-model="duijizhu.showStartYear" type="year"
                  value-format='yyyy' placeholder="选择年" size="small">
                </el-date-picker>

                <el-select v-model="duijizhu.showStartMonth" placeholder="选择月" style="width: 90px;" @change="changeMe()" size="small">
                  <el-option v-for="item in 12" :key="item" :label="item" :value="item">
                  </el-option>
                </el-select>
                <span style="margin: 0 5px;">~</span>
                <el-tag size="small">结束时间：</el-tag>
                <el-date-picker style="width: 90px;" class="buildingleft" v-model="duijizhu.showEndYear" type="year"
                  value-format='yyyy' placeholder="选择年" size="small">
                </el-date-picker>
                <el-select v-model="duijizhu.showEndMonth" placeholder="选择月" style="width: 90px;" size="small">
                  <el-option v-for="item in 12" :key="item" :label="item" :value="item">
                  </el-option>
                </el-select>
              </div>
              <div style="display: flex;align-items: center;gap: 8px;">
                <el-button class="info" size="small" @click="chageTypeDuijizhu(0)" plain>物品类别</el-button>
                <el-button class="info" size="small" @click="chageTypeDuijizhu(1)" plain>排放类型</el-button>
              </div>
            </div>
          </el-form>
        </div>
      </div>
      <div id="myduijiBar" style="width:95%;height:620px;margin-left:20px;margin-top: 10px;">
      </div>
    </div>
    <div style="margin: 25px auto; margin-top: 40px;">
      <span style="font-size: 22px;">图5 {{ showYear }}年近两年北京林业大学碳排放变化趋势（选择排放类型/排放源）</span>
    </div>

    <!-- <span style="font-size: 22px;">图7 {{ showYear }}年北京林业大学碳排放变化趋势（按排放源）</span> -->
    <div style="font-size: 30px;text-align: left; margin-left: 5%;margin-top: 1em;">
      四、结论
    </div>
    <div>
      <p>根据核算结果，{{ showYear }}年北京林业大学碳排放量为{{
              parseFloat(carbonInformation.totalEmission || 0).toFixed(2) == 0.0 ? 0.0 :
                (parseFloat(carbonInformation.totalEmission || 0) / 1000).toFixed(2)
            }} tCO₂，人均碳排放量为{{
                (parseFloat(carbonInformation.personAverageEmission || 0) / 1000).toFixed(2) }} tCO₂，其中最大的5个排放源依次是
        "{{ (emissionSource1 || '') }}"（{{
              parseFloat(emissionSourceAmount1 || 0).toFixed(2) == 0.0 ? 0.0 :
                (parseFloat(emissionSourceAmount1 || 0) / 1000).toFixed(2)
            }}
        tCO₂）、
        "{{ (emissionSource2 || '') }}"（{{
              parseFloat(emissionSourceAmount2 || 0).toFixed(2) == 0.0 ? 0.0 :
                (parseFloat(emissionSourceAmount2 || 0) / 1000).toFixed(2)
            }}
        tCO₂）、
        "{{ (emissionSource3 || '') }}"（{{
              parseFloat(emissionSourceAmount3 || 0).toFixed(2) == 0.0 ? 0.0 :
                (parseFloat(emissionSourceAmount3 || 0) / 1000).toFixed(2)
            }}
        tCO₂）、
        "{{ (emissionSource4 || '') }}"（{{
              parseFloat(emissionSourceAmount4 || 0).toFixed(2) == 0.0 ? 0.0 :
                (parseFloat(emissionSourceAmount4 || 0) / 1000).toFixed(2)
            }}
        tCO₂）、
        "{{ (emissionSource5 || '') }}"（{{
              parseFloat(emissionSourceAmount5 || 0).toFixed(2) == 0.0 ? 0.0 :
                (parseFloat(emissionSourceAmount5 || 0) / 1000).toFixed(2)
            }}
        tCO₂）。
        未来校园碳减排应围绕以上5个排放源重点展开。直接排放量、间接排放量和其他排放量分别为
        {{ parseFloat(carbonInformation.totalEmission || 0).toFixed(2) == 0.0 ? 0.0 :
              (parseFloat(zhijieEmissionAmount || 0) / 1000).toFixed(2) }}
        tCO₂、
        {{ parseFloat(carbonInformation.totalEmission || 0).toFixed(2) == 0.0 ? 0.0 :
              (parseFloat(jianjieEmissionAmount || 0) / 1000).toFixed(2) }}
        tCO₂
        {{ parseFloat(carbonInformation.totalEmission || 0).toFixed(2) == 0.0 ? 0.0 :
              (parseFloat(qitaEmissionAmount || 0) / 1000).toFixed(2)
        }}
        排放量最高的月份是{{ parseFloat(carbonInformation.totalEmission || 0).toFixed(2) == 0.0 ? "" : maxEmissionMonth }}月（{{
              parseFloat(carbonInformation.totalEmission || 0).toFixed(2) == 0.0 ? 0.0 :
                (parseFloat(maxEmissionMonthAmount || 0) / 1000).toFixed(2) }}
        tCO₂），
        排放量最低的月份是{{ parseFloat(carbonInformation.totalEmission || 0).toFixed(2) == 0.0 ? "" : minEmissionMonth }}月（{{
              parseFloat(carbonInformation.totalEmission || 0).toFixed(2) == 0.0 ? 0.0 :
                (parseFloat(minEmissionMonthAmount || 0) / 1000).toFixed(2) }}
        tCO₂）。</p>
    </div>
    </div>
    <!-- PDF预览容器结束 -->
  </div>
</template>
<script>
import * as echarts from 'echarts/core';
import { SankeyChart } from 'echarts/charts';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([SankeyChart, CanvasRenderer]);
import MyPie from "../../components/Assessment";
import TanHeader from "../../components/TanHeader";
import {
  getJunzhi,
  getCarbonMulberry,
  getDuijizhuCategory,
  getDuijizhuType,
  getBingtu,
  createExportTask,
  getExportTaskStatus,
  downloadExportTask,
} from "../../api/carbonEmission";
import { getAllCategory, getAllExchangeSetting } from "../../api/exchangeSetting";
import { getSchoolInfo } from "../../api/school";
import request from "../../utils/request";
export default {
  components: { TanHeader, MyPie },
  name: "",
  data() {
    return {
      showYear: "", // 初始为空，让后端自动回退查找
      tableData1: [],
      current1: 1,
      limit1: 10,
      total1: 0,
      carbonInformation: {
        totalEmission: '',
        personAverageEmission: '',
        groundAverageEmission: '',
      },

      tableData3: [],
      emissionTypeInformation: [],
      current2: 1,
      limit2: 10,
      total2: 0,
      zhijieRows: 1,
      jianjieRows: 1,
      qitaRows: 1,

      myChart4: '',
      xxValue: [],
      yyValue: [],

      myChart1: '',

      ms: 1,
      mse: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
      searchType: '',//选择查询类型
      duijizhu: {
        showStartYear: '',
        showStartMonth: '',
        showEndYear: '',
        showEndMonth: '',
        startYear: '2023',
        startMonth: '1',
        endYear: '2023',
        endMonth: '12',
        xArr: [],
        series: [],
      },

      emissionSource1: '',
      emissionSourceAmount1: '',
      emissionSource2: '',
      emissionSourceAmount2: '',
      emissionSource3: '',
      emissionSourceAmount3: '',
      emissionSource4: '',
      emissionSourceAmount4: '',
      emissionSource5: '',
      emissionSourceAmount5: '',
      zhijieEmissionAmount: '',
      jianjieEmissionAmount: '',
      qitaEmissionAmount: '',
      maxEmissionMonthAmount: '',
      maxEmissionMonth: '',
      minEmissionMonthAmount: '',
      minEmissionMonth: '',
      categoryList: [], // 碳排放分类列表
      exchangeSettings: [], // 碳排放转化系数列表
      isDoublePageView: false, // 是否为双页视图
      inventoryChart: null, // 图2的图表实例
      exportStatus: true,
      currentTaskId: null, // 当前导出任务ID
      pollingInterval: null, // 轮询定时器
      pollingErrorCount: 0, // 轮询错误次数
      maxPollingErrors: 3, // 最大轮询错误次数，超过后停止轮询
      schoolInfo: {
        schoolName: '',
        totalNumber: null,
        studentNumber: null,
        teacherNumber: null,
        totalArea: null,
        buildingArea: null,
        greenArea: null,
        imageUrl: ''
      },
      emissionSources: [], // 动态排放源列表
      defaultEmissionSources: ['通勤和差旅', '食物', '自来水', '污水', '纸张', '打印', '垃圾', '肥料', '实验室化学品'], // 默认排放源
      // 统一的图表样式变量
      chartStyles: {
        title: {
          fontSize: 14,
          fontWeight: 'normal',
          color: '#333'
        },
        legend: {
          fontSize: 12,
          color: '#666'
        },
        axisLabel: {
          fontSize: 11,
          color: '#666'
        },
        tooltip: {
          fontSize: 12,
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#ccc',
          borderWidth: 1,
          textStyle: {
            color: '#333'
          }
        },
        barWidth: '30%',
        pieLabel: {
          fontSize: 11,
          color: '#333'
        }
      }
    }
  },
  mounted: function () {
    this.loadCategoryList(); // 加载分类列表
    this.loadSchoolInfo(); // 加载学校信息
    this.loadExchangeSettings(); // 加载碳排放转化系数列表
    this.getExportData();
    const date = new Date();
  },
  methods: {
    toggleViewMode() {
      this.isDoublePageView = !this.isDoublePageView;
    },
    loadSchoolInfo() {
      getSchoolInfo().then(res => {
        if (res.data.code == 200) {
          this.schoolInfo = res.data.data || {};
        } else {
          this.$message.error(res.data.message || "获取学校信息失败！");
        }
      }).catch((error) => {
        if (!error.response || !error.response.data || error.response.data.code !== 40101) {
          this.$message.error("获取学校信息失败！");
        }
      });
    },
    calculateMonths(startYear, startMonth, endYear, endMonth) {
      const result = []; this.year
      // console.log("startYear", startYear, "  endYear", endYear, "  startMonth", startMonth, "  endMonth", endMonth)
      // 计算总共的月份数
      const totalMonths = (Number(endYear) - Number(startYear)) * 12 + (Number(endMonth) - Number(startMonth)) + 1;

      // 逐个生成年月字符串
      for (let i = 0; i < totalMonths; i++) {
        const currentYear = Number(startYear) + Math.floor((Number(startMonth) + i - 1) / 12);
        const currentMonth = ((Number(startMonth) + i - 1) % 12) + 1;
        const monthString = `${currentYear}年${currentMonth}月`;
        result.push(monthString);
      }
      return result;
    },
    handleCurrentChange(val) {
      this.current1 = val
      this.getTableData1()
    },
    handleCurrentChange2(val) {
      this.current2 = val
      this.tableData3.splice(0, this.tableData3.length)
      let zhijie = 0
      let jianjie = 0
      let qita = 0
      for (let i = (val - 1) * this.limit2; i < ((val - 1) * this.limit2 + this.limit2) && i < this.emissionTypeInformation.length; i++) {
        this.tableData3.push(this.emissionTypeInformation[i])
        if (this.emissionTypeInformation[i].emissionType == "直接排放")
          zhijie++;
        else if (this.emissionTypeInformation[i].emissionType == "间接排放")
          jianjie++;
        else if (this.emissionTypeInformation[i].emissionType == "其他排放")
          qita++;
      }
      this.zhijieRows = zhijie
      this.jianjieRows = jianjie
      this.qitaRows = qita
      //console.log("当前页直接排放数:", zhijie);
      //console.log("当前页间接排放数:", jianjie);
      //console.log("当前页其他排放数:", qita);
      //console.log(this.tableData3);
    },
    getUnit(category) {
      if (!category || !this.exchangeSettings || this.exchangeSettings.length === 0) {
        return "kg";
      }
      
      const setting = this.exchangeSettings.find(item => item.objectCategory === category);
      if (setting && setting.unit) {
        const unit = setting.unit;
        if (unit.includes('/')) {
          const parts = unit.split('/');
          return parts[parts.length - 1];
        }
        return unit;
      }
      
      return "kg";
    },
    getExportData() {
      // 先统一获取年份（通过getTableData2），然后使用这个年份统一请求所有图表
      this.getTableData2().then(() => {
        // 获取到年份后，使用统一的年份请求所有图表
        // 确保showYear有值
        if (!this.showYear || this.showYear === '') {
          const currentYear = new Date().getFullYear();
          this.showYear = currentYear.toString();
        }
        this.getTableData1();
        this.getTableData3();
        this.getChart3();
        this.getChart4();
        // 设置区间图的年份范围
        this.duijizhu.startYear = Number(this.showYear) - 1;
        this.duijizhu.endYear = Number(this.showYear) + 1;
        this.getChart6();
        this.getLastInfo();
        // 生成图2
        this.$nextTick(() => {
          this.generateInventoryChart();
        });
      }).catch(() => {
        // 如果请求失败，使用当前年份
        const currentYear = new Date().getFullYear();
        this.showYear = currentYear.toString();
        this.getTableData1();
        this.getTableData3();
        this.getChart3();
        this.getChart4();
        this.duijizhu.startYear = currentYear - 1;
        this.duijizhu.endYear = currentYear + 1;
        this.getChart6();
        this.getLastInfo();
        // 生成图2
        this.$nextTick(() => {
          this.generateInventoryChart();
        });
      });
    },
    getTableData1() {
      // 如果year为空，不传参数，让后端自动回退查找
      const params = {};
      if (this.showYear && this.showYear !== '') {
        params.year = this.showYear;
      }
      getBingtu(params).then((res) => {
        if (res.data.code == 200) {
          // 如果后端返回了actualYear，更新显示的年份
          if (res.data.data.actualYear && (!this.showYear || this.showYear === '')) {
            this.showYear = res.data.data.actualYear.toString();
          }
          // 处理返回的数据（可能是list字段或直接是数组）
          var dataList = res.data.data.list || res.data.data;
          // 确保 dataList 是数组
          if (Array.isArray(dataList)) {
            this.tableData1 = dataList;
            this.total1 = dataList.length;
          } else {
            // 如果不是数组，设置为空数组
            this.tableData1 = [];
            this.total1 = 0;
          }
        }
      });
    },
    getTableData2() {
      // 如果year为空，不传参数，让后端自动回退查找
      const params = {};
      if (this.showYear && this.showYear !== '') {
        params.year = this.showYear;
      }
      return getJunzhi(params).then(res => {
        if (res.data.code == 200) {
          // 如果后端返回了actualYear，更新显示的年份（统一使用这个年份）
          if (res.data.data.actualYear) {
            this.showYear = res.data.data.actualYear.toString();
          } else if (!this.showYear || this.showYear === '') {
            // 如果没有返回actualYear且当前year为空，使用当前年份
            this.showYear = new Date().getFullYear().toString();
          }
          this.carbonInformation.totalEmission = res.data.data.totalEmission;
          this.carbonInformation.personAverageEmission = res.data.data.personAverageEmission
          this.carbonInformation.groundAverageEmission = res.data.data.groundAverageEmission
        } else {
          // 如果请求失败，使用当前年份
          if (!this.showYear || this.showYear === '') {
            this.showYear = new Date().getFullYear().toString();
          }
        }
      }).catch(() => {
        // 如果请求失败，使用当前年份
        if (!this.showYear || this.showYear === '') {
          this.showYear = new Date().getFullYear().toString();
        }
      });
    },
    getTableData3() {
      this.emissionTypeInformation.splice(0, this.emissionTypeInformation.length)
      // 如果year为空，不传参数，让后端自动回退查找
      const params = {};
      if (this.showYear && this.showYear !== '') {
        params.year = this.showYear;
      }
      getCarbonMulberry(params).then(res => {
        if (res.data.code == 200) {
          // 如果后端返回了actualYear，更新显示的年份
          if (res.data.data.actualYear && (!this.showYear || this.showYear === '')) {
            this.showYear = res.data.data.actualYear.toString();
          }
          // 处理返回的数据（可能是list字段或直接是数组）
          var dataList = res.data.data.list || res.data.data;
          // 确保 dataList 是数组
          if (Array.isArray(dataList)) {
            for (var i = 0; i < dataList.length; i++) {
              for (var j = 0; j < dataList[i].emissionTypeAmount.length; j++) {//一个物品类别可能有多个排放类型(多个流向边)
                var emissionType = '';
                if (dataList[i].emissionTypeAmount[j].emissionType == 0)
                  emissionType = '直接排放'
                else if (dataList[i].emissionTypeAmount[j].emissionType == 1)
                  emissionType = '间接排放'
                else
                  emissionType = '其他排放'

                var emissionAmount = dataList[i].emissionTypeAmount[j].emissionAmount;
                this.emissionTypeInformation.push({ name: dataList[i].objectCategory, emissionType: emissionType, emissionAmount: (emissionAmount || 0) / 1000 })
              }
            }
            this.total2 = this.emissionTypeInformation.length;
            this.emissionTypeInformation.sort((a, b) => (a.emissionType > b.emissionType) ? 1 : -1);
            this.handleCurrentChange2(1)
          } else {
            // 如果不是数组，设置为空数组
            this.tableData3 = [];
            this.total2 = 0;
          }
        }
      })
    },
    rowMergedMethod({ row, column, rowIndex, columnIndex }) {
      // console.log("row",row);
      // console.log("column",column);
      if (columnIndex === 0) {
        if (this.qitaRows > 0 && rowIndex == 0) {
          return {
            rowspan: this.qitaRows,
            colspan: 1
          };
        }
        if (this.qitaRows > 0 && rowIndex > 0 && rowIndex < this.qitaRows) {
          return {
            rowspan: 0,
            colspan: 0
          };
        }
        if (this.zhijieRows > 0 && rowIndex == this.qitaRows) {
          return {
            rowspan: this.zhijieRows,
            colspan: 1
          };
        }
        if (this.zhijieRows > 0 && rowIndex > this.qitaRows && rowIndex < this.qitaRows + this.zhijieRows) {
          return {
            rowspan: 0,
            colspan: 0
          };
        }
        if (this.jianjieRows > 0 && rowIndex == this.zhijieRows + this.qitaRows) {
          return {
            rowspan: this.jianjieRows,
            colspan: 1
          };
        }
        if (this.jianjieRows > 0 && rowIndex > this.qitaRows + this.zhijieRows && rowIndex < this.qitaRows + this.zhijieRows + this.jianjieRows) {
          return {
            rowspan: 0,
            colspan: 0
          };
        }
      }
    },
    getChart3() {
      // 如果year为空，不传参数，让后端自动回退查找
      const params = {};
      if (this.showYear && this.showYear !== '') {
        params.year = this.showYear;
      }
      getBingtu(params).then((res) => {
        if (res.data.code == 200) {
          // 如果后端返回了actualYear，更新显示的年份
          if (res.data.data.actualYear && (!this.showYear || this.showYear === '')) {
            this.showYear = res.data.data.actualYear.toString();
          }
          // 处理返回的数据（可能是list字段或直接是数组）
          var dataList = res.data.data.list || res.data.data;
          // console.log(res.data)
          // console.log(dataList.length)
          var xValueTemp = [];
          var yValueTemp = [];
          for (var i = 0; i < dataList.length; i++) {
            xValueTemp.push(dataList[i].objectCategory);
            yValueTemp.push((dataList[i].emissionAmount || 0) / 1000);
          }
          this.xxValue = xValueTemp;
          this.yyValue = yValueTemp;
          //this.$refs.voteGraph.drawLine(xValueTemp, yValueTemp);
          // console.log(this.xxValue)
          // console.log(this.yyValue)
          this.myChart4 = this.$echarts.init(document.getElementById('myChart4'))
          this.myChart4.clear()
          this.myChart4.setOption({
            title: {
              text: 'CO₂排放量(tCO₂)',
              textStyle: this.chartStyles.title
            },
            tooltip: {
              trigger: 'item',
              formatter: (params) => {
                return '碳排放量' + '<br/>' + params.marker + params.name + ':' + parseFloat(params.value).toFixed(2) + '(tCO₂)' + '<br/>';
              },
              backgroundColor: this.chartStyles.tooltip.backgroundColor,
              borderColor: this.chartStyles.tooltip.borderColor,
              borderWidth: this.chartStyles.tooltip.borderWidth,
              textStyle: this.chartStyles.tooltip.textStyle
            },
            legend: {
              orient: 'vertical',
              x: 'right',
              y: 'top',
              data: ['CO₂排放量'],
              textStyle: this.chartStyles.legend
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              data: this.xxValue,
              axisLabel: {
                interval: 0,
                ...this.chartStyles.axisLabel
              },
            },
            yAxis: {
              axisLabel: this.chartStyles.axisLabel
            },
            series: [{
              name: 'CO₂排放量',
              type: 'bar',
              barWidth: this.chartStyles.barWidth,
              data: this.yyValue,
              itemStyle: {
                emphasis: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                },
                color: function () {
                  return "#" + Math.floor(Math.random() * 16777215).toString(16);
                }
              }
            }]
          }, true)
        }
      }).catch(() => {
      }).finally(() => {
      });

    },
    getChart4() {
      // 如果year为空，不传参数，让后端自动回退查找
      const params = {};
      if (this.showYear && this.showYear !== '') {
        params.year = this.showYear;
      }
      getBingtu(params).then((res) => {
        if (res.data.code == 200) {
          // 如果后端返回了actualYear，更新显示的年份
          if (res.data.data.actualYear && (!this.showYear || this.showYear === '')) {
            this.showYear = res.data.data.actualYear.toString();
          }
          // 处理返回的数据（可能是list字段或直接是数组）
          var dataList = res.data.data.list || res.data.data;
          var xValueTemp = [];
          var yValueTemp = [];
          for (var i = 0; i < dataList.length; i++) {
            xValueTemp.push(dataList[i].objectCategory);
            yValueTemp.push({
              name: dataList[i].objectCategory,
              value: (dataList[i].emissionAmount || 0) / 1000
            });
          }
          this.myChart1 = this.$echarts.init(document.getElementById('myChart1'))
          this.myChart1.clear()
          this.myChart1.setOption({
            title: {
              text: '',
              textStyle: this.chartStyles.title
            },
            tooltip: {
              trigger: 'item',
              formatter: (params) => {
                return '碳排放量' + '<br/>' + params.marker + params.name + ':' + parseFloat(params.value).toFixed(2) + '(tCO₂)' + '<br/>';
              },
              show: true,
              backgroundColor: this.chartStyles.tooltip.backgroundColor,
              borderColor: this.chartStyles.tooltip.borderColor,
              borderWidth: this.chartStyles.tooltip.borderWidth,
              textStyle: this.chartStyles.tooltip.textStyle
            },
            legend: {
              type: 'scroll',
              orient: 'vertical',
              right: 10,
              top: 75,
              bottom: 20,
              data: xValueTemp,
              textStyle: this.chartStyles.legend
            },
            xAxis: {
              type: "category",
              show: false,
            },
            yAxis: {
              type: "value",
            },
            series: [{
              name: '碳排放量占比',
              type: 'pie',
              radius: '75%',
              center: ['43%', '57%'],
              barWidth: '10%',
              data: yValueTemp,
              label: {
                show: true,
                position: 'outside',
                formatter: `{d}%`,
                ...this.chartStyles.pieLabel
              }
            }]
          }, true)
        }
      }).catch(() => {
      }).finally(() => {
      });

    },
    getChart6() {
      if (this.duijizhu.showStartYear == '' && this.duijizhu.showStartMonth == '' && this.duijizhu.showEndYear == '' && this.duijizhu.showEndMonth == '') {
        this.getCarbonBarDuiji();
      }
      else if (!(this.duijizhu.showStartYear != '' && this.duijizhu.showStartYear != null && this.duijizhu.showStartMonth != '' && this.duijizhu.showStartMonth != null
        && this.duijizhu.showEndYear != '' && this.duijizhu.showEndYear != null && this.duijizhu.showEndMonth != '' && this.duijizhu.showEndMonth != null
        && Number(this.duijizhu.showStartMonth) >= 1 && Number(this.duijizhu.showStartMonth) <= 12 && Number(this.duijizhu.showEndMonth) >= 1
        && Number(this.duijizhu.showEndMonth) <= 12 && Number(this.duijizhu.showStartYear) * 12 + Number(this.duijizhu.showStartMonth) <= Number(this.duijizhu.showEndYear) * 12 + Number(this.duijizhu.showEndMonth))) {
        this.$message.error('请输入完整且合理的查询起始年月！');
        this.duijizhu.showStartYear = '';
        this.duijizhu.showStartMonth = '';
        this.duijizhu.showEndYear = '';
        this.duijizhu.showEndMonth = '';
        this.duijizhu.startYear = '2023';
        this.duijizhu.startMonth = '1';
        this.duijizhu.endYear = '2023';
        this.duijizhu.endMonth = '6';
      } else {
        this.duijizhu.startYear = this.duijizhu.showStartYear;
        this.duijizhu.startMonth = this.duijizhu.showStartMonth;
        this.duijizhu.endYear = this.duijizhu.showEndYear;
        this.duijizhu.endMonth = this.duijizhu.showEndMonth;
        this.duijizhu.xArr = this.calculateMonths(this.duijizhu.startYear, this.duijizhu.startMonth, this.duijizhu.endYear, this.duijizhu.endMonth)
        this.getCarbonBarDuiji();
      }
    },
    async getCarbonBarDuiji() {
      this.duijizhu.series = []
      this.duijizhu.xArr = []
      if (this.duijizhu.startYear != '' && this.duijizhu.startYear != null && this.duijizhu.startMonth != '' && this.duijizhu.startMonth != null
        && this.duijizhu.endYear != '' && this.duijizhu.endYear != null && this.duijizhu.endMonth != '' && this.duijizhu.endMonth != null
        && Number(this.duijizhu.startMonth) >= 1 && Number(this.duijizhu.startMonth) <= 12 && Number(this.duijizhu.endMonth) >= 1
        && Number(this.duijizhu.endMonth) <= 12 && Number(this.duijizhu.startYear) * 12 + Number(this.duijizhu.startMonth) <= Number(this.duijizhu.endYear) * 12 + Number(this.duijizhu.endMonth)) {
        this.duijizhu.xArr = this.calculateMonths(this.duijizhu.startYear, this.duijizhu.startMonth, this.duijizhu.endYear, this.duijizhu.endMonth)
      } else {
        this.duijizhu.startYear = '';
        this.duijizhu.startMonth = '';
        this.duijizhu.endYear = '';
        this.duijizhu.endMonth = '';
        this.duijizhu.xArr = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
      }
      // console.log("this.searchType", this.searchType)
      if (this.searchType == '1') {
        await getDuijizhuType({ startYear: this.duijizhu.startYear, startMonth: this.duijizhu.startMonth, endYear: this.duijizhu.endYear, endMonth: this.duijizhu.endMonth }).then((res) => {
          if (res.data.code == 200) {
            this.duijizhu.series = []
            for (var i = 0; i < res.data.data.length; i++) {
              var emissionType = '';
              if (res.data.data[i].emissionType == 0)
                emissionType = '直接排放'
              else if (res.data.data[i].emissionType == 1)
                emissionType = '间接排放'
              else
                emissionType = '其他排放'
              var seriesobj = { //一个seriesobj即为一个物品分类1-12月的柱状图
                name: emissionType,
                type: 'bar',
                barWidth: this.chartStyles.barWidth,
                emphasis: {
                  focus: 'series'
                },
                stack: 'Ad',
                data: [],
                itemStyle: {
                  emphasis: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                  },
                  color: function () {
                    return "#" + Math.floor(Math.random() * 16777215).toString(16);
                  }
                }
              }
              for (var j = 0; j < res.data.data[i].emissionMonthAmount.length; j++) {
                var month = res.data.data[i].emissionMonthAmount[j].time
                var emissionAmount = (res.data.data[i].emissionMonthAmount[j].emissionAmount || 0) / 1000
                var x = [month, emissionAmount]
                seriesobj.data.push(x)
              }
              this.duijizhu.series.push(seriesobj)
            }
          }
        })
      }
      else {
        await getDuijizhuCategory({ startYear: this.duijizhu.startYear, startMonth: this.duijizhu.startMonth, endYear: this.duijizhu.endYear, endMonth: this.duijizhu.endMonth }).then((res) => {
          if (res.data.code == 200) {
            for (var i = 0; i < res.data.data.length; i++) {
              var seriesobj = {    //一个seriesobj即为一个物品分类1-12月的柱状图
                name: res.data.data[i].objectCategory,
                type: 'bar',
                barWidth: this.chartStyles.barWidth,
                stack: 'Ad',
                data: [],
                itemStyle: {
                  emphasis: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                  },
                  color: function () {
                    return "#" + Math.floor(Math.random() * 16777215).toString(16);
                  }
                }
              }
              for (var j = 0; j < res.data.data[i].emissionMonthAmount.length; j++) {
                var month = res.data.data[i].emissionMonthAmount[j].time
                var emissionAmount = (res.data.data[i].emissionMonthAmount[j].emissionAmount || 0) / 1000
                var x = [month, emissionAmount]
                seriesobj.data.push(x)
              }
              this.duijizhu.series.push(seriesobj)
            }
          }
        }).catch(() => {
        }).finally(() => {
        });
      }
      // console.log("this.duijizhu.series：",this.duijizhu.series)
      var chartDom = document.getElementById('myduijiBar');
      var myChart = echarts.init(chartDom);
      myChart.clear();
      var option;

      option = {
        title: {
          text: 'CO₂碳排放量(tCO₂)',
          left: 'left',
          top: 5,
          textStyle: this.chartStyles.title
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: (params) => {
            let res = '碳排放量' + '<br/>'
            for (var i = 0; i < params.length; i++) {
              res += params[i].marker + params[i].seriesName + ':' + parseFloat(params[i].data[1]).toFixed(2) + '(tCO₂)' + '<br/>'
            }
            return res;
          },
          backgroundColor: this.chartStyles.tooltip.backgroundColor,
          borderColor: this.chartStyles.tooltip.borderColor,
          borderWidth: this.chartStyles.tooltip.borderWidth,
          textStyle: this.chartStyles.tooltip.textStyle
        },
        legend: {
          right: 10,
          top: 5,
          orient: 'horizontal',
          textStyle: this.chartStyles.legend
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: 60,
          containLabel: true
        },
        xAxis: [
          {
            axisLabel: {
              margin: 10,
              interval: 0,
              rotate: 60,
              ...this.chartStyles.axisLabel
            },
            type: 'category',
            data: this.duijizhu.xArr,
          }
        ],
        yAxis: [
          {
            type: 'value',
            axisLabel: this.chartStyles.axisLabel
          }
        ],
        series: this.duijizhu.series,
        dataZoom: [
          {
            type: 'slider',
            show: true,
            start: 0,
            end: 70,
            height: 12,
            realtime: true,
          },
        ],
      };

      option && myChart.setOption(option, true);
    },
    chageTypeDuijizhu(chooseType) {
      this.searchType = chooseType
      this.getChart6()
    },
    getLastInfo() {
      // 如果year为空，不传参数，让后端自动回退查找
      const params = {};
      if (this.showYear && this.showYear !== '') {
        params.year = this.showYear;
      }
      request.get("/api/carbonEmission/exportReportDataByYear", { params: params }).then((res) => {
        if (res.data.code == 200) {
          // 如果后端返回了year，更新显示的年份
          if (res.data.data.year && (!this.showYear || this.showYear === '')) {
            this.showYear = res.data.data.year.toString();
          }
          // console.log("res.data.data", res.data.data);
          this.emissionSource1 = res.data.data.EmissionName1;
          this.emissionSourceAmount1 = res.data.data.EmissionNumber1;
          this.emissionSource2 = res.data.data.EmissionName2;
          this.emissionSourceAmount2 = res.data.data.EmissionNumber2;
          this.emissionSource3 = res.data.data.EmissionName3;
          this.emissionSourceAmount3 = res.data.data.EmissionNumber3;
          this.emissionSource4 = res.data.data.EmissionName4;
          this.emissionSourceAmount4 = res.data.data.EmissionNumber4;
          this.emissionSource5 = res.data.data.EmissionName5;
          this.emissionSourceAmount5 = res.data.data.EmissionNumber5;
          this.zhijieEmissionAmount = res.data.data.dirEmiNumber;
          this.jianjieEmissionAmount = res.data.data.indEmiNumber;
          this.qitaEmissionAmount = res.data.data.otherEmiNumber;
          this.maxEmissionMonth = res.data.data.maxMonth;
          this.maxEmissionMonthAmount = res.data.data.maxEmi;
          this.minEmissionMonth = res.data.data.minMonth;
          this.minEmissionMonthAmount = res.data.data.minEmi;
          
          // 如果排放源名称为空，从categoryList中获取
          this.fillMissingEmissionSources();
        }
      }).catch(() => {
      }).finally(() => {
      });
    },
    exportReportByYear() {
      if (this.showYear == null || this.showYear == '') {
        this.$message({
          type: 'error',
          message: '选择年份为空'
        })
        return;
      }
      if (parseFloat(this.carbonInformation.totalEmission).toFixed(2) == 0.0) {
        this.$notify({
          title: '导出失败',
          message: '当前年份尚无数据',
          type: 'warning',
          offset: 100
        });
        return;
      }
      if (this.exportStatus) { // 为事件加锁，点击之后，在动作没有完成之前不允许再次触发事件
        this.exportStatus = false
        if (this.msg) {	// 防止第一次点击报错
          this.msg.close()
        }
        this.msg = this.$message({
          duration: 0,
          type: 'warning',
          message: '正在导出....'
        })
        request.get("/api/carbonEmission/exportReportByYear", { params: { year: this.showYear }, responseType: 'blob' }).then((res) => {
          if (res.data != null) {
            this.msg.close()  // 这样才能正确关闭
            this.msg = this.$message({
              duration: 1000,
              type: 'success',
              message: '导出成功'
            })
            this.exportStatus = true
            let link = document.createElement("a");
            let blob = new Blob([res.data], {
              type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document;charset=UTF-8",
            });
            link.style.display = "none";
            link.href = URL.createObjectURL(blob);
            link.download = "北京林业大学" + this.showYear + "年碳排放报告"; //下载的文件名
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
          }
        }).catch((error) => {
          this.exportStatus = true
          this.$message.error("下载失败");
        });
      }
    },
    // 加载分类列表
    loadCategoryList() {
      getAllCategory().then(res => {
        if (res.data && Array.isArray(res.data)) {
          this.categoryList = res.data;
        } else if (res.data && res.data.code === 200 && Array.isArray(res.data.data)) {
          this.categoryList = res.data.data;
        }
        
        // 提取排放源名称
        this.emissionSources = this.categoryList.map(cat => cat.objectCategory || cat.name || '').filter(name => name.trim() !== '');
        
      }).catch(() => {
        this.$message.error("获取分类列表失败！");
        this.categoryList = [];
        this.emissionSources = [];
      });
    },
    // 加载碳排放转化系数列表
    loadExchangeSettings() {
      getAllExchangeSetting({ current: 1, size: 1000 }).then(res => {
        if (res.data && Array.isArray(res.data.records)) {
          this.exchangeSettings = res.data.records;
        } else if (res.data && Array.isArray(res.data)) {
          this.exchangeSettings = res.data;
        } else {
          this.exchangeSettings = [];
        }
      }).catch(() => {
        this.$message.error("获取碳排放转化系数失败！");
        this.exchangeSettings = [];
      });
    },
    // 填充缺失的排放源名称
    fillMissingEmissionSources() {
      const sources = [this.emissionSource1, this.emissionSource2, this.emissionSource3, this.emissionSource4, this.emissionSource5];
      const amounts = [this.emissionSourceAmount1, this.emissionSourceAmount2, this.emissionSourceAmount3, this.emissionSourceAmount4, this.emissionSourceAmount5];
      
      // 找出所有非空的排放源名称
      const existingSources = sources.filter(s => s && s.trim() !== '');
      
      // 找出categoryList中不在existingSources中的分类名称
      const missingSources = this.categoryList
        .map(cat => cat.objectCategory || cat.name || '')
        .filter(name => name.trim() !== '' && !existingSources.includes(name));
      
      // 填充空的排放源名称
      let missingIndex = 0;
      if (!this.emissionSource1 || this.emissionSource1.trim() === '') {
        this.emissionSource1 = missingSources[missingIndex++] || '';
      }
      if (!this.emissionSource2 || this.emissionSource2.trim() === '') {
        this.emissionSource2 = missingSources[missingIndex++] || '';
      }
      if (!this.emissionSource3 || this.emissionSource3.trim() === '') {
        this.emissionSource3 = missingSources[missingIndex++] || '';
      }
      if (!this.emissionSource4 || this.emissionSource4.trim() === '') {
        this.emissionSource4 = missingSources[missingIndex++] || '';
      }
      if (!this.emissionSource5 || this.emissionSource5.trim() === '') {
        this.emissionSource5 = missingSources[missingIndex++] || '';
      }
    },
    // 生成图2：碳排放核算范围及清单
    generateInventoryChart() {
      this.$nextTick(() => {
        const chartDom = document.getElementById('inventoryChart');
        if (!chartDom) {
          console.error('图2容器不存在');
          return;
        }
        
        // 如果图表已存在，先销毁
        if (this.inventoryChart) {
          this.inventoryChart.dispose();
        }
        
        this.inventoryChart = echarts.init(chartDom);
        
        // 获取实际的排放关系数据，类似TanResult.vue的处理
        const params = {};
        if (this.showYear && this.showYear !== '') {
          params.year = this.showYear;
        }
        
        getCarbonMulberry(params).then(res => {
          if (res.data.code == 200) {
            // 处理返回的数据（可能是list字段或直接是数组）
            var dataList = res.data.data.list || res.data.data;
            
            // 构建桑基图数据
            const sangShen = {
              data: [],//桑基图节点数据列表
              links: [],//节点间的边
            };
            
            // 添加根节点
            var rootNode = { 
              name: '校园碳排放核算范围及清单',
              itemStyle: {
                color: '#4a7c3a' // 设置根节点颜色
              },
              label: {
                fontSize: 16, // 增大字体
                fontWeight: 'bold', // 加粗
                color: '#2d5016', // 设置标签颜色
                position: 'right', // 设置标签位置在右侧
                offset: [-25, 0] // 最左侧根节点文字向右偏移，确保文字完整显示
              }
            };
            sangShen.data.push(rootNode);
            
            // 收集所有分类名称
            const categoryNames = new Set();
            for (var i = 0; i < dataList.length; i++) {
              categoryNames.add(dataList[i].objectCategory);
            }
            
            // 添加分类节点
            for (const category of categoryNames) {
              sangShen.data.push({ name: category });
              // 从根节点连接到分类
              sangShen.links.push({
                source: '校园碳排放核算范围及清单',
                target: category,
                value: 1 // 使用固定值，实际数据会在分类到排放类型的连接中体现
              });
            }
            
            // 添加排放类型节点
            var databoj1 = { name: '直接排放' };
            var databoj2 = { name: '间接排放' };
            var databoj3 = { name: '其他排放' };
            sangShen.data.push(databoj1);
            sangShen.data.push(databoj2);
            sangShen.data.push(databoj3);
            
            // 处理分类到排放类型的连接关系
            for (var i = 0; i < dataList.length; i++) {
              for (var j = 0; j < dataList[i].emissionTypeAmount.length; j++) {
                //一个物品类别可能有多个排放类型(多个流向边)
                var emissionType = '';
                if (dataList[i].emissionTypeAmount[j].emissionType == 0) {
                  emissionType = '直接排放';
                } else if (dataList[i].emissionTypeAmount[j].emissionType == 1) {
                  emissionType = '间接排放';
                } else {
                  emissionType = '其他排放';
                }
                
                var link = {
                  source: dataList[i].objectCategory,
                  target: emissionType,
                  value: dataList[i].emissionTypeAmount[j].emissionAmount
                };
                sangShen.links.push(link);
              }
            }
            
            const option = {
              tooltip: {
                trigger: 'item',
                triggerOn: 'mousemove',
                formatter: (params) => {
                  var name = params.name.indexOf('>') != -1 ? (params.name.split('>')[0] + '-' + params.name.split('>')[1]) : params.name;
                  return '碳排放量' + '<br/>' + name + ' : ' + parseFloat(params.value).toFixed(2) + '(kg)' + '<br/>';
                },
                backgroundColor: this.chartStyles.tooltip.backgroundColor,
                borderColor: this.chartStyles.tooltip.borderColor,
                borderWidth: this.chartStyles.tooltip.borderWidth,
                textStyle: this.chartStyles.tooltip.textStyle
              },
              series: [
                {
                  type: 'sankey',
                  layout: 'none',
                  emphasis: {
                    focus: 'adjacency'
                  },
                  left: '5%', // 增大左侧边距，为根节点文字留出更多空间
                  //right: '5%',
                  // top: '5%',
                  // bottom: '5%',
                  data: sangShen.data,
                  links: sangShen.links,
                  lineStyle: {
                    color: 'gradient',
                    curveness: 0.5
                  },
                  label: {
                    fontSize: this.chartStyles.legend.fontSize,
                    color: this.chartStyles.legend.color
                  }
                }
              ]
            };
            
            this.inventoryChart.setOption(option);
          }
        }).catch(error => {
          console.error('获取排放关系数据失败:', error);
        });
        
        // 响应式调整
        window.addEventListener('resize', () => {
          if (this.inventoryChart) {
            this.inventoryChart.resize();
          }
        });
      });
    },
    // 通用导出方法（异步导出）
    exportReport(format) {
      // 数据验证
      if (parseFloat(this.carbonInformation.totalEmission || 0).toFixed(2) == 0.0) {
        this.$notify({
          title: '导出失败',
          message: '当前年份尚无数据',
          type: 'warning',
          offset: 100
        });
        return;
      }
      
      // 防止重复点击
      if (!this.exportStatus) {
        return;
      }
      
      this.exportStatus = false;
      if (this.msg) {
        this.msg.close();
      }
      
      // 根据格式显示不同的加载消息
      const formatName = format === 'pdf' ? 'PDF' : 'Word';
      this.msg = this.$message({
        duration: 0,
        type: 'info',
        message: `正在创建${formatName}导出任务...`
      });
      
      // 构建请求参数
      const params = {
        format: format // 指定导出格式
      };
      if (this.showYear && this.showYear !== '') {
        params.year = this.showYear;
      }
      
      // 创建异步导出任务
      createExportTask(params).then((res) => {
        console.log('创建导出任务响应:', res);
        if (res.data && res.data.code === 200 && res.data.data && res.data.data.taskId) {
          const taskId = res.data.data.taskId;
          console.log('任务ID:', taskId);
          this.currentTaskId = taskId;
          
          // 更新消息
          this.msg.close();
          this.msg = this.$message({
            duration: 0,
            type: 'info',
            message: `${formatName}文档正在生成中，请稍候...`
          });
          
          // 开始轮询查询任务状态
          this.startPollingTaskStatus(taskId, format, formatName);
        } else {
          this.exportStatus = true;
          this.msg.close();
          const errorMsg = res.data && res.data.message ? res.data.message : "创建导出任务失败";
          console.error('创建导出任务失败，响应:', res);
          this.$message.error(errorMsg);
        }
      }).catch((error) => {
        this.exportStatus = true;
        this.msg.close();
        console.error(`创建导出任务失败:`, error);
        this.$message.error("创建导出任务失败：" + (error.message || "未知错误"));
      });
    },
    
    // 开始轮询查询任务状态
    startPollingTaskStatus(taskId, format, formatName) {
      // 清除之前的轮询
      if (this.pollingInterval) {
        clearInterval(this.pollingInterval);
        this.pollingInterval = null;
      }
      
      // 重置错误计数
      this.pollingErrorCount = 0;
      
      // 立即查询一次
      this.checkTaskStatus(taskId, format, formatName);
      
      // 设置轮询，每2秒查询一次
      this.pollingInterval = setInterval(() => {
        this.checkTaskStatus(taskId, format, formatName);
      }, 2000);
    },
    
    // 查询任务状态
    checkTaskStatus(taskId, format, formatName) {
      getExportTaskStatus(taskId).then((res) => {
        console.log('查询任务状态响应:', res);
        
        if (res.data && res.data.code === 200 && res.data.data) {
          // 重置错误计数（查询成功）
          this.pollingErrorCount = 0;
          
          const taskData = res.data.data;
          const status = taskData.status;
          console.log('任务状态:', status);
          
          if (status === 'COMPLETED') {
            // 任务完成，停止轮询并下载文件
            this.stopPolling();
            this.downloadTaskFile(taskId, taskData.fileName, format, formatName);
          } else if (status === 'FAILED') {
            // 任务失败，停止轮询并显示错误
            this.stopPolling();
            this.exportStatus = true;
            if (this.msg) {
              this.msg.close();
            }
            this.$message.error(`导出失败：${taskData.errorMessage || '未知错误'}`);
          } else if (status === 'PROCESSING') {
            // 任务处理中，更新消息（避免重复弹窗）
            if (!this.msg || this.msg.message !== `${formatName}文档正在生成中，请稍候...`) {
              if (this.msg) {
                this.msg.close();
              }
              this.msg = this.$message({
                duration: 0,
                type: 'info',
                message: `${formatName}文档正在生成中，请稍候...`
              });
            }
          }
          // PENDING状态不需要特殊处理，继续轮询即可
        } else if (res.data && res.data.code !== 200) {
          // 任务不存在或已过期
          this.pollingErrorCount++;
          console.warn(`任务查询失败 (${this.pollingErrorCount}/${this.maxPollingErrors}):`, res.data);
          
          // 只弹窗一次，超过最大错误次数后停止轮询
          if (this.pollingErrorCount >= this.maxPollingErrors) {
            const errorMsg = res.data.message || res.data.description || "任务不存在或已过期";
            this.stopPolling();
            this.exportStatus = true;
            if (this.msg) {
              this.msg.close();
            }
            this.$message.error(errorMsg);
          }
        } else {
          // 响应格式异常
          this.pollingErrorCount++;
          console.warn(`响应格式异常 (${this.pollingErrorCount}/${this.maxPollingErrors}):`, res);
          
          if (this.pollingErrorCount >= this.maxPollingErrors) {
            this.stopPolling();
            this.exportStatus = true;
            if (this.msg) {
              this.msg.close();
            }
            this.$message.error("查询任务状态失败：响应格式异常");
          }
        }
      }).catch((error) => {
        console.error('查询任务状态失败:', error);
        // 网络错误时，累计错误次数
        this.pollingErrorCount++;
        if (this.pollingErrorCount >= this.maxPollingErrors) {
          this.stopPolling();
          this.exportStatus = true;
          if (this.msg) {
            this.msg.close();
          }
          this.$message.error("查询任务状态失败，请稍后重试");
        }
      });
    },
    
    // 停止轮询
    stopPolling() {
      if (this.pollingInterval) {
        clearInterval(this.pollingInterval);
        this.pollingInterval = null;
      }
      this.currentTaskId = null;
    },
    
    // 下载任务文件
    downloadTaskFile(taskId, fileName, format, formatName) {
      downloadExportTask(taskId).then((res) => {
        if (res.data != null) {
          this.msg.close();
          this.msg = this.$message({
            duration: 2000,
            type: 'success',
            message: `${formatName}文档导出成功`
          });
          this.exportStatus = true;
          
          // 根据格式设置正确的MIME类型和文件扩展名
          const mimeType = format === 'pdf' ? "application/pdf;charset=UTF-8" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document;charset=UTF-8";
          
          // 创建下载链接
          let link = document.createElement("a");
          let blob = new Blob([res.data], {
            type: mimeType,
          });
          link.style.display = "none";
          link.href = URL.createObjectURL(blob);
          link.download = fileName || `北京林业大学${this.showYear || new Date().getFullYear()}年度碳排放报告.${format}`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          
          // 释放URL对象
          URL.revokeObjectURL(link.href);
        } else {
          this.exportStatus = true;
          this.msg.close();
          this.$message.error("下载文件失败：服务器返回数据为空");
        }
      }).catch((error) => {
        this.exportStatus = true;
        this.msg.close();
        console.error(`下载文件失败:`, error);
        this.$message.error("下载文件失败：" + (error.message || "未知错误"));
      });
    },
    // 导出PDF
    exportToPDF() {
      this.exportReport('pdf');
    },
    // 导出Word
    exportToWord() {
      this.exportReport('docx');
    },
  },
  // 组件销毁时清理轮询
  beforeDestroy() {
    this.stopPolling();
    if (this.msg) {
      this.msg.close();
    }
  },
}
</script>
<style scoped>
p {
  font-size: 22px;
  padding-left: 6.5%;
  text-align: left;
  text-indent: 2em;
}

.bottom {
  border-top: 0;
  /* box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04); */
}

.hr-shadow {
  margin-top: 50px;
  padding-top: 10px;
  border-left: 0;
  border-right: 0;
  border-bottom: 0;
  color: #d0d0d5;
  border-top: 10px solid rgba(0, 0, 0, .1);
  box-shadow: inset 0 10px 10px -10px;
}

.tanResult {
  width: 100%;
  background-color: var(--forest-bg-primary);
  text-align: left;
  margin: 0;
  margin-top: 10px;
  padding: 20px 20px 0 20px;
  box-sizing: border-box;
  min-height: calc(100vh - 100px);
}

.tanExport.tanResult {
  text-align: center;
}

.forest-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(45, 80, 22, 0.1);
  overflow: hidden;
  background: #fff;
}

.forest-card:hover {
  box-shadow: 0 4px 16px rgba(45, 80, 22, 0.15);
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

/* PDF预览样式 */
.export-controls {
  background: #fff;
  width: 210mm;
  padding: 15px 20px;
  margin: 0 auto 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  box-sizing: border-box;
}

.pdf-preview-container {
  background: #fff;
  width: 210mm; /* A4宽度 */
  min-height: 297mm; /* A4高度 */
  margin: 0 auto;
  padding: 20mm;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
}

.double-page-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
  padding: 20px;
  background: #f5f5f5;
}

.a4-page {
  width: 210mm;
  min-height: 297mm;
  padding: 20mm;
  background: white;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.15);
  box-sizing: border-box;
  page-break-after: always;
  overflow: hidden;
}

.exportTitle {
  font-family: "SimSun", "宋体", serif;
  line-height: 1.6;
  color: #333;
}

.exportTitle p {
  text-indent: 2em;
  margin: 10px 0;
}

/* 打印样式 */
@media print {
  .export-controls {
    display: none;
  }
  
  .pdf-preview-container {
    width: 100%;
    min-height: auto;
    margin: 0;
    padding: 0;
    box-shadow: none;
  }
  
  .tanExport {
    background: #fff;
  }
}

.flex-box-headerr {
  margin-left: 20px;
  margin-right: 15px;
  margin-top: 15px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.junzhi,
.zhuzhuangtu {
  display: inline-block;

}

.junzhi {
  width: 370px;
  margin-right: 10px;

}

.zhuzhuangtu {
  display: inline-block;
  margin-top: 20px;
  width: 100%;
  max-width: 650px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04);
  margin-bottom: 10px;
}

.duijizhu {
  display: inline-block;
  width: 100%;
  max-width: 650px;
  height: 700px;
  margin: 0 auto;
}

.bingtu {
  display: inline-block;
  width: 100%;
  max-width: 600px;
  height: 480px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04);
  margin: 0 auto;
}
</style>
<style>
.el-input__inner {
  height: 32.5px;
}
</style>
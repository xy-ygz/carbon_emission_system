<template>
  <div class="tanResult">
    <div class="main-box-result">
      <!-- 筛选条件区域 -->
      <div class="forest-card filter-section">
      <div class="forest-card-header">
        <i class="el-icon-search"></i>
        <span>查询条件</span>
      </div>
      <div class="filter-content">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="选择年份：">
            <el-date-picker v-model="showYear" type="year" value-format='yyyy'
              placeholder="选择年" size="small" class="filter-input">
            </el-date-picker>
          </el-form-item>
          <el-form-item label="选择月份：">
            <el-select v-model="showMonth" placeholder="选择月" size="small" class="filter-input">
              <el-option v-for="item in 12" :key="item" :label="item" :value="item">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button size="small" @click="refreshCharts()" type="primary" icon="el-icon-search" class="forest-btn">查询</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 简化的单窗口布局 - 只保留两个核心图表 -->
    <div class="result-content">
      <!-- 排放流动情况区域 -->
      <div class="flow-section">
        <div class="sankey-card forest-card">
          <div class="forest-card-header">
            <i class="el-icon-share"></i>
            <span>{{ this.year }}年{{ this.month }}月CO<sub>2</sub>排放流动情况</span>
          </div>
          <div id="myMulberry" style="width:100%;height:calc(100% - 50px);flex:1;margin-top: 5px;margin-bottom: 5px;">
          </div>
        </div>
      </div>

      <!-- 排放趋势区域 -->
      <div class="trend-section">
        <div class="trend-card forest-card">
          <div class="forest-card-header">
            <i class="el-icon-trending-up"></i>
            <span>{{ this.duijizhu.startYear }}年{{ this.duijizhu.startMonth }}月&nbsp;&nbsp;~&nbsp;&nbsp;{{ this.duijizhu.endYear }}年{{ this.duijizhu.endMonth }}月CO<sub>2</sub>排放趋势</span>
          </div>
          <div class="trend-controls">
            <el-form :inline="true" class="filter-form">
              <el-form-item>
                <el-tag>起始时间：</el-tag>
                <el-date-picker style="width: 100px;" v-model="duijizhu.showStartYear" type="year"
                  value-format='yyyy' placeholder="选择年" size="small">
                </el-date-picker>
                <el-select v-model="duijizhu.showStartMonth" placeholder="选择月" style="width: 80px;" @change="changeMe()">
                  <el-option v-for="item in 12" :key="item" :label="item" :value="item">
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-tag>结束时间：</el-tag>
                <el-date-picker style="width: 100px;" v-model="duijizhu.showEndYear" type="year"
                  value-format='yyyy' placeholder="选择年" size="small">
                </el-date-picker>
                <el-select v-model="duijizhu.showEndMonth" placeholder="选择月" style="width: 80px;" @change="changeMe()">
                  <el-option v-for="item in 12" :key="item" :label="item" :value="item">
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button class="info" size="small" @click="searchType = 0" plain>物品类别</el-button>
                <el-button class="info" size="small" @click="searchType = 1" plain>排放类型</el-button>
                <el-button class="forest-btn" size="small" @click="getConditionalDuijizhu()" plain
                  icon="el-icon-search" style="margin-left: 10px;">查询</el-button>
              </el-form-item>
            </el-form>
          </div>
          <div id="myduijiBar" style="width:100%;height: 450px;flex:1;margin-top: 10px;min-height: 300px;">
          </div>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>
<script>
import * as echarts from 'echarts/core';
import { SankeyChart } from 'echarts/charts';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([SankeyChart, CanvasRenderer]);

// 注意：ECharts库内部会添加一些事件监听器，包括非passive的wheel事件
// 这个警告是Chrome浏览器对性能优化的建议，不会影响功能
// 禁用相关功能可能影响用户体验，因此选择接受此警告
import MyPie from "../../components/Assessment";
import TanTotal from "../../components/TanTotal";
import {
  getCarbonMulberry,
  getDuijizhuCategory,
  getDuijizhuType,
  getBingtu,
} from "../../api/carbonEmission";
import axios from "axios";
import { publicNetworkIpAndPort } from "../../api/globalVar";
export default {
  components: { TanTotal, MyPie },
  name: "",
  data() {
    return {
      ms: 1,
      mse: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
      showYear: '',
      showMonth: '',
      year: '',
      searchType: '',//选择查询类型
      sangShen: {
        data: [],//桑基图节点数据列表
        links: [],//节点间的边。注意: 桑基图理论上只支持有向无环图
      },
      duijizhu: {
        showStartYear: '',
        showStartMonth: '',
        showEndYear: '',
        showEndMonth: '',
        startYear: '2023',
        startMonth: '1',
        endYear: '2023',
        endMonth: '6',
        xArr: [],
        series: [],
      },
      month: '',
      carbonInfor: {},
      myLine: '',
      emissionInformationIn: {
        year: '',
      },
    }
  },
  mounted: function () {
    // 先初始化桑基图，获取实际年份，然后用这个年份初始化趋势图
    this.getMulberry().then(() => {
      // 使用桑基图返回的年份来初始化趋势图
      const sankeyYear = this.year || new Date().getFullYear();
      this.duijizhu.startYear = sankeyYear.toString();
      this.duijizhu.startMonth = 1;
      this.duijizhu.endYear = sankeyYear.toString();
      this.duijizhu.endMonth = 12;
      this.duijizhu.showStartYear = sankeyYear.toString(); // 转换为字符串
      this.duijizhu.showStartMonth = 1;
      this.duijizhu.showEndYear = sankeyYear.toString(); // 转换为字符串
      this.duijizhu.showEndMonth = 12;
      // 直接调用获取堆积折叠图数据的方法，使用默认的物品分类查询
      this.getCarbonBarDuiji();
    }).catch(() => {
      // 如果桑基图加载失败，使用默认年份
      const currentYear = new Date().getFullYear();
      this.year = currentYear;
      this.duijizhu.startYear = currentYear.toString();
      this.duijizhu.startMonth = 1;
      this.duijizhu.endYear = currentYear.toString();
      this.duijizhu.endMonth = 12;
      this.duijizhu.showStartYear = currentYear.toString(); // 转换为字符串
      this.duijizhu.showStartMonth = 1;
      this.duijizhu.showEndYear = currentYear.toString(); // 转换为字符串
      this.duijizhu.showEndMonth = 12;
      // 直接调用获取堆积折叠图数据的方法，使用默认的物品分类查询
      this.getCarbonBarDuiji();
    });
  },
  methods: {
    changeMe() {
      console.log("this.duijizhu.showStartMonth:", this.duijizhu.showStartMonth);
      this.ms = parseInt(this.duijizhu.showStartMonth) + 1;
      console.log("ms:", this.ms);
    },
    //饼图
    //返回指定年月中包含的年月数
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
    refreshCharts() {
      // 用户手动查询时，根据选择的年月刷新桑基图
      if ((this.showYear == null || this.showYear == '') && (this.showMonth == null || this.showMonth == '')) {
        // 用户没有选择年月，清空并使用默认年月
        this.year = '';
        this.month = '';
        this.getMulberry();
      }
      else if (((this.showYear == '' || this.showYear == null) && (this.showMonth != '' && this.showMonth != null))
        || ((this.showMonth != '' && this.showMonth != null) && (Number(this.showMonth) <= 0 || Number(this.showMonth) > 12))) {
        this.$message.error('请输入完整且合理的查询年月！');
        this.year = '';
        this.month = '';
        this.showYear = '';
        this.showMonth = '';
        this.getMulberry();
      }
      else {
        // 用户选择了年月，使用用户选择的年月刷新桑基图
        this.year = this.showYear;
        this.month = this.showMonth;
        this.getMulberry();
      }
    },
    //条件柱状堆积图
    getConditionalDuijizhu() {
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
    // 初始化柱状图
    initBarChart(retryCount = 0) {
      const maxRetries = 10; // 最大重试次数

      // console.log("this.duijizhu.series：",this.duijizhu.series)
      // console.log("this.duijizhu.xArr：",this.duijizhu.xArr)

      var chartDom = document.getElementById('myduijiBar');
      if (!chartDom) {
        console.error('堆积折叠图容器不存在');
        return;
      }

      // 检查DOM尺寸
      if (chartDom.clientWidth === 0 || chartDom.clientHeight === 0) {
        if (retryCount < maxRetries) {
          console.warn(`堆积折叠图容器尺寸为0，延迟初始化 (${retryCount + 1}/${maxRetries})`);
          setTimeout(() => {
            this.initBarChart(retryCount + 1);
          }, 200); // 增加延迟时间
        } else {
          console.error('堆积折叠图容器尺寸始终为0，放弃初始化');
        }
        return;
      }

      // console.log('堆积折叠图容器尺寸正常，开始初始化:', {
      //   width: chartDom.clientWidth,
      //   height: chartDom.clientHeight
      // });

      var myChart = echarts.init(chartDom, null, {
        renderer: 'canvas',
        useDirtyRect: false,
        ssr: false,
        width: chartDom.clientWidth,
        height: chartDom.clientHeight
      });
      myChart.clear();

      var option;

      option = {
        // 移除title以节省空间
        tooltip: {
          trigger: 'axis',
          triggerOn: 'click', // 改为点击触发，避免鼠标移动时的滚动事件
          axisPointer: {
            type: 'shadow'
          },
          formatter: (params) => {
            // console.log("params", params);
            let res = '碳排放量' + '<br/>'
            for (var i = 0; i < params.length; i++) {
              res += params[i].marker + params[i].seriesName + ':' + parseFloat(params[i].value).toFixed(2) + '(kg)' + '<br/>'
            }
            return res;
          },
        },
        legend: {
          top: 10, // 由于没有title，将legend移到更上方
          textStyle: {
            color: '#2d5016',
          }
        },
        grid: {
          left: '8%', // 增加左侧间距，避免y轴标签被遮挡
          right: '5%',
          bottom: '15%',
          top: '15%', // 减少上方间距，因为没有title
          containLabel: true
        },
        xAxis: [
          {
            axisLabel: {
              margin: 10,
              interval: 0,
              rotate: 45,
              color: '#2d5016',
              fontSize: 11,
            },
            type: 'category',
            data: this.duijizhu.xArr,
            name: '时间',
            nameLocation: 'middle',
            nameGap: 30,
          }
        ],
        yAxis: [
          {
            type: 'value',
            name: '排放量(kg)',
            nameLocation: 'middle',
            nameGap: 70, // 进一步增加间距，确保单位标签与刻度有足够距离
            axisLabel: {
              color: '#2d5016',
              fontSize: 11,
              formatter: function(value) {
                return value.toFixed(0); // 格式化数值，减少小数位
              }
            }
          }
        ],
        series: this.duijizhu.series,
        dataZoom: [
          {
            type: 'slider',
            show: this.duijizhu.xArr && this.duijizhu.xArr.length > 6,
            start: 0,
            end: 70,
            height: 12,
            bottom: 10,
            realtime: false, // 设置为false以减少事件监听器，但警告仍可能出现（这是ECharts库内部行为）
          }
        ],
      };

      // 如果没有数据，显示空状态
      if (!this.duijizhu.series || this.duijizhu.series.length === 0 || !this.duijizhu.xArr || this.duijizhu.xArr.length === 0) {
        option = {
          title: {
            text: '暂无数据',
            left: 'center',
            top: 'center',
            textStyle: {
              color: '#999',
              fontSize: 14,
            }
          },
          graphic: {
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
              text: '请检查查询条件或数据是否存在',
              fontSize: 12,
              fill: '#999',
            }
          }
        };
      }

      option && myChart.setOption(option, {
        notMerge: false,
        lazyUpdate: true,
        silent: false
      });

      // 添加窗口大小改变监听
      window.addEventListener('resize', () => {
        myChart.resize();
      });
    },
    //堆叠柱状图
    async getCarbonBarDuiji() {
      // console.log('开始获取堆积折叠图数据...');
      // console.log('当前参数:', {
      //   startYear: this.duijizhu.startYear,
      //   startMonth: this.duijizhu.startMonth,
      //   endYear: this.duijizhu.endYear,
      //   endMonth: this.duijizhu.endMonth,
      //   searchType: this.searchType
      // });

      this.duijizhu.series = []
      // 如果startYear和endYear都为空，不设置xArr，让图表显示为空
      if (this.duijizhu.startYear != '' && this.duijizhu.startYear != null && this.duijizhu.startMonth != '' && this.duijizhu.startMonth != null
        && this.duijizhu.endYear != '' && this.duijizhu.endYear != null && this.duijizhu.endMonth != '' && this.duijizhu.endMonth != null
        && Number(this.duijizhu.startMonth) >= 1 && Number(this.duijizhu.startMonth) <= 12 && Number(this.duijizhu.endMonth) >= 1
        && Number(this.duijizhu.endMonth) <= 12 && Number(this.duijizhu.startYear) * 12 + Number(this.duijizhu.startMonth) <= Number(this.duijizhu.endYear) * 12 + Number(this.duijizhu.endMonth)) {
        this.duijizhu.xArr = this.calculateMonths(this.duijizhu.startYear, this.duijizhu.startMonth, this.duijizhu.endYear, this.duijizhu.endMonth)
        // console.log('计算的xArr:', this.duijizhu.xArr);
      } else {
        // 如果时间范围无效，清空数据，不设置默认的xArr
        // console.warn('时间范围无效，清空数据');
        this.duijizhu.startYear = '';
        this.duijizhu.startMonth = '';
        this.duijizhu.endYear = '';
        this.duijizhu.endMonth = '';
        this.duijizhu.xArr = [];
      }
      // console.log("this.searchType", this.searchType)
      if (this.searchType == '1') {
        // console.log('调用排放类型API...');
        await getDuijizhuType({ startYear: this.duijizhu.startYear, startMonth: this.duijizhu.startMonth, endYear: this.duijizhu.endYear, endMonth: this.duijizhu.endMonth }).then((res) => {
          // console.log('排放类型API响应:', res);
          if (res.data.code == 200) {
            // console.log('排放类型数据:', res.data.data);
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
                barWidth: 30,
                emphasis: {
                  focus: 'series',
                  itemStyle: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                  }
                },
                stack: 'Ad',
                data: [],
                itemStyle: {
                  color: function () {
                    return "#" + Math.floor(Math.random() * 16777215).toString(16);
                  }
                }
              }

              // 创建一个映射，将月份映射到排放量
              var monthDataMap = {};
              for (var j = 0; j < res.data.data[i].emissionMonthAmount.length; j++) {
                var month = res.data.data[i].emissionMonthAmount[j].time;
                var emissionAmount = res.data.data[i].emissionMonthAmount[j].emissionAmount;
                monthDataMap[month] = emissionAmount;
              }

              // 按照xArr的顺序填充数据
              for (var k = 0; k < this.duijizhu.xArr.length; k++) {
                var monthLabel = this.duijizhu.xArr[k];
                var value = monthDataMap[monthLabel] || 0; // 如果没有数据，默认为0
                seriesobj.data.push(value);
              }

              this.duijizhu.series.push(seriesobj)
            }
            // console.log('构建的series数据:', this.duijizhu.series);
          } else {
            console.error('API响应码不是200:', res.data.code);
          }
        }).catch((error) => {
          console.error('排放类型API调用失败:', error);
        })
      }
      else {
        // console.log('调用物品分类API...');
        await getDuijizhuCategory({ startYear: this.duijizhu.startYear, startMonth: this.duijizhu.startMonth, endYear: this.duijizhu.endYear, endMonth: this.duijizhu.endMonth }).then((res) => {
          // console.log('物品分类API响应:', res);
          if (res.data.code == 200) {
            // console.log('物品分类数据:', res.data.data);
            for (var i = 0; i < res.data.data.length; i++) {
              var seriesobj = {    //一个seriesobj即为一个物品分类1-12月的柱状图
                name: res.data.data[i].objectCategory,
                type: 'bar',
                barWidth: 30,
                stack: 'Ad',
                data: [],
                emphasis: {
                  itemStyle: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                  }
                },
                itemStyle: {
                  color: function () {
                    return "#" + Math.floor(Math.random() * 16777215).toString(16);
                  }
                }
              }

              // 创建一个映射，将月份映射到排放量
              var monthDataMap = {};
              for (var j = 0; j < res.data.data[i].emissionMonthAmount.length; j++) {
                var month = res.data.data[i].emissionMonthAmount[j].time;
                var emissionAmount = res.data.data[i].emissionMonthAmount[j].emissionAmount;
                monthDataMap[month] = emissionAmount;
              }

              // 按照xArr的顺序填充数据
              for (var k = 0; k < this.duijizhu.xArr.length; k++) {
                var monthLabel = this.duijizhu.xArr[k];
                var value = monthDataMap[monthLabel] || 0; // 如果没有数据，默认为0
                seriesobj.data.push(value);
              }

              this.duijizhu.series.push(seriesobj)
            }
            // console.log('构建的series数据:', this.duijizhu.series);
          } else {
            console.error('API响应码不是200:', res.data.code);
          }
        }).catch((error) => {
          console.error('物品分类API调用失败:', error);
        }).finally(() => {
        });
      }
      // 初始化柱状图 - 使用nextTick确保DOM更新完成
      // console.log('调用initBarChart初始化堆积折叠图');
      this.$nextTick(() => {
        this.initBarChart();
      });
    },
    // 初始化桑基图
    initSankeyChart(dataList) {
      this.$nextTick(() => {
        var chartDom = document.getElementById('myMulberry');
        if (!chartDom) {
          console.error('桑基图容器不存在');
          return;
        }

        // 检查DOM尺寸
        if (chartDom.clientWidth === 0 || chartDom.clientHeight === 0) {
          console.warn('桑基图容器尺寸为0，延迟初始化');
          setTimeout(() => {
            this.initSankeyChart(dataList);
          }, 100);
          return;
        }

        var myChart = echarts.init(chartDom, null, {
          renderer: 'canvas',
          useDirtyRect: false,
          ssr: false,
          width: chartDom.clientWidth,
          height: chartDom.clientHeight
        });
        myChart.clear();
        var option;
        // 处理桑基图数据
          //将固定的直接手动添加进去
          var databoj1 = {
            name: '直接排放'
          }
          var databoj2 = {
            name: '间接排放'
          }
          var databoj3 = {
            name: '其他排放'
          }
          this.sangShen.data.push(databoj1)
          this.sangShen.data.push(databoj2)
          this.sangShen.data.push(databoj3)
          for (var i = 0; i < dataList.length; i++) {
            var databoj = {
              name: dataList[i].objectCategory
            }
            this.sangShen.data.push(databoj)//拿到所有物品分类
            for (var j = 0; j < dataList[i].emissionTypeAmount.length; j++) {//一个物品类别可能有多个排放类型(多个流向边)
              var emissionType = '';
              if (dataList[i].emissionTypeAmount[j].emissionType == 0)
                emissionType = '直接排放'
              else if (dataList[i].emissionTypeAmount[j].emissionType == 1)
                emissionType = '间接排放'
              else
                emissionType = '其他排放'

              var link = {//一个流向边
                source: dataList[i].objectCategory,
                target: emissionType,
                value: dataList[i].emissionTypeAmount[j].emissionAmount
              }
              this.sangShen.links.push(link);
            }
          }
          // console.log("this.sangShen.data", this.sangShen.data)
          // console.log("this.sangShen.links", this.sangShen.links)
          myChart.clear();
        option = {
          title: {
            text: '',
            left: 'center',
            top: 10,
          },
          tooltip: {
            trigger: 'item',
            formatter: (params) => {
              // console.log(params);
              var name = params.name.indexOf('>') != -1 ? (params.name.split('>')[0] + '-' + params.name.split('>')[1]) : params.name
              return '碳排放量' + '<br/>' + name + ' : ' + parseFloat(params.value).toFixed(2) + '(kg)' + '<br/>';
            },
          },
          series: {
            type: 'sankey',
            layout: 'none',
            emphasis: {
              focus: 'adjacency'
            },
            left: '3%',
            right: '8%',  /* 增加右侧距离，防止label越界 */
            top: '8%',    /* 增加顶部距离 */
            bottom: '12%', /* 增加底部距离 */
            nodeWidth: 20,
            nodeGap: 8,
            orient: 'horizontal',
            data: this.sangShen.data,
            links: this.sangShen.links,
            label: {
              fontSize: 12,
              color: '#2d5016',
            },
            lineStyle: {
              color: 'source',
              curveness: 0.5,
            },
          }
        };
        option && myChart.setOption(option, {
          notMerge: false,
          lazyUpdate: true,
          silent: false
        });

        // 强制resize确保canvas正确定位
        setTimeout(() => {
          myChart.resize();
        }, 50);

        // 添加窗口大小改变监听
        window.addEventListener('resize', () => {
          myChart.resize();
        });
      });
    },
    //桑葚图
    getMulberry() {
      return new Promise((resolve, reject) => {
        this.sangShen.data = []
        this.sangShen.links = []
        // 如果year和month为空，不传参数，让后端自动回退查找
        const params = {};
        if (this.year && this.year !== '') {
          params.year = this.year;
        }
        if (this.month && this.month !== '') {
          params.month = this.month;
        }
        getCarbonMulberry(params).then(res => {
          if (res.data.code == 200) {
            // 处理返回的数据（可能是list字段或直接是数组）
            var dataList = res.data.data.list || res.data.data;
            // 如果后端返回了actualYear，更新显示的年份
            if (res.data.data.actualYear) {
              this.year = res.data.data.actualYear;
            }
            // 开始画桑葚图
            this.initSankeyChart(dataList);
            resolve();
          } else {
            reject(new Error('API response code is not 200'));
          }
        }).catch(err => {
          reject(err);
        });
      });
    },
  },
}
</script>
<style scoped>
/* 两排布局样式 - 每排占满宽度，与查询条件宽度统一 */
.result-content {
  width: 100%;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: auto;
}

/* 排放流动情况区域 */
.flow-section {
  width: 100%;
}

.sankey-card {
  width: 100%;
  height: 450px; /* 进一步增加高度，提供更多显示空间 */
  position: relative; /* 确保定位基准 */
  overflow: hidden; /* 防止内容溢出 */
}

/* 桑葚图标题固定高度 */
.sankey-card .forest-card-header {
  height: 50px; /* 固定标题高度 */
  flex-shrink: 0; /* 防止被压缩 */
}

/* 排放趋势区域 */
.trend-section {
  width: 100%;
}

.trend-card {
  width: 100%;
  height: 500px; /* 增加高度，与桑葚图保持一致 */
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .result-content {
    padding: 15px;
    gap: 15px;
  }

  .sankey-card {
    height: 450px; /* 移动端也相应增加高度 */
  }

  .trend-card {
    height: 450px; /* 移动端保持一致 */
  }

  .trend-controls {
    padding: 15px;
  }

  .filter-form {
    gap: 15px;
  }
}

/* 趋势对比控制区域 */
.trend-controls {
  background: var(--forest-bg-card);
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 10px;
  border: 1px solid var(--forest-border-light);
  flex-shrink: 0; /* 防止压缩 */
}

.trend-controls .filter-form {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.trend-controls .filter-form .el-form-item {
  margin-bottom: 0;
}

/* Element UI 标签页样式覆盖 */
::v-deep .el-tabs__header {
  margin-bottom: 20px;
}

::v-deep .el-tabs__nav-wrap::after {
  display: none;
}

::v-deep .el-tabs__item {
  color: var(--forest-text-secondary);
  font-weight: 600;
  padding: 0 30px;
  font-size: 16px;
}

::v-deep .el-tabs__item:hover {
  color: var(--forest-primary);
}

::v-deep .el-tabs__item.is-active {
  color: var(--forest-primary);
}

::v-deep .el-tabs__active-bar {
  background-color: var(--forest-primary);
  height: 3px;
}

::v-deep .el-tabs__content {
  padding: 0;
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
  padding: 20px;
  box-sizing: border-box;
  min-height: auto;
}

/* 主要内容容器 */
.main-box-result {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0; /* 去除所有内边距，让内容紧贴容器边缘 */
  background-color: transparent;
}

/* 筛选条件区域 */
.filter-section {
  width: 100%;
  margin: 0 auto 20px; /* 与 TanMonitor.vue 保持一致: 上边距0，下边距20px */
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

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-content {
    padding: 15px;
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }

  .filter-input {
    width: 100%;
  }
}



.top {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  gap: 20px;
}

.junzhi {
  width: 370px;
  flex-shrink: 0;
}

.zhuzhuangtu {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  padding: 0;
  max-width: calc(1400px - 370px - 20px);
}

.middle {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  width: 100%;
  max-width: 1400px;
  margin: 20px auto;
  padding: 0 20px;
  gap: 20px;
  box-sizing: border-box;
}

.sangshen {
  width: 730px;
  height: 500px;
  overflow: hidden;
  flex-shrink: 0;
}

.bottom {
  width: 100%;
  margin: 0;
  padding: 20px;
  box-sizing: border-box;
  background-color: var(--forest-bg-primary);
}

.duijizhu {
  width: 100%;
  max-width: 1360px;
  height: 740px;
  overflow: hidden;
  margin: 0 auto;
}

.bingtu {
  width: 610px;
  height: 500px;
  overflow: hidden;
  flex-shrink: 0;
}
</style>
<style>
.el-input__inner {
  height: 32.5px;
}
</style>
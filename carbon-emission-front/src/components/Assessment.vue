<template>
  <div class="container">
    <!-- 改改style探索下样式-->
    <div :id="ids" :style="{ width: '100%', height: '100%' }"></div>
  </div>
</template>
<!--, border: '1px solid #000'-->
<script>
export default {
  props: ["x", "y", "ids"],
  mounted() {
    this.drawLine(this.y);
  },
  methods: {
    drawLine(value) {
      let haveValueFlag = false;
      for (let i = 0; i < value.length; i++) {
        var tempValue = value[i].value;
        if (tempValue == 0) {
          value[i].value = null;
        } else {
          haveValueFlag = true;
        }
      }

      this.y = value;
      // 获取 DOM 元素
      const chartDom = document.getElementById(this.ids);
      if (!chartDom) {
        return;
      }
      
      // 先检查是否已有图表实例，如果有则先销毁
      let myChart = this.$echarts.getInstanceByDom(chartDom);
      if (myChart) {
        myChart.dispose();
      }
      
      // 基于准备好的dom，初始化echarts实例
      myChart = this.$echarts.init(chartDom);
      // 绘制图表
      var haveValueOption = {
        legend: {
          orient: 'vertical',
          right: '10%',
          top: 'middle',
          data: this.x,
          textStyle: {
            fontSize: 14
          },
          itemWidth: 14,
          itemHeight: 14,
          itemGap: 10
        },
        tooltip: {
          trigger: 'item',
          formatter: (params) => {
            // console.log(params);
            return '碳排放量' + '<br/>' + params.marker + params.name + ':' + parseFloat(params.value).toFixed(2) + '(kg)' + '<br/>';
          },
          show: true,
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
          radius: ['45%', '75%'],
          center: ['35%', '50%'],
          barWidth: '10%',
          data: value,
          label: {
            show: true,
            position: 'outside',
            formatter: `{d}%`,
            fontSize: 12
          },
          labelLine: {
            length: 15,
            length2: 10
          }
        }]
      };

      myChart.setOption(haveValueOption);

    },
  },
};
</script>
<style scoped>
#a,
#b {
  float: left;
  margin: 0px;
}
</style>

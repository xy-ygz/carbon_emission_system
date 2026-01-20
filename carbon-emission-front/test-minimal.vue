<template>
  <div class="tanResult">
    <div class="chooseBtn">
      <div class="flex-box-headerr">
        <el-form>
          <el-tag>选择年份：</el-tag>
          <el-form-item style="display: inline-block;margin-bottom: 5px !important;">
            <el-date-picker style="width: 200px;" class="buildingleft" v-model="showYear" type="year" value-format='yyyy'
              placeholder="选择年" size="small">
            </el-date-picker>
            <el-select v-model="showMonth" placeholder="选择月">
              <el-option v-for="item in 12" :key="item" :label="item" :value="item">
              </el-option>
            </el-select>
          </el-form-item>
          <el-button class="form-item-inline" size="small" @click="refreshCharts()" plain
            icon="el-icon-search">查询</el-button>
        </el-form>
      </div>
    </div>

    <div class="result-tabs">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="总体概览" name="overview">
          <div class="tab-content">
            <p>总体概览内容</p>
          </div>
        </el-tab-pane>
        <el-tab-pane label="构成分析" name="composition">
          <div class="tab-content">
            <p>构成分析内容</p>
          </div>
        </el-tab-pane>
        <el-tab-pane label="趋势对比" name="trend">
          <div class="tab-content">
            <p>趋势对比内容</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
export default {
  name: "",
  data() {
    return {
      activeTab: 'overview',
      showYear: '',
      showMonth: '',
    }
  },
  methods: {
    refreshCharts() {
      console.log('refresh charts');
    },
    handleTabClick(tab) {
      console.log('tab clicked:', tab.name);
    }
  }
}
</script>

<style scoped>
.result-tabs {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.tab-content {
  padding: 20px 0;
}
</style>
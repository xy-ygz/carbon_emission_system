<template>
  <div class="content-wrapper">
      <!-- 筛选条件区域 -->
      <div class="forest-card filter-section">
        <div class="forest-card-header">
          <i class="el-icon-search"></i>
          <span>查询条件</span>
        </div>
        <div class="filter-content">
          <el-form :inline="true" class="filter-form">
            <el-form-item label="选择年份：">
              <el-date-picker
                v-model="searchYear"
                type="year"
                @change="handleYearChange"
                value-format="yyyy"
                placeholder="选择年"
                size="small"
                class="filter-input">
              </el-date-picker>
            </el-form-item>
            <el-form-item label="选择月份：">
              <el-select
                v-model="searchMonth"
                placeholder="选择月"
                size="small"
                @change="handleMonthChange"
                clearable
                class="filter-input">
                <el-option
                  v-for="item in 12"
                  :key="item"
                  :label="item + '月'"
                  :value="item">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                size="small"
                @click="loadData"
                type="primary"
                icon="el-icon-search"
                class="forest-btn">
                查询
              </el-button>
              <el-button
                size="small"
                @click="resetData"
                icon="el-icon-refresh"
                class="forest-btn">
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 主要显示区域 -->
      <div class="main-content">
        <!-- 左侧：现值与计算值对比 -->
        <div class="left-panel">
          <div class="value-card">
            <div class="card-header">
              <h3>碳排放量对比</h3>
              <span class="year-tag">{{ displayYear }}年{{ displayMonth ? displayMonth + '月' : '' }}</span>
            </div>
            <div class="value-content">
              <div class="value-item present-value">
                <div class="value-label">
                  <i class="el-icon-data-line"></i>
                  <span>现值</span>
                </div>
                <div class="value-number">
                  {{ formatNumber(presentValue) }} kg
                </div>
                <div class="value-desc">当前年份实际碳排放量</div>
              </div>
              
              <div class="value-item calculated-value" :class="{ 'increased': calculatedValue > presentValue, 'decreased': calculatedValue < presentValue }">
                <div class="value-label">
                  <i class="el-icon-cpu"></i>
                  <span>计算值</span>
                </div>
                <div class="value-number">
                  {{ formatNumber(calculatedValue) }} kg
                </div>
                <div class="value-desc">调整后的碳排放量</div>
              </div>

              <div class="comparison-section">
                <div class="comparison-item">
                  <span class="comparison-label">差值：</span>
                  <span class="comparison-value" :class="{ 'positive': difference > 0, 'negative': difference < 0 }">
                    {{ difference > 0 ? '+' : '' }}{{ formatNumber(difference) }} kg
                  </span>
                </div>
                <div class="comparison-item">
                  <span class="comparison-label">变化率：</span>
                  <span class="comparison-value" :class="{ 'positive': changeRate > 0, 'negative': changeRate < 0 }">
                    {{ changeRate > 0 ? '+' : '' }}{{ formatNumber(changeRate) }}%
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：分类调节区域 -->
        <div class="right-panel">
          <div class="category-card">
            <div class="card-header">
              <h3>分类调节</h3>
              <span class="category-count">共 {{ categoryList.length }} 个分类</span>
            </div>
            <div class="category-list" v-if="categoryList.length > 0">
              <div
                v-for="(item, index) in categoryList"
                :key="index"
                class="category-item">
                <div class="category-info">
                  <div class="category-name">
                    <i class="el-icon-box"></i>
                    <span>{{ item.category }}</span>
                  </div>
                  <div class="category-value">
                    <el-input-number
                      v-model="item.consumptionCount"
                      :min="0"
                      :max="100000000"
                      :precision="2"
                      :step="100"
                      @change="handleConsumptionChange"
                      size="small"
                      style="width: 200px;">
                    </el-input-number>
                    <span class="unit-text">单位</span>
                  </div>
                </div>
                <div class="category-stats">
                  <span class="stat-item">
                    当前值：<strong>{{ formatNumber(item.consumptionCount) }}</strong>
                  </span>
                  <span class="stat-item" v-if="item.originalValue !== undefined">
                    原始值：<strong>{{ formatNumber(item.originalValue) }}</strong>
                  </span>
                </div>
              </div>
            </div>
            <div class="empty-state" v-else>
              <i class="el-icon-warning-outline"></i>
              <p>暂无数据</p>
            </div>
          </div>
        </div>
      </div>
    </div>
</template>

<script>
import TanHeader from "../../components/TanHeader";
import { getConsumptionCount } from "../../api/carbonEmission";
import { getAllCategory } from "../../api/exchangeSetting";
import request from "../../utils/request";

export default {
  components: { TanHeader },
  name: "TanAnalyse",
  data() {
    return {
      searchYear: '',
      searchMonth: '',
      displayYear: '',
      displayMonth: '',
      presentValue: 0,
      calculatedValue: 0,
      categoryList: [],
      loading: false,
    };
  },
  computed: {
    difference() {
      return this.calculatedValue - this.presentValue;
    },
    changeRate() {
      if (this.presentValue === 0) return 0;
      return ((this.calculatedValue - this.presentValue) / this.presentValue) * 100;
    }
  },
  mounted() {
    // 初始加载：不传年份，让后端自动回退查找
    this.loadData();
  },
  activated() {
    // 如果组件被 keep-alive 缓存，激活时重新加载数据
    // 检查数据是否为空，如果为空则重新加载
    if (!this.categoryList || this.categoryList.length === 0 || this.presentValue === 0) {
      this.loadData();
    }
  },
  methods: {
    /**
     * 加载数据
     */
    loadData() {
      this.loading = true;
      // 重置数据，确保界面显示
      this.presentValue = 0;
      this.calculatedValue = 0;
      this.categoryList = [];
      this.displayYear = '';
      this.displayMonth = '';
      
      // 先获取现值
      this.getPresentValue().then(() => {
        // 再获取分类数据
        this.getCategoryData();
      }).catch((error) => {
        console.error('加载数据失败:', error);
        this.loading = false;
        // 确保即使失败也有默认值显示
        if (this.presentValue === 0) {
          this.presentValue = 0;
          this.calculatedValue = 0;
        }
      });
    },
    
    /**
     * 获取现值（当前年份的碳排放总量）
     */
    getPresentValue() {
      const params = {};
      if (this.searchYear && this.searchYear !== '') {
        params.year = this.searchYear;
      }
      if (this.searchMonth && this.searchMonth !== '') {
        params.month = this.searchMonth;
      }
      
      return request.get("/api/carbonEmission/collegeCarbonEmission", { params }).then((res) => {
        if (res.data && res.data.code == 200) {
          this.presentValue = parseFloat(res.data.data.totalEmission || 0);
          // 如果后端返回了actualYear，更新显示的年份
          if (res.data.data.actualYear) {
            this.displayYear = res.data.data.actualYear.toString();
          } else if (this.searchYear && this.searchYear !== '') {
            this.displayYear = this.searchYear;
          } else {
            this.displayYear = new Date().getFullYear().toString();
          }
        } else {
          // 如果响应格式不正确，设置默认值
          this.presentValue = 0;
          this.displayYear = new Date().getFullYear().toString();
        }
      }).catch((error) => {
        console.error('获取现值失败:', error);
        this.presentValue = 0;
        this.displayYear = new Date().getFullYear().toString();
      });
    },
    
    /**
     * 获取分类数据
     */
    getCategoryData() {
      const params = {};
      if (this.searchYear && this.searchYear !== '') {
        params.year = this.searchYear;
      }
      if (this.searchMonth && this.searchMonth !== '') {
        params.month = this.searchMonth;
      }
      
      getAllCategory().then((exchangeRes) => {
        if (exchangeRes.data && exchangeRes.data.code == 200) {
          const exchangeData = exchangeRes.data.data;
            
          getConsumptionCount(params).then((res) => {
            if (res.data && res.data.code == 200) {
              const consumptionDataList = res.data.data.list || res.data.data;
              
              const consumptionMap = {};
              if (Array.isArray(consumptionDataList)) {
                consumptionDataList.forEach(item => {
                  consumptionMap[item.category] = {
                    consumptionCount: item.consumptionCount,
                    originalValue: item.consumptionCount
                  };
                });
              }
              
              this.categoryList = exchangeData.map(item => {
                const consumptionData = consumptionMap[item.objectCategory] || {
                  consumptionCount: 0,
                  originalValue: 0
                };
                return {
                  category: item.objectCategory,
                  consumptionCount: consumptionData.consumptionCount,
                  originalValue: consumptionData.originalValue
                };
              });
              
              if (res.data.data.actualYear) {
                this.displayYear = res.data.data.actualYear.toString();
              }
              
              if (this.searchMonth && this.searchMonth !== '') {
                this.displayMonth = this.searchMonth;
              } else {
                this.displayMonth = '';
              }
              
              this.calculatedValue = this.presentValue;
            } else {
              this.categoryList = exchangeData.map(item => ({
                category: item.objectCategory,
                consumptionCount: 0,
                originalValue: 0
              }));
              this.calculatedValue = this.presentValue;
            }
          }).catch((error) => {
            console.error('获取分类数据失败:', error);
            this.categoryList = exchangeData.map(item => ({
              category: item.objectCategory,
              consumptionCount: 0,
              originalValue: 0
            }));
            this.calculatedValue = this.presentValue;
          }).finally(() => {
            this.loading = false;
          });
        } else {
          this.categoryList = [];
          this.calculatedValue = this.presentValue;
          this.loading = false;
        }
      }).catch((error) => {
        console.error('获取转化系数失败:', error);
        this.categoryList = [];
        this.calculatedValue = this.presentValue;
        this.loading = false;
      });
    },
    
    /**
     * 处理消耗量变化
     */
    handleConsumptionChange() {
      this.calculateEmission();
    },
    
    /**
     * 计算碳排放总量
     */
    calculateEmission() {
      if (!this.categoryList || this.categoryList.length === 0) {
        this.calculatedValue = this.presentValue; // 如果没有分类数据，计算值等于现值
        return;
      }
      
      // 构建请求数据 - 确保是数组格式
      const requestData = this.categoryList.map(item => ({
        category: item.category,
        consumptionCount: item.consumptionCount || 0
      }));
      
      // 确保 requestData 是数组
      if (!Array.isArray(requestData) || requestData.length === 0) {
        this.calculatedValue = this.presentValue;
        return;
      }
      
      // 使用 request.post 发送数组数据
      // axios 会自动将数组序列化为 JSON 数组格式
      // 确保 requestData 是纯数组，不要包装成对象
      request.post("/api/carbonEmission/countAllCarbonEmission", requestData).then(res => {
        if (res.data.code == 200) {
          this.calculatedValue = parseFloat(res.data.data || 0);
        }
      }).catch((error) => {
        console.error('计算碳排放量失败:', error);
        console.error('请求数据:', JSON.stringify(requestData));
        if (error.response && error.response.data) {
          console.error('错误详情:', error.response.data);
        }
        // 如果请求失败，使用现值
        this.calculatedValue = this.presentValue;
      });
    },
    
    /**
     * 年份变化处理
     */
    handleYearChange() {
      // 年份变化时，月份保持不变，但需要重新加载数据
      this.loadData();
    },
    
    /**
     * 月份变化处理
     */
    handleMonthChange() {
      // 月份变化时，需要重新加载数据
      this.loadData();
    },
    
    /**
     * 重置数据
     */
    resetData() {
      this.searchYear = '';
      this.searchMonth = '';
      this.displayYear = '';
      this.displayMonth = '';
      this.loadData();
    },
    
    /**
     * 格式化数字
     */
    formatNumber(num) {
      if (num === null || num === undefined || isNaN(num)) {
        return '0.00';
      }
      return parseFloat(num).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }
  }
};
</script>

<style scoped>
.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  padding-top: 20px;
  box-sizing: border-box;
  background-color: var(--forest-bg-primary);
  min-height: calc(100vh - 100px);
}

/* 筛选条件区域 */
.filter-section {
  width: 100%;
  margin: 0 auto 20px;
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

/* 主要内容区域 */
.main-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* 左侧面板 */
.left-panel {
  flex: 0 0 400px;
}

.value-card {
  background: var(--forest-bg-card);
  border-radius: 12px;
  box-shadow: var(--forest-shadow-sm);
  border: 1px solid var(--forest-border-light);
  overflow: hidden;
}

.card-header {
  padding: 16px 20px;
  background: var(--forest-gradient-primary);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 12px 12px 0 0;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header h3 i {
  font-size: 20px;
}

.year-tag {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
}

.value-content {
  padding: 20px;
}

.value-item {
  margin-bottom: 20px;
  padding: 20px;
  border-radius: 8px;
  background: var(--forest-bg-hover);
  transition: all 0.3s;
}

.value-item:last-child {
  margin-bottom: 0;
}

.present-value {
  border-left: 4px solid var(--forest-secondary);
}

.calculated-value {
  border-left: 4px solid var(--forest-success);
}

.calculated-value.increased {
  border-left-color: var(--forest-error);
  background: rgba(255, 77, 79, 0.1);
}

.calculated-value.decreased {
  border-left-color: var(--forest-success);
  background: rgba(82, 196, 26, 0.1);
}

.value-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.value-label i {
  font-size: 18px;
}

.value-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.value-desc {
  font-size: 12px;
  color: #909399;
}

.comparison-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--forest-border-light);
}

.comparison-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.comparison-item:last-child {
  margin-bottom: 0;
}

.comparison-label {
  font-size: 14px;
  color: #606266;
}

.comparison-value {
  font-size: 16px;
  font-weight: 600;
}

.comparison-value.positive {
  color: var(--forest-error);
}

.comparison-value.negative {
  color: var(--forest-success);
}

/* 右侧面板 */
.right-panel {
  flex: 1;
}

.category-card {
  background: var(--forest-bg-card);
  border-radius: 12px;
  box-shadow: var(--forest-shadow-sm);
  border: 1px solid var(--forest-border-light);
  overflow: hidden;
}

.category-count {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.category-list {
  max-height: 600px;
  overflow-y: auto;
  padding: 20px;
}

.category-item {
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid var(--forest-border-light);
  border-radius: 8px;
  transition: all 0.3s;
  background: var(--forest-bg-hover);
}

.category-item:hover {
  box-shadow: var(--forest-shadow-sm);
  border-color: var(--forest-secondary);
  background: var(--forest-bg-card);
}

.category-item:last-child {
  margin-bottom: 0;
}

.category-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.category-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.category-name i {
  font-size: 20px;
  color: var(--forest-secondary);
}

.category-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit-text {
  font-size: 12px;
  color: #909399;
}

.category-stats {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #606266;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-item strong {
  color: #303133;
  font-weight: 600;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
  color: #909399;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  display: block;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* 滚动条样式 */
.category-list::-webkit-scrollbar {
  width: 6px;
}

.category-list::-webkit-scrollbar-track {
  background: var(--forest-bg-hover);
  border-radius: 3px;
}

.category-list::-webkit-scrollbar-thumb {
  background: var(--forest-border);
  border-radius: 3px;
}

.category-list::-webkit-scrollbar-thumb:hover {
  background: var(--forest-secondary);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }
  
  .left-panel {
    flex: 1;
  }
}
</style>

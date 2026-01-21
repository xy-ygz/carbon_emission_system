<template>
  <div class="main-box-admin">
    <el-breadcrumb separator="/" style="font-size: 18px;">
      <el-breadcrumb-item :to="{ path: '/Tan/ManageCarbon' }">数据输入管理首页</el-breadcrumb-item>
      <el-breadcrumb-item>学校信息</el-breadcrumb-item>
    </el-breadcrumb>
    <br>
    <div>
      <div class="table_container" style="width: 100%; overflow-x: auto; overflow-y: visible;">
        <el-card shadow="never" style="border-radius: 12px;">
          <div slot="header" class="clearfix" style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-size: 18px; font-weight: bold; color: white;">
              <i class="el-icon-school" style="margin-right: 8px;"></i>学校信息
            </span>
            <el-button style="padding: 3px 0; color: white;" type="text" @click="loadSchoolInfo" icon="el-icon-refresh">刷新</el-button>
          </div>
          <el-form :model="schoolData" ref="schoolForm" label-width="160px" :rules="schoolRules" style="padding: 20px;">
            <el-form-item label="学校名称" prop="schoolName">
              <el-input v-model="schoolData.schoolName" style="width: 750px" placeholder="请输入学校名称" class="text-center-input"></el-input>
            </el-form-item>
            <el-form-item label="学校总人数" prop="totalNumber">
              <el-input-number v-model="schoolData.totalNumber" :min="0" style="width: 750px" placeholder="请输入学校总人数"></el-input-number>
            </el-form-item>
            <el-form-item label="在校学生人数" prop="studentNumber">
              <el-input-number v-model="schoolData.studentNumber" :min="0" style="width: 750px" placeholder="请输入在校学生人数"></el-input-number>
            </el-form-item>
            <el-form-item label="在校教师人数" prop="teacherNumber">
              <el-input-number v-model="schoolData.teacherNumber" :min="0" style="width: 750px" placeholder="请输入在校教师人数"></el-input-number>
            </el-form-item>
            <el-form-item label="学校总占地面积(m²)" prop="totalArea">
              <el-input-number v-model="schoolData.totalArea" :min="0" style="width: 750px" placeholder="请输入学校总占地面积"></el-input-number>
            </el-form-item>
            <el-form-item label="校园建筑物面积(m²)" prop="buildingArea">
              <el-input-number v-model="schoolData.buildingArea" :min="0" style="width: 750px" placeholder="请输入校园建筑物面积"></el-input-number>
            </el-form-item>
            <el-form-item label="校园绿化总面积(m²)" prop="greenArea">
              <el-input-number v-model="schoolData.greenArea" :min="0" style="width: 750px" placeholder="请输入校园绿化总面积"></el-input-number>
            </el-form-item>
            <el-form-item label="学校图片地址" prop="imageUrl">
              <el-input v-model="schoolData.imageUrl" style="width: 750px" placeholder="请输入学校图片地址（URL）" class="text-center-input"></el-input>
              <div style="margin-top: 10px;margin-left: 20px; color: #909399; font-size: 12px;">
                提示：可以输入图片的URL地址，例如：https://example.com/image.jpg 或 /static/images/school.jpg
              </div>
            </el-form-item>
            <el-form-item class="form-actions">
              <el-button style="margin-left: 20px;" type="primary" @click="updateSchoolInfo" :loading="updating">保存修改</el-button>
              <el-button @click="loadSchoolInfo">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>
<script>
import { getSchoolInfo, updateSchoolInfo } from "../../api/school";
import { getCurrentUserRoles } from "../../api/userManage";
import { extractPermissionsFromRoles, hasPermission } from "../../utils/permission";
export default {
  data() {
    return {
      schoolData: {
        id: null,
        schoolName: '',
        totalNumber: null,
        studentNumber: null,
        teacherNumber: null,
        totalArea: null,
        buildingArea: null,
        greenArea: null,
        imageUrl: ''
      },
      originalSchoolData: {},
      updating: false,
      userPermissions: [], // 当前用户的权限编码列表
      schoolRules: {
        schoolName: [
          { required: true, message: '请输入学校名称', trigger: 'blur' }
        ],
        totalNumber: [
          { required: true, message: '请输入学校总人数', trigger: 'blur' },
          { type: 'number', min: 0, message: '人数必须大于等于0', trigger: 'blur' }
        ],
        studentNumber: [
          { required: true, message: '请输入在校学生人数', trigger: 'blur' },
          { type: 'number', min: 0, message: '人数必须大于等于0', trigger: 'blur' }
        ],
        teacherNumber: [
          { required: true, message: '请输入在校教师人数', trigger: 'blur' },
          { type: 'number', min: 0, message: '人数必须大于等于0', trigger: 'blur' }
        ],
        totalArea: [
          { required: true, message: '请输入学校总占地面积', trigger: 'blur' },
          { type: 'number', min: 0, message: '面积必须大于等于0', trigger: 'blur' }
        ],
        buildingArea: [
          { required: true, message: '请输入校园建筑物面积', trigger: 'blur' },
          { type: 'number', min: 0, message: '面积必须大于等于0', trigger: 'blur' }
        ],
        greenArea: [
          { required: true, message: '请输入校园绿化总面积', trigger: 'blur' },
          { type: 'number', min: 0, message: '面积必须大于等于0', trigger: 'blur' }
        ]
      }
    }
  },
  mounted: function () {
    this.loadUserPermissions()
    this.loadSchoolInfo()
  },
  methods: {
    // 加载当前用户权限
    loadUserPermissions() {
      getCurrentUserRoles().then(res => {
        if (res.data.code == 200) {
          const roles = res.data.data || [];
          this.userPermissions = extractPermissionsFromRoles(roles);
        }
      }).catch(() => {
        this.userPermissions = [];
      });
    },
    // 检查是否有指定权限
    checkPermission(permissionCode) {
      return hasPermission(this.userPermissions, permissionCode);
    },
    loadSchoolInfo() {
      getSchoolInfo().then(res => {
        if (res.data.code == 200) {
          this.schoolData = res.data.data || {}
          // 保存原始数据用于重置
          this.originalSchoolData = JSON.parse(JSON.stringify(this.schoolData))
        } else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message || "获取学校信息失败！");
        }
      }).catch((error) => {
        // 40101错误已经在request.js中处理，这里不再重复提示
        if (!error.response || !error.response.data || error.response.data.code !== 40101) {
          this.$message.error("获取学校信息失败！");
        }
      });
    },
    updateSchoolInfo() {
      this.$refs.schoolForm.validate((valid) => {
        if (valid) {
          this.updating = true
          updateSchoolInfo(this.schoolData).then(res => {
            if (res.data.code == 200) {
              this.$message.success("修改成功！")
              this.loadSchoolInfo()
            } else {
              // 优先显示description，如果没有则显示message
              this.$message.error(res.data.description || res.data.message || "修改失败！")
            }
          }).catch((error) => {
            // 40101错误已经在request.js中处理，这里不再重复提示
            if (!error.response || !error.response.data || error.response.data.code !== 40101) {
              this.$message.error("修改失败！")
            }
          }).finally(() => {
            this.updating = false
          });
        }
      });
    }
  }
}
</script>

<style scoped>
/* .main-box-admin 样式已在全局 common.css 中统一定义 */

.table_container {
  width: 100%;
  overflow-x: auto;
  overflow-y: visible;
}

/* 表单项样式优化 */
::v-deep .el-form-item__label {
  margin-right: 20px !important; /* 增加标签右边距 */
}

/* 按钮表单项对齐 */
.form-actions ::v-deep .el-form-item__content {
  margin-left: 160px !important; /* 与输入框左边界对齐（标签宽度160px） */
}

/* 输入框内容居中显示 - 仅在此页面生效 */
/* Vue 2 使用 /deep/ 语法进行深度选择 */
/deep/ .el-input__inner {
  text-align: center !important;
}
</style>


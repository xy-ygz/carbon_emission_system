<template>
  <div class="main-box-admin">
    <el-breadcrumb separator="/" style="font-size: 18px; margin-bottom: 20px;">
      <el-breadcrumb-item :to="{ path: '/Tan/TanPage' }">
        <i class="el-icon-s-home"></i> 首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>权限管理</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="forest-card" style="padding: 20px; margin-bottom: 20px;">
      <div class="flex-box-header-new">
        <div class="energyleft">
          <el-form>
            <el-tag style="font-size: 13px;">权限编码：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchPermissionCode" placeholder="请输入权限编码" size="small" style="width:150px"></el-input>
            </el-form-item>
            <el-tag style="font-size: 13px; margin-left: 10px;">权限名称：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchPermissionName" placeholder="请输入权限名称" size="small" style="width:150px"></el-input>
            </el-form-item>
            <el-tag style="font-size: 13px; margin-left: 10px;">权限类型：</el-tag>
            <el-form-item style="display: inline-block">
              <el-select v-model="searchPermissionType" placeholder="请选择" size="small" style="width:120px" clearable>
                <el-option label="菜单" value="menu"></el-option>
                <el-option label="按钮" value="button"></el-option>
                <el-option label="接口" value="api"></el-option>
              </el-select>
            </el-form-item>
            <el-button class="form-item-inline" size="small" @click="filterPermissionTree" plain
              icon="el-icon-search">查询</el-button>
            <el-button class="form-item-inline" type="success" size="small" @click="getAllPermissionList" plain
              icon="el-icon-search">显示全部</el-button>
          </el-form>
        </div>
        <div style="margin-left: 50px;margin-bottom: 21px;">
          <el-form>
            <el-button class="form-item-inline" type="primary" size="small" @click="showAddDialog(null)" style="white-space: nowrap;">新增权限（顶级）</el-button>
          </el-form>
        </div>
      </div>
      <div class="table_container" style="width: 100%; overflow-x: auto; overflow-y: visible;">
        <el-table
          :data="filteredPermissionTreeData"
          style="width: 100%;font-size: 15px;"
          size="small"
          class="forest-table"
          :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
          row-key="id"
          :default-expand-all="true"
          :key="tableKey">
          <el-table-column label="权限编码" prop="permissionCode" min-width="340px" show-overflow-tooltip></el-table-column>
          <el-table-column label="权限名称" prop="permissionName" min-width="200px" show-overflow-tooltip></el-table-column>
          <el-table-column label="权限类型" prop="permissionType" width="120px">
            <template slot-scope="scope">
              <el-tag :type="scope.row.permissionType === 'menu' ? 'success' : scope.row.permissionType === 'button' ? 'warning' : 'info'" size="small">
                {{ scope.row.permissionType === 'menu' ? '菜单' : scope.row.permissionType === 'button' ? '按钮' : '接口' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="路由路径" prop="path" min-width="200px" show-overflow-tooltip></el-table-column>
          <el-table-column label="图标" prop="icon" width="120px">
            <template slot-scope="scope">
              <i :class="scope.row.icon" v-if="scope.row.icon"></i>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="sortOrder" width="80px"></el-table-column>
          <el-table-column label="状态" prop="status" width="100px">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="320px" fixed="right" style="white-space:nowrap;">
            <template slot-scope="scope">
              <el-button @click="showAddDialog(scope.row)" type="success" plain size="mini" icon="el-icon-plus" style="margin: 2px;">添加子权限</el-button>
              <el-button @click="showUpdateDialog(scope.row)" type="warning" plain size="mini" icon="el-icon-edit" style="margin: 2px;">修改</el-button>
              <el-button @click="deletePermissionById(scope.row)" type="danger" plain size="mini" icon="el-icon-delete" style="margin: 2px;">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 新增权限对话框 -->
      <el-dialog :title="addPermissionData.parentId === 0 || !addPermissionData.parentId ? '新增权限（顶级）' : '新增子权限'" :visible.sync="addShow" :modal="false" top="10%" width="60%">
        <el-form :model="addPermissionData" ref="addPermissionForm" label-width="120px" :rules="permissionRules">
          <el-form-item label="权限编码" prop="permissionCode">
            <el-input v-model="addPermissionData.permissionCode" style="width: 400px" placeholder="请输入权限编码（如：USER_MANAGE）"></el-input>
          </el-form-item>
          <el-form-item label="权限名称" prop="permissionName">
            <el-input v-model="addPermissionData.permissionName" style="width: 400px" placeholder="请输入权限名称"></el-input>
          </el-form-item>
          <el-form-item label="权限类型" prop="permissionType">
            <el-select v-model="addPermissionData.permissionType" placeholder="请选择权限类型" style="width: 400px">
              <el-option label="菜单" value="menu"></el-option>
              <el-option label="按钮" value="button"></el-option>
              <el-option label="接口" value="api"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="父权限" prop="parentId">
            <el-input v-model="addPermissionData.parentName" :disabled="true" style="width: 400px" v-if="addPermissionData.parentId && addPermissionData.parentId !== 0"></el-input>
            <span v-else style="color: #999;">顶级权限</span>
          </el-form-item>
          <el-form-item label="路由路径" prop="path" v-if="addPermissionData.permissionType === 'menu'">
            <el-input v-model="addPermissionData.path" style="width: 400px" placeholder="请输入路由路径（如：/Tan/ManageUser）"></el-input>
          </el-form-item>
          <el-form-item label="组件路径" prop="component" v-if="addPermissionData.permissionType === 'menu'">
            <el-input v-model="addPermissionData.component" style="width: 400px" placeholder="请输入组件路径（如：ManageUser）"></el-input>
          </el-form-item>
          <el-form-item label="图标" prop="icon" v-if="addPermissionData.permissionType === 'menu'">
            <el-input v-model="addPermissionData.icon" style="width: 400px" placeholder="请输入图标类名（如：el-icon-user）"></el-input>
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="addPermissionData.sortOrder" :min="0" style="width: 400px"></el-input-number>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="addPermissionData.description" type="textarea" :rows="3" style="width: 400px" placeholder="请输入权限描述"></el-input>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="addPermissionData.status" placeholder="请选择状态" style="width: 400px">
              <el-option label="启用" :value="1"></el-option>
              <el-option label="禁用" :value="0"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="insertPermissionData">确认添加</el-button>
            <el-button @click="addShow = false">取消</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>

      <!-- 修改权限对话框 -->
      <el-dialog title="修改权限" :visible.sync="updateShow" :modal="false" top="10%" width="60%">
        <el-form :model="updatePermissionData" ref="updatePermissionForm" label-width="120px" :rules="permissionRules">
          <el-form-item label="权限编码" prop="permissionCode">
            <el-input v-model="updatePermissionData.permissionCode" style="width: 400px" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item label="权限名称" prop="permissionName">
            <el-input v-model="updatePermissionData.permissionName" style="width: 400px" placeholder="请输入权限名称"></el-input>
          </el-form-item>
          <el-form-item label="权限类型" prop="permissionType">
            <el-select v-model="updatePermissionData.permissionType" placeholder="请选择权限类型" style="width: 400px">
              <el-option label="菜单" value="menu"></el-option>
              <el-option label="按钮" value="button"></el-option>
              <el-option label="接口" value="api"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="路由路径" prop="path" v-if="updatePermissionData.permissionType === 'menu'">
            <el-input v-model="updatePermissionData.path" style="width: 400px" placeholder="请输入路由路径"></el-input>
          </el-form-item>
          <el-form-item label="组件路径" prop="component" v-if="updatePermissionData.permissionType === 'menu'">
            <el-input v-model="updatePermissionData.component" style="width: 400px" placeholder="请输入组件路径"></el-input>
          </el-form-item>
          <el-form-item label="图标" prop="icon" v-if="updatePermissionData.permissionType === 'menu'">
            <el-input v-model="updatePermissionData.icon" style="width: 400px" placeholder="请输入图标类名"></el-input>
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="updatePermissionData.sortOrder" :min="0" style="width: 400px"></el-input-number>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="updatePermissionData.description" type="textarea" :rows="3" style="width: 400px" placeholder="请输入权限描述"></el-input>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="updatePermissionData.status" placeholder="请选择状态" style="width: 400px">
              <el-option label="启用" :value="1"></el-option>
              <el-option label="禁用" :value="0"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updatePermissionDataById">确认修改</el-button>
            <el-button @click="updateShow = false">取消</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import { getPermissionTree, addPermission, updatePermission, deletePermission } from "../../api/permission";
export default {
  data() {
    return {
      permissionTreeData: [],
      filteredPermissionTreeData: [],
      addShow: false,
      updateShow: false,
      isTableExpanded: true, // 控制表格默认展开
      tableKey: 0, // 用于强制重新渲染表格

      searchPermissionCode: '',
      searchPermissionName: '',
      searchPermissionType: '',

      addPermissionData: {
        permissionCode: '',
        permissionName: '',
        permissionType: 'menu',
        parentId: 0,
        parentName: '',
        path: '',
        component: '',
        icon: '',
        sortOrder: 0,
        description: '',
        status: 1
      },
      updatePermissionData: {},

      permissionRules: {
        permissionCode: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
        permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
        permissionType: [{ required: true, message: '请选择权限类型', trigger: 'change' }]
      }
    }
  },
  mounted: function () {
    this.getPermissionTreeList()
  },
  activated() {
    this.getPermissionTreeList()
  },

  methods: {
    getPermissionTreeList() {
      getPermissionTree().then(res => {
        if (res.data.code == 200) {
          this.permissionTreeData = res.data.data || []
          this.filteredPermissionTreeData = res.data.data || []
          // 数据加载完成后，强制重新渲染表格以应用 default-expand-all
          this.$nextTick(() => {
            this.isTableExpanded = true
            this.tableKey += 1 // 强制重新渲染表格
          })
        } else {
          // 40101错误已经在request.js中处理，这里不再重复提示
          if (res.data.code !== 40101) {
            // 优先显示description，如果没有则显示message
            this.$message.error(res.data.description || res.data.message || "获取权限树失败！");
          }
        }
      }).catch((error) => {
        // 40101错误已经在request.js中处理，这里不再重复提示
        if (!error.response || !error.response.data || error.response.data.code !== 40101) {
          this.$message.error("获取权限树失败！");
        }
      });
    },
    getAllPermissionList() {
      this.searchPermissionCode = ''
      this.searchPermissionName = ''
      this.searchPermissionType = ''
      this.filteredPermissionTreeData = JSON.parse(JSON.stringify(this.permissionTreeData))
      // 重置表格展开状态
      this.$nextTick(() => {
        this.isTableExpanded = true
        this.tableKey += 1
      })
    },
    filterPermissionTree() {
      if (!this.searchPermissionCode && !this.searchPermissionName && !this.searchPermissionType) {
        this.filteredPermissionTreeData = JSON.parse(JSON.stringify(this.permissionTreeData))
        return
      }

      const filterNode = (node) => {
        // 检查当前节点是否匹配搜索条件
        const codeMatch = !this.searchPermissionCode || 
          (node.permissionCode && node.permissionCode.toLowerCase().includes(this.searchPermissionCode.toLowerCase()))
        const nameMatch = !this.searchPermissionName || 
          (node.permissionName && node.permissionName.includes(this.searchPermissionName))
        const typeMatch = !this.searchPermissionType || node.permissionType === this.searchPermissionType

        // 递归过滤子节点
        let filteredChildren = []
        if (node.children && node.children.length > 0) {
          filteredChildren = node.children.map(child => filterNode(child)).filter(child => child !== null)
        }

        // 如果当前节点匹配，或者有匹配的子节点，则保留该节点
        if (codeMatch && nameMatch && typeMatch) {
          return {
            ...node,
            children: filteredChildren
          }
        } else if (filteredChildren.length > 0) {
          // 如果当前节点不匹配，但有匹配的子节点，也保留该节点
          return {
            ...node,
            children: filteredChildren
          }
        }

        return null
      }

      this.filteredPermissionTreeData = this.permissionTreeData
        .map(node => filterNode(node))
        .filter(node => node !== null)
      // 过滤后重置表格展开状态
      this.$nextTick(() => {
        this.isTableExpanded = true
        this.tableKey += 1
      })
    },
    showAddDialog(parent) {
      this.addShow = true;
      if (parent) {
        this.addPermissionData = {
          permissionCode: '',
          permissionName: '',
          permissionType: 'menu',
          parentId: parent.id,
          parentName: parent.permissionName,
          path: '',
          component: '',
          icon: '',
          sortOrder: 0,
          description: '',
          status: 1
        }
      } else {
        this.addPermissionData = {
          permissionCode: '',
          permissionName: '',
          permissionType: 'menu',
          parentId: 0,
          parentName: '',
          path: '',
          component: '',
          icon: '',
          sortOrder: 0,
          description: '',
          status: 1
        }
      }
      this.$nextTick(() => {
        setTimeout(() => {
          const firstInput = this.$el.querySelector('.el-dialog__body .el-input__inner');
          if (firstInput) {
            firstInput.focus();
          } else {
            const dialogBody = this.$el.querySelector('.el-dialog__body');
            if (dialogBody) {
              dialogBody.setAttribute('tabindex', '-1');
              dialogBody.focus();
            }
          }
        }, 200);
      });
    },
    showUpdateDialog(row) {
      this.updateShow = true;
      this.updatePermissionData = {
        id: row.id,
        permissionCode: row.permissionCode,
        permissionName: row.permissionName,
        permissionType: row.permissionType,
        parentId: row.parentId,
        path: row.path || '',
        component: row.component || '',
        icon: row.icon || '',
        sortOrder: row.sortOrder || 0,
        description: row.description || '',
        status: row.status
      }
    },
    insertPermissionData() {
      this.$refs.addPermissionForm.validate((valid) => {
        if (valid) {
          addPermission(this.addPermissionData).then(res => {
            if (res.data.code == 200) {
              this.$message.success("新增成功！")
              this.getPermissionTreeList()
              this.addShow = false
            } else {
              // 优先显示description，如果没有则显示message
              this.$message.error(res.data.description || res.data.message || "新增失败！")
            }
          }).catch(() => {
            this.$message.error("新增失败！")
          });
        }
      });
    },
    updatePermissionDataById() {
      this.$refs.updatePermissionForm.validate((valid) => {
        if (valid) {
          updatePermission(this.updatePermissionData).then(res => {
            if (res.data.code == 200) {
              this.$message.success("修改成功！")
              this.getPermissionTreeList()
              this.updateShow = false
            } else {
              // 优先显示description，如果没有则显示message
              this.$message.error(res.data.description || res.data.message || "修改失败！")
            }
          }).catch(() => {
            this.$message.error("修改失败！")
          });
        }
      });
    },
    deletePermissionById(row) {
      this.$confirm('此操作将永久删除该权限及其子权限, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deletePermission({ id: row.id }).then(res => {
          if (res.data.code == 200) {
            this.$message.success("删除成功！")
            this.getPermissionTreeList()
          } else {
            // 优先显示description，如果没有则显示message
            this.$message.error(res.data.description || res.data.message || "删除失败！")
          }
        }).catch(() => {
          this.$message.error("删除失败！")
        });
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消删除'
        });
      })
    },
  }
}
</script>

<style scoped>
.energyleft,
.energyright {
  display: inline-block;
}

.energyright {
  vertical-align: top !important;
  margin-bottom: 19px;
}

.el-table__body tr:hover>td {
  background-color: #e8f5e3 !important;
}

.el-table .el-table__expand-icon {
  color: #666;
}

.el-table .el-table__expand-icon:hover {
  color: #4CAF50;
}

.form-item-inline {
  font-size: 13px;
}

.el-table__fixed {
  height: 100% !important;
}

.main-box-admin {
  width: 94%;
  margin: 0 auto;
  text-align: left;
  padding: 5px; /* 从3%减少到5px，减少页面顶部间距 */
}

.flex-box-header-new {
  margin-left: 20px;
  margin-right: 15px;
  margin-top: 15px;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: start;
}
</style>


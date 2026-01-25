<template>
  <div class="main-box-admin">
    <el-breadcrumb separator="/" style="font-size: 18px; margin-bottom: 20px;">
      <el-breadcrumb-item :to="{ path: '/Tan/TanPage' }">
        <i class="el-icon-s-home"></i> 首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>角色管理</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="forest-card" style="padding: 20px; margin-bottom: 20px;">
      <div class="flex-box-header-new">
        <div class="energyleft">
          <el-form>
            <el-tag style="font-size: 13px;">角色编码：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchRoleCode" placeholder="请输入角色编码" size="small" style="width:150px"></el-input>
            </el-form-item>
            <el-tag style="font-size: 13px; margin-left: 10px;">角色名称：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchRoleName" placeholder="请输入角色名称" size="small" style="width:150px"></el-input>
            </el-form-item>
            <el-button class="form-item-inline" size="small" @click="getRoleList" plain
              icon="el-icon-search">查询</el-button>
            <el-button class="form-item-inline" type="success" size="small" @click="getAllRoleList" plain
              icon="el-icon-search">显示全部</el-button>
          </el-form>
        </div>
        <div style="margin-left: 50px;margin-bottom: 21px;">
          <el-form>
            <el-button class="form-item-inline" type="primary" size="small" @click="showAddDialog">新增角色</el-button>
          </el-form>
        </div>
      </div>
      <div class="table_container" style="width: 100%; overflow-x: auto; overflow-y: visible;">
        <el-table :data="tableData" :key="tableKey" style="width: 100%;font-size: 15px;" size="small" class="forest-table">
          <el-table-column label="序号" min-width="80" align="center">
            <template slot-scope="scope">
              {{ (scope.$index + 1) + (current - 1) * limit }}
            </template>
          </el-table-column>
          <el-table-column prop="roleCode" label="角色编码" min-width="150" align="center"></el-table-column>
          <el-table-column prop="roleName" label="角色名称" min-width="150" align="center"></el-table-column>
          <el-table-column prop="description" label="描述" min-width="250" align="center"></el-table-column>
          <el-table-column prop="status" label="状态" min-width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdTime" label="创建时间" min-width="180" align="center"></el-table-column>
          <el-table-column label="操作" min-width="320" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button @click="showUpdateDialog(scope.row)" type="warning" plain size="mini" icon="el-icon-edit" style="margin: 2px;">修改</el-button>
              <el-button @click="showAssignPermissionDialog(scope.row)" type="primary" plain size="mini" icon="el-icon-key" style="margin: 2px;">分配权限</el-button>
              <el-button @click="deleteRoleById(scope.row)" type="danger" plain size="mini" icon="el-icon-delete" style="margin: 2px;">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="width: 100%; text-align:center; margin:20px;padding-right: 50px;">
          <el-pagination background @current-change="handleCurrentChange" :page-size="limit" :current-page.sync="current"
            layout="total, prev, pager, next" :total="total" v-if="pageshow">
          </el-pagination>
        </div>
      </div>

      <!-- 新增角色对话框 -->
      <el-dialog title="新增角色" :visible.sync="addShow" :modal="false" top="10%" width="50%">
        <el-form :model="addRoleData" ref="addRoleForm" label-width="120px" :rules="roleRules">
          <el-form-item label="角色编码" prop="roleCode">
            <el-input v-model="addRoleData.roleCode" style="width: 300px" placeholder="请输入角色编码（如：ADMIN）"></el-input>
          </el-form-item>
          <el-form-item label="角色名称" prop="roleName">
            <el-input v-model="addRoleData.roleName" style="width: 300px" placeholder="请输入角色名称"></el-input>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="addRoleData.description" type="textarea" :rows="3" style="width: 300px" placeholder="请输入角色描述"></el-input>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="addRoleData.status" placeholder="请选择状态" style="width: 300px">
              <el-option label="启用" :value="1"></el-option>
              <el-option label="禁用" :value="0"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="insertRoleData">确认添加</el-button>
            <el-button @click="addShow = false">取消</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>

      <!-- 修改角色对话框 -->
      <el-dialog title="修改角色" :visible.sync="updateShow" :modal="false" top="10%" width="50%">
        <el-form :model="updateRoleData" ref="updateRoleForm" label-width="120px" :rules="roleRules">
          <el-form-item label="角色编码" prop="roleCode">
            <el-input v-model="updateRoleData.roleCode" style="width: 300px" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item label="角色名称" prop="roleName">
            <el-input v-model="updateRoleData.roleName" style="width: 300px" placeholder="请输入角色名称"></el-input>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="updateRoleData.description" type="textarea" :rows="3" style="width: 300px" placeholder="请输入角色描述"></el-input>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="updateRoleData.status" placeholder="请选择状态" style="width: 300px">
              <el-option label="启用" :value="1"></el-option>
              <el-option label="禁用" :value="0"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updateRoleDataById">确认修改</el-button>
            <el-button @click="updateShow = false">取消</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>

      <!-- 分配权限对话框 -->
      <el-dialog title="分配权限" :visible.sync="assignPermissionShow" :modal="false" top="5%" width="60%" @opened="handleAssignPermissionDialogOpened">
        <el-form label-width="120px">
          <el-form-item label="角色名称">
            <el-input v-model="assignPermissionData.roleName" :disabled="true" style="width: 300px"></el-input>
          </el-form-item>
          <el-form-item label="选择权限">
            <el-tree
              ref="permissionTree"
              :key="permissionTreeKey"
              :data="permissionTreeData"
              show-checkbox
              node-key="id"
              :default-expanded-keys="defaultExpandedKeys"
              :props="{ children: 'children', label: 'permissionName' }"
              :check-strictly="true"
              @check-change="handleCheckChange"
              style="max-height: 400px; overflow-y: auto;">
            </el-tree>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="assignPermissionDataById">确认分配</el-button>
            <el-button @click="assignPermissionShow = false">取消</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import { getRoleList, addRole, updateRole, deleteRole, assignPermissions, getPermissionTree, getRoleWithPermissions } from "../../api/role";
export default {
  data() {
    return {
      tableKey: 0, // 用于强制表格重新渲染
      tableData: [],
      current: 1,
      size: 10,
      limit: 10,
      total: 0,
      pageshow: true,
      addShow: false,
      updateShow: false,
      assignPermissionShow: false,

      addRoleData: {
        roleCode: '',
        roleName: '',
        description: '',
        status: 1
      },
      updateRoleData: {},
      assignPermissionData: {},

      searchRoleCode: '',
      searchRoleName: '',

      permissionTreeData: [],
      permissionTreeKey: 0, // 用于强制重新渲染树组件
      defaultExpandedKeys: [], // 默认展开的节点key列表

      roleRules: {
        roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
      }
    }
  },
  mounted: function () {
    this.currentPage = 1
    // 先调用getRoleList，如果成功再调用loadPermissionTree
    this.getRoleList().then(() => {
      // 只有在getRoleList成功时才调用loadPermissionTree
      this.loadPermissionTree()
      // 强制表格重新渲染以确保表头显示
      this.$nextTick(() => {
        this.tableKey++
      })
    }).catch(() => {
      // getRoleList失败时不再调用loadPermissionTree，避免重复弹窗
    })
  },
  activated() {
    // 只在组件激活时刷新数据，避免重复请求
    this.getRoleList().then(() => {
      // 强制表格重新渲染以确保表头显示
      this.$nextTick(() => {
        this.tableKey++
      })
    })
  },

  methods: {
    loadPermissionTree() {
      getPermissionTree().then(res => {
        if (res.data.code == 200) {
          this.permissionTreeData = res.data.data || []
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
    getAllRoleList() {
      this.searchRoleCode = ""
      this.searchRoleName = ""
      this.getRoleList();
    },
    getRoleList() {
      return getRoleList({
        current: this.current,
        size: this.size,
        roleCode: this.searchRoleCode,
        roleName: this.searchRoleName
      }).then(res => {
        if (res.data.code == 200) {
          this.tableData = res.data.data.records || []
          this.total = res.data.data.total || 0
          return Promise.resolve(res); // 返回成功的结果
        } else {
          // 40101错误已经在request.js中处理，这里不再重复提示
          if (res.data.code !== 40101) {
            // 优先显示description，如果没有则显示message
            this.$message.error(res.data.description || res.data.message || "获取角色列表失败！");
          }
          return Promise.reject(res); // 返回失败的结果
        }
      }).catch((error) => {
        // 40101错误已经在request.js中处理，这里不再重复提示
        if (!error.response || !error.response.data || error.response.data.code !== 40101) {
          this.$message.error("获取角色列表失败！");
        }
        return Promise.reject(error); // 继续抛出错误
      });
    },
    handleCurrentChange(val) {
      this.current = val
      this.getRoleList()
    },
    showAddDialog() {
      this.addShow = true;
      this.addRoleData = {
        roleCode: '',
        roleName: '',
        description: '',
        status: 1
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
      this.updateRoleData = {
        id: row.id,
        roleCode: row.roleCode,
        roleName: row.roleName,
        description: row.description,
        status: row.status
      }
    },
    showAssignPermissionDialog(row) {
      // 先重置权限数据
      this.assignPermissionData = {
        id: row.id,
        roleName: row.roleName,
        permissionIds: []
      };
      // 获取角色详情（包含权限）
      getRoleWithPermissions({ roleId: row.id }).then(res => {
        if (res.data.code == 200) {
          const roleData = res.data.data;
          this.assignPermissionData = {
            id: roleData.id,
            roleName: roleData.roleName,
            permissionIds: roleData.permissions ? this.flattenPermissionIds(roleData.permissions) : []
          };
          // 数据加载完成后再打开对话框，确保数据已准备好
          this.assignPermissionShow = true;
        } else {
          // 40101错误已经在request.js中处理，这里不再重复提示
          if (res.data.code !== 40101) {
            // 优先显示description，如果没有则显示message
            this.$message.error(res.data.description || res.data.message || "获取角色权限失败！");
          }
        }
      }).catch((error) => {
        // 40101错误已经在request.js中处理，这里不再重复提示
        if (!error.response || !error.response.data || error.response.data.code !== 40101) {
          this.$message.error("获取角色权限失败！");
        }
      });
    },
    handleAssignPermissionDialogOpened() {
      // 对话框打开后，强制重新渲染树组件并设置选中项
      // 先设置默认展开的节点key列表
      this.defaultExpandedKeys = this.getAllNodeKeys(this.permissionTreeData);
      this.permissionTreeKey += 1;
      this.$nextTick(() => {
        if (this.$refs.permissionTree && this.assignPermissionData.permissionIds) {
          // 设置选中项，只设置叶子节点的选中状态，避免父节点被自动勾选
          this.$refs.permissionTree.setCheckedKeys(this.assignPermissionData.permissionIds, true);
        }
      });
    },
    handleCheckChange(data, checked, indeterminate) {
      // 获取树组件实例
      const tree = this.$refs.permissionTree;
      if (!tree) return;

      // 递归更新父节点的选中状态
      this.updateParentCheckState(data, tree);
    },
    updateParentCheckState(node, tree) {
      // 如果节点没有父节点，直接返回
      if (!node.parent) return;

      // 获取父节点的所有子节点
      const parent = node.parent;
      const children = parent.childNodes;

      // 检查所有子节点是否都被选中
      let allChecked = true;
      let anyChecked = false;

      children.forEach(child => {
        if (child.checked) {
          anyChecked = true;
        } else {
          allChecked = false;
        }
      });

      // 更新父节点的选中状态
      if (allChecked) {
        tree.setChecked(parent.key, true, false);
      } else {
        tree.setChecked(parent.key, false, false);
      }

      // 递归更新上级父节点
      this.updateParentCheckState(parent, tree);
    },
    getAllNodeKeys(nodes) {
      // 递归获取所有节点的 key，用于展开所有节点
      let keys = [];
      if (nodes && nodes.length > 0) {
        nodes.forEach(node => {
          keys.push(node.id);
          if (node.children && node.children.length > 0) {
            keys = keys.concat(this.getAllNodeKeys(node.children));
          }
        });
      }
      return keys;
    },
    flattenPermissionIds(permissions) {
      let ids = [];
      permissions.forEach(perm => {
        if (!perm.children || perm.children.length === 0) {
          ids.push(perm.id);
        }
        if (perm.children && perm.children.length > 0) {
          ids = ids.concat(this.flattenPermissionIds(perm.children));
        }
      });
      return ids;
    },
    insertRoleData() {
      this.$refs.addRoleForm.validate((valid) => {
        if (valid) {
          addRole(this.addRoleData).then(res => {
            if (res.data.code == 200) {
              this.$message.success("新增成功！")
              this.getRoleList()
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
    updateRoleDataById() {
      this.$refs.updateRoleForm.validate((valid) => {
        if (valid) {
          updateRole(this.updateRoleData).then(res => {
            if (res.data.code == 200) {
              this.$message.success("修改成功！")
              this.getRoleList()
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
    assignPermissionDataById() {
      // 只获取完全选中的节点（不包括半选中的父节点）
      const checkedKeys = this.$refs.permissionTree.getCheckedKeys();
      
      if (checkedKeys.length === 0) {
        this.$message.warning("请至少选择一个权限！");
        return;
      }
      
      const assignData = {
        id: this.assignPermissionData.id,
        permissions: checkedKeys.map(permId => ({ id: permId }))
      };
      
      assignPermissions(assignData).then(res => {
        if (res.data.code == 200) {
          this.$message.success("分配成功！")
          // 分配成功后，重新获取角色权限数据并更新，确保下次打开对话框时显示最新数据
          getRoleWithPermissions({ roleId: this.assignPermissionData.id }).then(roleRes => {
            if (roleRes.data.code == 200) {
              const roleData = roleRes.data.data;
              // 更新 assignPermissionData，确保下次打开对话框时显示最新数据
              this.assignPermissionData = {
                id: roleData.id,
                roleName: roleData.roleName,
                permissionIds: roleData.permissions ? this.flattenPermissionIds(roleData.permissions) : []
              };
            }
          }).catch(() => {
            // 获取角色权限失败，不影响分配成功的提示
          });
          // 关闭对话框
          this.assignPermissionShow = false;
          // 分配成功后，刷新角色列表
          this.getRoleList()
        } else {
          // 优先显示description，如果没有则显示message
          this.$message.error(res.data.description || res.data.message || "分配失败！")
        }
      }).catch(() => {
        this.$message.error("分配失败！")
      });
    },
    deleteRoleById(row) {
      this.$confirm('此操作将永久删除该角色, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteRole({ id: row.id }).then(res => {
          if (res.data.code == 200) {
            this.$message.success("删除成功！")
            this.getRoleList()
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
.cell {
  text-align: center;
}

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


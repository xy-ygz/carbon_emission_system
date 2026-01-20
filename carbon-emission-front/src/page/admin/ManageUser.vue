<template>
  <div class="main-box-admin">
    <el-breadcrumb separator="/" style="font-size: 18px; margin-bottom: 20px;">
      <el-breadcrumb-item :to="{ path: '/Tan/TanPage' }">
        <i class="el-icon-s-home"></i> 首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>用户管理</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="forest-card" style="padding: 20px; margin-bottom: 20px;">
      <div class="flex-box-header-new">
        <div class="energyleft">
          <el-form>
            <el-tag style="font-size: 13px;">姓名：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchName" placeholder="请输入姓名" size="small" style="width:150px"></el-input>
            </el-form-item>
            <el-tag style="font-size: 13px; margin-left: 10px;">用户名：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchUsername" placeholder="请输入用户名" size="small" style="width:150px"></el-input>
            </el-form-item>
            <el-tag style="font-size: 13px; margin-left: 10px;">部门：</el-tag>
            <el-form-item style="display: inline-block">
              <el-input v-model="searchDepartment" placeholder="请输入部门" size="small" style="width:150px"></el-input>
            </el-form-item>
            <el-button class="form-item-inline" size="small" @click="getUserList" plain
              icon="el-icon-search">查询</el-button>
            <el-button class="form-item-inline" type="success" size="small" @click="getAllUserList" plain
              icon="el-icon-search">显示全部</el-button>
          </el-form>
        </div>
        <div style="margin-left: 50px;margin-bottom: 21px;">
          <el-form>
            <el-button class="form-item-inline" type="primary" size="small" @click="showAddDialog">新增用户</el-button>
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
          <el-table-column prop="name" label="姓名" min-width="120" align="center"></el-table-column>
          <el-table-column prop="username" label="用户名" min-width="150" align="center"></el-table-column>
          <el-table-column prop="department" label="部门" min-width="150" align="center"></el-table-column>
          <el-table-column prop="phone" label="联系方式" min-width="150" align="center"></el-table-column>
          <el-table-column prop="status" label="状态" min-width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="roles" label="角色" min-width="200" align="center">
            <template slot-scope="scope">
              <el-tag v-for="role in scope.row.roles" :key="role.id" size="small" style="margin-right: 5px;">
                {{ role.roleName }}
              </el-tag>
              <span v-if="!scope.row.roles || scope.row.roles.length === 0" style="color: #999;">未分配</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdTime" label="创建时间" min-width="180" align="center"></el-table-column>
          <el-table-column label="操作" min-width="450" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button @click="showUpdateDialog(scope.row)" type="warning" plain size="mini" icon="el-icon-edit" style="margin: 2px;">修改</el-button>
              <el-button @click="showAssignRoleDialog(scope.row)" type="primary" plain size="mini" icon="el-icon-user" style="margin: 2px;">分配角色</el-button>
              <el-button @click="resetUserPassword(scope.row)" type="info" plain size="mini" icon="el-icon-key" style="margin: 2px;">重置密码</el-button>
              <el-button @click="deleteUserById(scope.row)" type="danger" plain size="mini" icon="el-icon-delete" style="margin: 2px;">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="width: 100%; text-align:center; margin:20px;padding-right: 50px;">
          <el-pagination background @current-change="handleCurrentChange" :page-size="limit" :current-page.sync="current"
            layout="total, prev, pager, next" :total="total" v-if="pageshow">
          </el-pagination>
        </div>
      </div>

      <!-- 新增用户对话框 -->
      <el-dialog title="新增用户" :visible.sync="addShow" :modal="false" top="10%" width="50%">
        <el-form :model="addUserData" ref="addUserForm" label-width="120px" :rules="userRules">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="addUserData.name" style="width: 300px" placeholder="请输入姓名"></el-input>
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="addUserData.username" style="width: 300px" placeholder="请输入用户名"></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="addUserData.password" type="password" style="width: 300px" placeholder="请输入密码"></el-input>
          </el-form-item>
          <el-form-item label="部门" prop="department">
            <el-input v-model="addUserData.department" style="width: 300px" placeholder="请输入部门"></el-input>
          </el-form-item>
          <el-form-item label="联系方式" prop="phone">
            <el-input v-model="addUserData.phone" style="width: 300px" placeholder="请输入联系方式"></el-input>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="addUserData.status" placeholder="请选择状态" style="width: 300px">
              <el-option label="启用" :value="1"></el-option>
              <el-option label="禁用" :value="0"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="角色" prop="roleIds">
            <el-select v-model="addUserData.roleIds" multiple placeholder="请选择角色" style="width: 300px">
              <el-option 
                v-for="role in roleList" 
                :key="role.id" 
                :label="role.roleName" 
                :value="role.id"
                :disabled="isRoleDisabled(role)">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="insertUserData">确认添加</el-button>
            <el-button @click="addShow = false">取消</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>

      <!-- 修改用户对话框 -->
      <el-dialog title="修改用户" :visible.sync="updateShow" :modal="false" top="10%" width="50%">
        <el-form :model="updateUserData" ref="updateUserForm" label-width="120px" :rules="userUpdateRules">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="updateUserData.name" style="width: 300px" placeholder="请输入姓名"></el-input>
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="updateUserData.username" style="width: 300px" placeholder="请输入用户名" :disabled="true"></el-input>
          </el-form-item>
          <el-form-item label="部门" prop="department">
            <el-input v-model="updateUserData.department" style="width: 300px" placeholder="请输入部门"></el-input>
          </el-form-item>
          <el-form-item label="联系方式" prop="phone">
            <el-input v-model="updateUserData.phone" style="width: 300px" placeholder="请输入联系方式"></el-input>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="updateUserData.status" placeholder="请选择状态" style="width: 300px">
              <el-option label="启用" :value="1"></el-option>
              <el-option label="禁用" :value="0"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="角色" prop="roles">
            <el-select v-model="updateUserData.roleIds" multiple placeholder="请选择角色" style="width: 300px">
              <el-option 
                v-for="role in roleList" 
                :key="role.id" 
                :label="role.roleName" 
                :value="role.id"
                :disabled="isRoleDisabled(role)">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updateUserDataById">确认修改</el-button>
            <el-button @click="updateShow = false">取消</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>

      <!-- 分配角色对话框 -->
      <el-dialog title="分配角色" :visible.sync="assignRoleShow" :modal="false" top="15%" width="40%">
        <el-form label-width="120px">
          <el-form-item label="用户姓名">
            <el-input v-model="assignRoleData.name" :disabled="true" style="width: 300px"></el-input>
          </el-form-item>
          <el-form-item label="选择角色">
            <el-select v-model="assignRoleData.roleIds" multiple placeholder="请选择角色" style="width: 300px">
              <el-option 
                v-for="role in roleList" 
                :key="role.id" 
                :label="role.roleName" 
                :value="role.id"
                :disabled="isRoleDisabled(role)">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="assignRoleDataById">确认分配</el-button>
            <el-button @click="assignRoleShow = false">取消</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import { getUserList, addUser, updateUser, deleteUser, resetPassword, assignRoles, getAllRoles, getCurrentUserRoles } from "../../api/userManage";
import { extractPermissionsFromRoles, hasPermission } from "../../utils/permission";
export default {
  data() {
    return {
      tableData: [],
      current: 1,
      size: 10,
      limit: 10,
      total: 0,
      pageshow: true,
      addShow: false,
      updateShow: false,
      assignRoleShow: false,

      addUserData: {
        name: '',
        username: '',
        password: '',
        department: '',
        phone: '',
        status: 1,
        roleIds: []
      },
      updateUserData: {},
      assignRoleData: {},

      searchName: '',
      searchUsername: '',
      searchDepartment: '',

      roleList: [],
      currentUserRoles: [], // 当前登录用户的角色列表
      currentUserMinOrder: 999, // 当前用户的最低权限等级（order值，越小等级越高）
      tableKey: 0, // 用于强制表格重新渲染
      userPermissions: [], // 当前用户的权限编码列表

      userRules: {
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      userUpdateRules: {
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
      }
    }
  },
  mounted: function () {
    this.currentPage = 1
    // 先加载当前用户角色信息（用于权限等级判断）
    this.loadCurrentUserRoles()
    // 加载用户权限
    this.loadUserPermissions()
    // 先调用getUserList，如果成功再调用loadRoleList
    this.getUserList().then(() => {
      // 只有在getUserList成功时才调用loadRoleList
      this.loadRoleList()
      // 强制表格重新渲染以确保表头显示
      this.$nextTick(() => {
        this.tableKey++
      })
    }).catch(() => {
      // getUserList失败时不再调用loadRoleList，避免重复弹窗
    })
  },
  activated() {
    // 只在组件激活时刷新数据，避免重复请求
    this.getUserList().then(() => {
      // 强制表格重新渲染以确保表头显示
      this.$nextTick(() => {
        this.tableKey++
      })
    })
  },

  methods: {
    loadRoleList() {
      getAllRoles().then(res => {
        if (res.data.code == 200) {
          this.roleList = res.data.data || []
        } else {
          // 40101错误已经在request.js中处理，这里不再重复提示
          if (res.data.code !== 40101) {
            this.$message.error(res.data.message || "获取角色列表失败！");
          }
        }
      }).catch((error) => {
        // 40101错误已经在request.js中处理，这里不再重复提示
        if (!error.response || !error.response.data || error.response.data.code !== 40101) {
          this.$message.error("获取角色列表失败！");
        }
      });
    },
    loadCurrentUserRoles() {
      getCurrentUserRoles().then(res => {
        if (res.data.code == 200) {
          this.currentUserRoles = res.data.data || []
          // 计算当前用户的最低权限等级（order值，越小等级越高）
          if (this.currentUserRoles.length > 0) {
            const orders = this.currentUserRoles
              .map(role => role.order !== null && role.order !== undefined ? role.order : 999)
              .filter(order => order !== null && order !== undefined)
            this.currentUserMinOrder = orders.length > 0 ? Math.min(...orders) : 999
          } else {
            this.currentUserMinOrder = 999
          }
        } else {
          // 40101错误已经在request.js中处理，这里不再重复提示
          if (res.data.code !== 40101) {
            this.$message.error(res.data.message || "获取当前用户角色失败！");
          }
        }
      }).catch((error) => {
        // 40101错误已经在request.js中处理，这里不再重复提示
        if (!error.response || !error.response.data || error.response.data.code !== 40101) {
          this.$message.error("获取当前用户角色失败！");
        }
      });
    },
    // 加载当前用户权限
    loadUserPermissions() {
      getCurrentUserRoles().then(res => {
        if (res.data.code == 200) {
          const roles = res.data.data || [];
          this.userPermissions = extractPermissionsFromRoles(roles);
        }
      }).catch(() => {
        // 权限加载失败，不影响页面显示
        this.userPermissions = [];
      });
    },
    // 检查是否有指定权限
    checkPermission(permissionCode) {
      return hasPermission(this.userPermissions, permissionCode);
    },
    // 判断角色是否应该被禁用（权限等级高于当前用户）
    isRoleDisabled(role) {
      // 如果角色没有order字段，默认不禁用
      if (role.order === null || role.order === undefined) {
        return false
      }
      // 如果角色的order < 当前用户的order，说明角色权限更高，应该禁用
      return role.order < this.currentUserMinOrder
    },
    getAllUserList() {
      this.searchName = ""
      this.searchUsername = ""
      this.searchDepartment = ""
      this.getUserList();
    },
    getUserList() {
      return getUserList({
        current: this.current,
        size: this.size,
        name: this.searchName,
        username: this.searchUsername,
        department: this.searchDepartment
      }).then(res => {
        if (res.data.code == 200) {
          this.tableData = res.data.data.records || []
          this.total = res.data.data.total || 0
          return Promise.resolve(res); // 返回成功的结果
        } else {
          // 40101错误已经在request.js中处理，这里不再重复提示
          if (res.data.code !== 40101) {
            this.$message.error(res.data.message || "获取用户列表失败！");
          }
          return Promise.reject(res); // 返回失败的结果
        }
      }).catch((error) => {
        // 40101错误已经在request.js中处理，这里不再重复提示
        if (!error.response || !error.response.data || error.response.data.code !== 40101) {
          this.$message.error("获取用户列表失败！");
        }
        return Promise.reject(error); // 继续抛出错误
      });
    },
    handleCurrentChange(val) {
      this.current = val
      this.getUserList()
    },
    showAddDialog() {
      this.addShow = true;
      this.addUserData = {
        name: '',
        username: '',
        password: '',
        department: '',
        phone: '',
        status: 1,
        roleIds: []
      }
      // 弹窗打开后，自动聚焦到第一个输入框
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
      this.updateUserData = {
        id: row.id,
        name: row.name,
        username: row.username,
        department: row.department,
        phone: row.phone,
        status: row.status,
        roleIds: row.roles ? row.roles.map(r => r.id) : []
      }
      // 弹窗打开后，自动聚焦到第一个输入框
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
    showAssignRoleDialog(row) {
      this.assignRoleShow = true;
      this.assignRoleData = {
        id: row.id,
        name: row.name,
        roleIds: row.roles ? row.roles.map(r => r.id) : []
      }
      // 弹窗打开后，自动聚焦到选择框
      this.$nextTick(() => {
        setTimeout(() => {
          const firstInput = this.$el.querySelector('.el-dialog__body .el-input__inner, .el-dialog__body .el-select');
          if (firstInput) {
            if (firstInput.classList && firstInput.classList.contains('el-select')) {
              // 如果是选择框，聚焦到输入框
              const selectInput = firstInput.querySelector('.el-input__inner');
              if (selectInput) {
                selectInput.focus();
              }
            } else {
              firstInput.focus();
            }
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
    insertUserData() {
      this.$refs.addUserForm.validate((valid) => {
        if (valid) {
          addUser(this.addUserData).then(res => {
            if (res.data.code == 200) {
              this.$message.success("新增成功！")
              this.getUserList()
              this.addShow = false
            } else {
              this.$message.error(res.data.message || "新增失败！")
            }
          }).catch(() => {
            this.$message.error("新增失败！")
          });
        }
      });
    },
    updateUserDataById() {
      this.$refs.updateUserForm.validate((valid) => {
        if (valid) {
          const updateData = {
            id: this.updateUserData.id,
            name: this.updateUserData.name,
            username: this.updateUserData.username,
            department: this.updateUserData.department,
            phone: this.updateUserData.phone,
            status: this.updateUserData.status,
            roles: this.updateUserData.roleIds.map(roleId => {
              const role = this.roleList.find(r => r.id === roleId);
              return role ? { id: roleId, roleName: role.roleName } : { id: roleId };
            })
          };
          updateUser(updateData).then(res => {
            if (res.data.code == 200) {
              this.$message.success("修改成功！")
              this.getUserList()
              this.updateShow = false
            } else {
              this.$message.error(res.data.message || "修改失败！")
            }
          }).catch(() => {
            this.$message.error("修改失败！")
          });
        }
      });
    },
    assignRoleDataById() {
      if (!this.assignRoleData.roleIds || this.assignRoleData.roleIds.length === 0) {
        this.$message.warning("请至少选择一个角色！");
        return;
      }
      const assignData = {
        id: this.assignRoleData.id,
        roles: this.assignRoleData.roleIds.map(roleId => {
          const role = this.roleList.find(r => r.id === roleId);
          return role ? { id: roleId, roleName: role.roleName } : { id: roleId };
        })
      };
      assignRoles(assignData).then(res => {
        if (res.data.code == 200) {
          this.$message.success("分配成功！")
          this.getUserList()
          this.assignRoleShow = false
        } else {
          this.$message.error(res.data.message || "分配失败！")
        }
      }).catch(() => {
        this.$message.error("分配失败！")
      });
    },
    resetUserPassword(row) {
      this.$confirm('确定要重置该用户的密码为"123456"吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        resetPassword({ id: row.id }).then(res => {
          if (res.data.code == 200) {
            this.$message.success("密码已重置为123456！")
          } else {
            this.$message.error(res.data.message || "重置失败！")
          }
        }).catch(() => {
          this.$message.error("重置失败！")
        });
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消操作'
        });
      })
    },
    deleteUserById(row) {
      this.$confirm('此操作将永久删除该用户, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteUser({ id: row.id }).then(res => {
          if (res.data.code == 200) {
            this.$message.success("删除成功！")
            this.getUserList()
          } else {
            this.$message.error(res.data.message || "删除失败！")
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

/* .main-box-admin 样式已在全局 common.css 中统一定义 */

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


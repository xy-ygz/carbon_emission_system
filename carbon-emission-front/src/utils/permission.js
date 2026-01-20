/**
 * 权限工具函数
 * 用于检查用户是否有特定权限
 */

/**
 * 检查用户是否有指定权限
 * @param {Array} userPermissions - 用户权限列表（权限编码数组）
 * @param {String} permissionCode - 要检查的权限编码
 * @returns {Boolean} 是否有权限
 */
export function hasPermission(userPermissions, permissionCode) {
  if (!userPermissions || !Array.isArray(userPermissions)) {
    return false;
  }
  // 检查是否有完全匹配的权限
  if (userPermissions.includes(permissionCode)) {
    return true;
  }
  // 检查是否有父权限（例如有 PERMISSION_MANAGE 权限，则拥有所有 PERMISSION_MANAGE_* 权限）
  return userPermissions.some(perm => {
    return perm === permissionCode || perm.startsWith(permissionCode.split('_')[0] + '_');
  });
}

/**
 * 从用户角色中提取权限编码列表
 * @param {Array} userRoles - 用户角色列表
 * @returns {Array} 权限编码列表
 */
export function extractPermissionsFromRoles(userRoles) {
  if (!userRoles || !Array.isArray(userRoles)) {
    return [];
  }
  
  const permissions = [];
  userRoles.forEach(role => {
    if (role.permissions && Array.isArray(role.permissions)) {
      role.permissions.forEach(perm => {
        if (perm.permissionCode && !permissions.includes(perm.permissionCode)) {
          permissions.push(perm.permissionCode);
        }
        // 递归处理子权限
        if (perm.children && Array.isArray(perm.children)) {
          extractPermissionsRecursive(perm.children, permissions);
        }
      });
    }
  });
  
  return permissions;
}

/**
 * 递归提取权限编码
 * @param {Array} permissions - 权限列表
 * @param {Array} result - 结果数组
 */
function extractPermissionsRecursive(permissions, result) {
  permissions.forEach(perm => {
    if (perm.permissionCode && !result.includes(perm.permissionCode)) {
      result.push(perm.permissionCode);
    }
    if (perm.children && Array.isArray(perm.children)) {
      extractPermissionsRecursive(perm.children, result);
    }
  });
}


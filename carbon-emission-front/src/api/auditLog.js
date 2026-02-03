import request from '../utils/request'

/**
 * 获取日志列表
 */
export function getLogList(params) {
  return request({
    url: '/api/auditLog/getLogList',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询日志
 */
export function getLogById(id) {
  return request({
    url: '/api/auditLog/getLogById',
    method: 'get',
    params: { id }
  })
}

/**
 * 新增日志
 */
export function addLog(data) {
  return request({
    url: '/api/auditLog/addLog',
    method: 'post',
    data
  })
}

/**
 * 更新日志
 */
export function updateLog(data) {
  return request({
    url: '/api/auditLog/updateLog',
    method: 'post',
    data
  })
}

/**
 * 删除日志
 */
export function deleteLog(id) {
  return request({
    url: '/api/auditLog/deleteLog',
    method: 'get',
    params: { id }
  })
}

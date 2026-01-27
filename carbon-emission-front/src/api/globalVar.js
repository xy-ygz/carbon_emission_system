export function publicNetworkIpAndPort() {
   // 开发环境：使用完整 URL（或者自己配置proxyTable代理）
   // return "http://localhost";
   
   // 生产环境：使用相对路径，让浏览器自动使用当前域名
   // nginx 会通过容器名 carbon-backend 代理到后端
   return ""; // 空字符串表示使用相对路径，浏览器会自动使用当前域名
}

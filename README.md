# 碳排放管理系统

一个UI精简、优美且前后端分离的碳排放管理系统，可视化管理碳排放数据。
<br>读研期间接的学校的系统开发项目，麻雀虽小，五脏俱全。<br>
系统集成了JWT无状态认证、RBAC角色权限校验、数据可视化、文件流导出、线程池异步处理、缓存限流、AOP自定义切面注解、设计模式等最佳实践。
![tangif2](https://github.com/user-attachments/assets/5deadc68-dd48-4420-bd11-dea2ec9d3454)


## 🛠 技术栈

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 1.8 | 开发语言 |
| Spring Boot | 2.6.4 | 核心框架 |
| Spring Security | 2.6.4 | 安全框架 |
| MyBatis Plus | 3.5.1 | ORM框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7 | 缓存和限流 |
| JWT | 0.9.1 | 无状态认证 |
| Apache POI | 5.1.0 | Office文档处理 |
| Apache PDFBox | 2.0.28 | PDF生成 |
| docx4j | 8.3.15 | Word文档处理 |
| EasyExcel | 2.2.10 | Excel处理 |
| Hutool | 5.8.12 | Java工具类库 |
| FastJSON | 1.2.80 | JSON处理 |
| JFreeChart | 1.5.3 | 图表生成 |
| ECharts Java | 3.0.0.6 | ECharts封装 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 2.5.2 | 前端框架 |
| Element UI | 2.15.6 | UI组件库 |
| ECharts | 5.3.1 | 数据可视化 |
| Axios | 0.26.1 | HTTP客户端 |
| Vue Router | 3.0.1 | 路由管理 |
| Webpack | 3.6.0 | 构建工具 |
| Less | 4.1.2 | CSS预处理器 |

### 部署技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Docker | Latest | 容器化 |
| Docker Compose | Latest | 容器编排 |
| Nginx | 1.20-alpine | Web服务器 |
| Tomcat | 9.0-jdk8 | 应用服务器 |

## 📦 一键部署
### 快速开始
需要提前安装`docker`及`docker-compose`，安装脚本：`./docker/install-docker.sh `

一键部署脚本，只需执行以下命令：
![1月20日](https://github.com/user-attachments/assets/d2b7bc74-894f-40e2-85d1-0c289f3eeaa4)
```bash
# 进入docker目录
cd docker

# 赋予执行权限（Linux/Mac）
chmod +x deploy.sh

# 执行一键部署脚本
./deploy.sh
```

### 部署脚本功能

`deploy.sh`脚本会自动完成以下操作：

1. **环境检查**：检查Maven、npm、Docker等必要工具
2. **后端打包**：使用Maven打包后端项目，生成WAR包
3. **前端打包**：使用npm构建前端项目，生成dist目录
4. **文件复制**：将打包产物复制到docker目录
5. **容器启动**：使用Docker Compose启动所有服务


## 许可证

本项目采用 [LICENSE](LICENSE) 许可证。

## 后话

项目很轻量，主要是提供平台，欢迎提交 Issue 和 Pull Request 以此为基，添加你觉得能有价值的功能点，展示技术实力，一起成为本项目的开源贡献者。

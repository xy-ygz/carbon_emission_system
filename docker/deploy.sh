#!/bin/bash

# 一键部署脚本
# 功能：
# 1. 打包后端项目并复制 war 包到 docker/backend
# 2. 打包前端项目并复制 dist 目录到 docker/frontend
# 3. 启动 docker-compose 进行部署

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目根目录（脚本所在目录的父目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_ROOT/carbon-emission-backend"
FRONTEND_DIR="$PROJECT_ROOT/carbon-emission-front"
DOCKER_DIR="$SCRIPT_DIR"
BACKEND_TARGET="$DOCKER_DIR/backend"
FRONTEND_TARGET="$DOCKER_DIR/frontend"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}开始一键部署流程${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查必要的工具
echo -e "${YELLOW}[1/5] 检查必要的工具...${NC}"
command -v mvn >/dev/null 2>&1 || { echo -e "${RED}错误: 未找到 mvn 命令，请先安装 Maven${NC}" >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo -e "${RED}错误: 未找到 npm 命令，请先安装 Node.js${NC}" >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo -e "${RED}错误: 未找到 docker 命令，请先安装 Docker${NC}" >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo -e "${RED}错误: 未找到 docker-compose 命令，请先安装 Docker Compose${NC}" >&2; exit 1; }
echo -e "${GREEN}✓ 工具检查完成${NC}"

# 步骤1: 打包后端项目
echo -e "${YELLOW}[2/5] 打包后端项目...${NC}"
cd "$BACKEND_DIR"
echo "当前目录: $(pwd)"
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}错误: 未找到 pom.xml 文件${NC}" >&2
    exit 1
fi

echo "执行 Maven 打包..."
mvn clean package -DskipTests

# 检查 war 包是否生成
WAR_FILE="$BACKEND_DIR/target/sys_carbon-0.0.1-SNAPSHOT.war"
if [ ! -f "$WAR_FILE" ]; then
    echo -e "${RED}错误: 未找到打包后的 war 文件: $WAR_FILE${NC}" >&2
    exit 1
fi

# 复制 war 包到 docker/backend 目录
echo "复制 war 包到 $BACKEND_TARGET..."
mkdir -p "$BACKEND_TARGET"
cp "$WAR_FILE" "$BACKEND_TARGET/"
echo -e "${GREEN}✓ 后端打包完成，war 包已复制到 $BACKEND_TARGET/${NC}"

# 步骤2: 打包前端项目
echo -e "${YELLOW}[3/5] 打包前端项目...${NC}"
cd "$FRONTEND_DIR"
echo "当前目录: $(pwd)"
if [ ! -f "package.json" ]; then
    echo -e "${RED}错误: 未找到 package.json 文件${NC}" >&2
    exit 1
fi

# 检查 node_modules 是否存在，如果不存在则安装依赖
if [ ! -d "node_modules" ]; then
    echo "未找到 node_modules，正在安装依赖..."
    npm install
fi

echo "执行 npm 构建..."
npm run build

# 检查 dist 目录是否生成
DIST_DIR="$FRONTEND_DIR/dist"
if [ ! -d "$DIST_DIR" ]; then
    echo -e "${RED}错误: 未找到打包后的 dist 目录: $DIST_DIR${NC}" >&2
    exit 1
fi

# 复制 dist 目录到 docker/frontend 目录
echo "复制 dist 目录到 $FRONTEND_TARGET..."
mkdir -p "$FRONTEND_TARGET"
# 删除旧的 dist 目录（如果存在）
if [ -d "$FRONTEND_TARGET/dist" ]; then
    rm -rf "$FRONTEND_TARGET/dist"
fi
cp -r "$DIST_DIR" "$FRONTEND_TARGET/"
echo -e "${GREEN}✓ 前端打包完成，dist 目录已复制到 $FRONTEND_TARGET/${NC}"

# 步骤3: 检查 docker-compose.yml 路径配置
echo -e "${YELLOW}[4/5] 检查 docker-compose.yml 配置...${NC}"
cd "$DOCKER_DIR"
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}错误: 未找到 docker-compose.yml 文件${NC}" >&2
    exit 1
fi

# 验证必要的文件是否存在
if [ ! -f "$BACKEND_TARGET/sys_carbon-0.0.1-SNAPSHOT.war" ]; then
    echo -e "${RED}错误: 后端 war 包不存在: $BACKEND_TARGET/sys_carbon-0.0.1-SNAPSHOT.war${NC}" >&2
    exit 1
fi

if [ ! -d "$FRONTEND_TARGET/dist" ]; then
    echo -e "${RED}错误: 前端 dist 目录不存在: $FRONTEND_TARGET/dist${NC}" >&2
    exit 1
fi

if [ ! -f "$FRONTEND_TARGET/default.conf" ]; then
    echo -e "${YELLOW}警告: 未找到 frontend/default.conf 配置文件${NC}"
fi

echo -e "${GREEN}✓ 配置检查完成${NC}"

# 步骤4: 启动 docker-compose
echo -e "${YELLOW}[5/5] 启动 Docker 容器...${NC}"
cd "$DOCKER_DIR"

# 停止并删除旧容器（如果存在）
echo "停止现有容器..."
docker-compose down 2>/dev/null || true

# 启动容器
echo "启动容器..."
docker-compose up -d

# 等待容器启动
echo "等待容器启动..."
sleep 5

# 检查容器状态
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}部署完成！容器状态：${NC}"
echo -e "${GREEN}========================================${NC}"
docker-compose ps

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}访问地址：${NC}"
echo -e "${GREEN}前端: http://localhost:4432${NC}"
echo -e "${GREEN}后端: http://localhost:12306/api${NC}"
echo -e "${GREEN}数据库: localhost:4436${NC}"
echo -e "${GREEN}========================================${NC}"

#!/bin/bash

# Docker 和 Docker Compose 一键安装脚本
# 适用于 Alibaba Cloud Linux 8 / CentOS 8
# 功能：
# 1. 安装 Docker Engine
# 2. 安装 Docker Compose
# 3. 启动并配置 Docker 服务
# 4. 验证安装

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Docker 和 Docker Compose 安装脚本${NC}"
echo -e "${GREEN}适用于 Alibaba Cloud Linux 8 / CentOS 8${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}错误: 请使用 root 用户或 sudo 权限运行此脚本${NC}" >&2
    exit 1
fi

# 检测系统类型
echo -e "${YELLOW}[1/6] 检测系统信息...${NC}"
OS_RELEASE=$(cat /etc/os-release | grep "^ID=" | cut -d'=' -f2 | tr -d '"')
OS_VERSION=$(cat /etc/os-release | grep "^VERSION_ID=" | cut -d'=' -f2 | tr -d '"')
echo -e "${BLUE}操作系统: $OS_RELEASE $OS_VERSION${NC}"

# 检查 Docker 是否已安装
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    echo -e "${YELLOW}检测到已安装 Docker: $DOCKER_VERSION${NC}"
    read -p "是否重新安装？(y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${GREEN}跳过 Docker 安装${NC}"
        SKIP_DOCKER=true
    else
        echo -e "${YELLOW}卸载旧版本 Docker...${NC}"
        yum remove -y docker docker-client docker-client-latest docker-common \
            docker-latest docker-latest-logrotate docker-logrotate docker-engine 2>/dev/null || true
        SKIP_DOCKER=false
    fi
else
    SKIP_DOCKER=false
fi

# 步骤1: 安装必要的依赖
if [ "$SKIP_DOCKER" = false ]; then
    echo -e "${YELLOW}[2/6] 安装必要的依赖包...${NC}"
    yum install -y yum-utils device-mapper-persistent-data lvm2
    echo -e "${GREEN}✓ 依赖包安装完成${NC}"
    
    # 步骤2: 添加 Docker 仓库（优先使用国内镜像源）
    echo -e "${YELLOW}[3/6] 添加 Docker 仓库（优先使用国内镜像源）...${NC}"
    
    # 检测是否为阿里云服务器
    IS_ALIYUN=false
    if [ -f /etc/system-release ]; then
        if grep -q "Alibaba Cloud Linux" /etc/system-release 2>/dev/null; then
            IS_ALIYUN=true
            echo -e "${BLUE}检测到阿里云服务器，优先使用阿里云镜像源${NC}"
        fi
    fi
    
    # 优先使用国内镜像源列表
    MIRROR_REPOS=(
        "https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo"
        "https://mirrors.tuna.tsinghua.edu.cn/docker-ce/linux/centos/docker-ce.repo"
        "https://mirrors.ustc.edu.cn/docker-ce/linux/centos/docker-ce.repo"
        "https://download.docker.com/linux/centos/docker-ce.repo"
    )
    
    REPO_ADDED=false
    for repo_url in "${MIRROR_REPOS[@]}"; do
        echo -e "${BLUE}尝试添加仓库: $repo_url${NC}"
        if yum-config-manager --add-repo "$repo_url" 2>/dev/null; then
            echo -e "${GREEN}✓ 成功添加仓库: $repo_url${NC}"
            REPO_ADDED=true
            break
        else
            echo -e "${YELLOW}添加失败，尝试下一个镜像源...${NC}"
        fi
    done
    
    if [ "$REPO_ADDED" = false ]; then
        echo -e "${RED}错误: 无法添加任何 Docker 仓库${NC}" >&2
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker 仓库添加完成${NC}"
    
    # 步骤3: 安装 Docker Engine
    echo -e "${YELLOW}[4/6] 安装 Docker Engine...${NC}"
    yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    echo -e "${GREEN}✓ Docker Engine 安装完成${NC}"
else
    echo -e "${YELLOW}[2-4/6] 跳过 Docker 安装步骤${NC}"
fi

# 步骤4: 安装 Docker Compose（独立版本，兼容 docker-compose 命令）
echo -e "${YELLOW}[5/6] 安装 Docker Compose...${NC}"

# 优先检查 docker compose（插件版本）是否可用
DOCKER_COMPOSE_PLUGIN_AVAILABLE=false
if docker compose version &> /dev/null; then
    COMPOSE_PLUGIN_VERSION=$(docker compose version 2>/dev/null | head -n1)
    echo -e "${GREEN}检测到 Docker Compose 插件版本: $COMPOSE_PLUGIN_VERSION${NC}"
    DOCKER_COMPOSE_PLUGIN_AVAILABLE=true
fi

# 检查是否已安装独立的 docker-compose 命令
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version 2>/dev/null | head -n1)
    echo -e "${GREEN}检测到已安装独立的 Docker Compose: $COMPOSE_VERSION${NC}"
    SKIP_COMPOSE=true
elif [ "$DOCKER_COMPOSE_PLUGIN_AVAILABLE" = true ]; then
    # 如果 docker compose 插件可用，创建兼容的 docker-compose 命令
    echo -e "${BLUE}创建 docker-compose 兼容命令（基于 docker compose 插件）...${NC}"
    
    # 创建包装脚本
    cat > /usr/local/bin/docker-compose <<'EOF'
#!/bin/bash
# Docker Compose 兼容包装脚本
# 将 docker-compose 命令转换为 docker compose 插件命令
exec docker compose "$@"
EOF
    
    chmod +x /usr/local/bin/docker-compose
    
    # 创建软链接（如果不存在）
    if [ ! -f /usr/bin/docker-compose ]; then
        ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
    fi
    
    echo -e "${GREEN}✓ docker-compose 兼容命令创建完成${NC}"
    SKIP_COMPOSE=true
else
    SKIP_COMPOSE=false
fi

if [ "$SKIP_COMPOSE" = false ]; then
    # 使用固定版本，避免访问 GitHub API（国内访问较慢）
    COMPOSE_VERSION="v2.24.5"
    echo -e "${BLUE}将安装 Docker Compose 版本: $COMPOSE_VERSION${NC}"
    
    # 获取系统信息
    OS_TYPE=$(uname -s)
    ARCH_TYPE=$(uname -m)
    COMPOSE_BINARY="docker-compose-${OS_TYPE}-${ARCH_TYPE}"
    
    # 下载源列表（优先使用国内镜像）
    DOWNLOAD_URLS=(
        "https://mirror.ghproxy.com/https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/${COMPOSE_BINARY}"
        "https://ghproxy.com/https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/${COMPOSE_BINARY}"
        "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/${COMPOSE_BINARY}"
    )
    
    # 尝试从多个源下载
    DOWNLOAD_SUCCESS=false
    for url in "${DOWNLOAD_URLS[@]}"; do
        echo -e "${BLUE}尝试从镜像源下载: $(echo $url | sed 's|https://||' | cut -d'/' -f1)${NC}"
        if curl -L --connect-timeout 10 --max-time 60 -f "$url" -o /usr/local/bin/docker-compose 2>/dev/null; then
            echo -e "${GREEN}✓ 下载成功${NC}"
            DOWNLOAD_SUCCESS=true
            break
        else
            echo -e "${YELLOW}下载失败，尝试下一个镜像源...${NC}"
        fi
    done
    
    if [ "$DOWNLOAD_SUCCESS" = false ]; then
        echo -e "${RED}错误: 无法从任何镜像源下载 Docker Compose${NC}" >&2
        echo -e "${YELLOW}提示: 请检查网络连接或手动下载安装${NC}" >&2
        exit 1
    fi
    
    chmod +x /usr/local/bin/docker-compose
    
    # 创建软链接（如果不存在）
    if [ ! -f /usr/bin/docker-compose ]; then
        ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
    fi
    
    echo -e "${GREEN}✓ Docker Compose 安装完成${NC}"
else
    echo -e "${YELLOW}[5/6] 跳过 Docker Compose 安装步骤${NC}"
fi

# 步骤5: 启动并配置 Docker 服务
echo -e "${YELLOW}[6/6] 启动并配置 Docker 服务...${NC}"

# 启动 Docker 服务
systemctl start docker

# 设置 Docker 开机自启
systemctl enable docker

# 配置 Docker 镜像加速（使用多个国内镜像源）
echo -e "${BLUE}配置 Docker 镜像加速...${NC}"
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com",
    "https://registry.cn-hangzhou.aliyuncs.com",
    "https://dockerhub.azk8s.cn",
    "https://reg-mirror.qiniu.com"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "max-concurrent-downloads": 10,
  "max-concurrent-uploads": 5
}
EOF

# 重启 Docker 使配置生效
systemctl daemon-reload
systemctl restart docker

echo -e "${GREEN}✓ Docker 服务配置完成${NC}"

# 验证安装
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}验证安装结果...${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查 Docker 版本
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    echo -e "${GREEN}✓ Docker: $DOCKER_VERSION${NC}"
else
    echo -e "${RED}✗ Docker 未正确安装${NC}"
    exit 1
fi

# 检查 Docker Compose 版本
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version 2>/dev/null | head -n1)
    echo -e "${GREEN}✓ Docker Compose: $COMPOSE_VERSION${NC}"
elif docker compose version &> /dev/null; then
    # 如果 docker-compose 命令不存在，但 docker compose 可用，创建兼容命令
    echo -e "${YELLOW}检测到 docker compose 插件，创建兼容命令...${NC}"
    cat > /usr/local/bin/docker-compose <<'EOF'
#!/bin/bash
exec docker compose "$@"
EOF
    chmod +x /usr/local/bin/docker-compose
    if [ ! -f /usr/bin/docker-compose ]; then
        ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
    fi
    COMPOSE_VERSION=$(docker-compose --version 2>/dev/null | head -n1)
    echo -e "${GREEN}✓ Docker Compose: $COMPOSE_VERSION (通过 docker compose 插件)${NC}"
else
    echo -e "${RED}✗ Docker Compose 未正确安装${NC}"
    exit 1
fi

# 检查 Docker 服务状态
if systemctl is-active --quiet docker; then
    echo -e "${GREEN}✓ Docker 服务运行正常${NC}"
else
    echo -e "${RED}✗ Docker 服务未运行${NC}"
    exit 1
fi

# 测试 Docker 是否正常工作
echo -e "${BLUE}测试 Docker 功能...${NC}"
if docker run --rm hello-world &> /dev/null; then
    echo -e "${GREEN}✓ Docker 功能测试通过${NC}"
else
    echo -e "${YELLOW}⚠ Docker 功能测试失败，但可能不影响使用${NC}"
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}安装完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${BLUE}常用命令：${NC}"
echo -e "  启动 Docker: ${YELLOW}systemctl start docker${NC}"
echo -e "  停止 Docker: ${YELLOW}systemctl stop docker${NC}"
echo -e "  查看状态: ${YELLOW}systemctl status docker${NC}"
echo -e "  查看版本: ${YELLOW}docker --version${NC}"
echo -e "  查看 Compose 版本: ${YELLOW}docker-compose --version${NC}"
echo -e "${GREEN}========================================${NC}"

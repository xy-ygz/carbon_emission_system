# 中文字体配置说明

## 推荐字体

### 1. 思源黑体（Source Han Sans）- 推荐 ⭐
- **下载地址**：https://github.com/adobe-fonts/source-han-sans/releases
- **特点**：开源免费，支持简体中文、繁体中文、日文、韩文
- **推荐文件**：
  - `SourceHanSans-Regular.ttf`（常规）
  - `SourceHanSans-Bold.ttf`（粗体）
- **下载步骤**：
  1. 访问 https://github.com/adobe-fonts/source-han-sans/releases
  2. 下载最新版本的 `SubsetOTF/CN/`（简体中文）或 `OTF/`（完整版）
  3. 解压后找到 `SourceHanSans-Regular.otf` 或 `SourceHanSans-Bold.otf`
  4. 重命名为 `SourceHanSans-Regular.ttf`（PDFBox 支持 .otf，但建议统一命名）
  5. 放到本目录（`src/main/resources/fonts/`）

### 2. Noto Sans CJK
- **下载地址**：https://www.google.com/get/noto/#sans-hans
- **特点**：Google 开源字体，覆盖完整
- **推荐文件**：`NotoSansCJK-Regular.ttf`

### 3. 思源宋体（Source Han Serif）
- **下载地址**：https://github.com/adobe-fonts/source-han-serif/releases
- **特点**：宋体风格，适合正式文档
- **推荐文件**：`SourceHanSerif-Regular.ttf`

## 放置位置

下载字体后，请将字体文件放到：
```
src/main/resources/fonts/
```

例如：
- `src/main/resources/fonts/SourceHanSans-Regular.ttf`
- `src/main/resources/fonts/SourceHanSans-Bold.ttf`

## 字体文件命名

代码会按以下顺序查找字体：
1. `SourceHanSans-Regular.ttf`（思源黑体 Regular）
2. `SourceHanSans-Bold.ttf`（思源黑体 Bold）
3. `SourceHanSerif-Regular.ttf`（思源宋体 Regular）
4. `NotoSansCJK-Regular.ttf`（Noto Sans CJK）
5. `SimSun.ttf`（宋体）
6. `SimHei.ttf`（黑体）

## 快速开始

1. **下载思源黑体**：
   - 访问：https://github.com/adobe-fonts/source-han-sans/releases
   - 下载 `SubsetOTF/CN/SourceHanSansCN-Regular.otf`
   - 重命名为 `SourceHanSans-Regular.ttf`
   - 放到 `src/main/resources/fonts/` 目录

2. **重新编译项目**：
   ```bash
   mvn clean compile
   ```

3. **测试PDF导出**：
   - 访问：http://localhost:8080/api/carbonEmission/exportReport?format=pdf

## 注意事项

- 字体文件较大（通常 10-20MB），建议使用 Subset 版本（只包含简体中文）
- 确保字体文件有读取权限
- 如果使用 .otf 格式，代码也能正常加载

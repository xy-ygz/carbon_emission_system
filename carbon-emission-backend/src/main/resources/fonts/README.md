# 中文字体配置说明

## 关于 CO₂、平方²、立方³ 不显示的问题

导出 Word/PDF 中 **CO₂**、**²**、**³** 不显示，是因为 **SimSun/宋体 不包含 Unicode 下标 ₂ (U+2082)** 等字符。  
请优先使用下面「含上下标的字体」之一，图表与 PDF 会优先加载这些字体，从而正确显示 ² ³ ₂ 等。

---

## 推荐字体（含 ² ³ ₂，优先使用）

### 1. Noto Sans CJK SC（推荐，解决 CO₂ 不显示）⭐
- **下载地址**：https://github.com/notofonts/noto-cjk/releases
- **特点**：开源免费，含中文 + 上标²³、下标₂₃ 等，可正确显示 CO₂、kgCO₂/kWh、Nm³
- **推荐文件**：`NotoSansCJKsc-Regular.otf` 或 `NotoSansCJK-Regular.otf`（简体）
- **放置**：将 `.otf` 或 `.ttf` 放到本目录 `src/main/resources/fonts/`

### 2. 思源黑体（Source Han Sans）
- **下载地址**：https://github.com/adobe-fonts/source-han-sans/releases
- **特点**：开源免费，支持简体中文，含上下标字符，可正确显示 CO₂
- **推荐文件**：
  - `SourceHanSansSC-Regular.otf` 或 `SourceHanSans-Regular.ttf`
  - `SourceHanSans-Bold.ttf`（粗体可选）
- **下载步骤**：
  1. 访问 https://github.com/adobe-fonts/source-han-sans/releases
  2. 下载 `SubsetOTF/CN/`（简体）或 `OTF/`（完整版）
  3. 解压后把 `SourceHanSansSC-Regular.otf` 或 `SourceHanSans-Regular.otf` 放到本目录
  4. 无需改名为 .ttf，代码支持 .otf

### 3. 思源宋体（Source Han Serif）
- **下载地址**：https://github.com/adobe-fonts/source-han-serif/releases
- **特点**：宋体风格，适合正式文档；也含上下标
- **推荐文件**：`SourceHanSerif-Regular.ttf`

## 放置位置

下载字体后，请将字体文件放到：
```
src/main/resources/fonts/
```

例如：
- `src/main/resources/fonts/SourceHanSans-Regular.ttf`
- `src/main/resources/fonts/SourceHanSans-Bold.ttf`

## 字体加载优先级（图表与 PDF）

图表和导出会**按以下顺序**查找字体（先找到先使用）：
1. `NotoSansCJKsc-Regular.otf` / `NotoSansCJK-Regular.ttf`（**静态**，图表中 CO₂ 显示最稳）
2. `SourceHanSerifSC-VF.ttf` / `SourceHanSansSC-VF.ttf`（可变字体 VF）
3. `SourceHanSansSC-Regular.otf` / `SourceHanSans-Regular.otf`（思源黑体）
4. `SimSun.ttf`（宋体，不含下标 ₂）

**若图 3 图例「CO₂排放量」仍缺下标 ₂**：Java 对可变字体(VF)的下标字形支持有限，建议**额外**下载 **Noto Sans CJK SC 静态版**（`NotoSansCJKsc-Regular.otf`）放到本目录，代码会优先使用，图表中 ₂ 即可正常显示。

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
- 确保字体文件放在 **`src/main/resources/fonts/`** 并**重新打包**，否则以 jar 方式启动时找不到字体
- 表 1 单位（如 kgCO₂/Nm³）已不再被替换，只要使用含 ² ³ ₂ 的字体即可正常显示
- 若使用可变字体(VF)时图例仍缺 ₂，请增加一个**静态**字体（如 NotoSansCJKsc-Regular.otf）并放在本目录

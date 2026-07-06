# Workpaper Android 开发日志

## 目标功能
实现类似 iPhone 的视差/3D 动态壁纸效果：
- 手机倾斜时，不同深度的物体有不同的位移幅度（近大远小）
- 图像不被扭曲，分层刚性位移
- 正确的宽高比适配，无拉伸、无黑边

---

## 深度模型对比（2026-07-06 实测结论）

### MiDaS v2.1 Small (TFLite)
- **模型大小**: 66.3MB，单文件
- **输入**: 256×256，ImageNet 归一化
- **输出**: 逆深度（Inverse Depth），对比度天然较高
- **优点**: 开箱即用效果好，前景/背景分离明显
- **缺点**: 边缘模糊（Over-smoothing），物体边缘锯齿严重，背景会侵占主体

### Depth Anything V2 (ONNX)
- **模型大小**: 180KB + 50MB 数据文件
- **输入**: 518×518，[0,1] 归一化
- **输出**: 相对深度（Relative Depth），值域集中且线性
- **优点**: 边缘极其锐利（Sharp boundaries），细小物体分辨好
- **缺点**: 原始输出对比度低，看起来像"平移"，需要后处理

### 后处理管线（DAV2 专用）
```
原始深度 → 百分位截断(1%-99%) → 归一化 → 反转 → Sigmoid对比度(contrast=6) → 双边滤波(radius=7)
```
- 百分位截断：去除极端值，强制拉开中景层次
- Sigmoid 对比度：`1/(1+exp(-6*(x-0.5)))`，制造前中后景"深度断层"
- 双边滤波：保持锐利边缘，平滑表面噪点

---

## 技术架构

### 渲染器
- `GLDepthLayerRenderer`: 3层刚性图层 + 顶点空间视差
  - 背景层(0): alpha=1.0，无混合，shift=0.15
  - 中景层(1): 自适应阈值，shift=0.55
  - 前景层(2): 自适应阈值，shift=1.0
  - 宽高比校正: `offsetY = -tiltY * shift * aspect`
  - 着色器: `gl_Position = vec4(vPosition.xy * uScale + uParallaxOffset, 0.0, 1.0)`

### 深度估算
- `DepthEstimator` 接口 + 工厂模式
- `MiDaSDepthEstimator`: TFLite，256×256 输入
- `DAV2DepthEstimator`: ONNX Runtime，518×518 输入 + 后处理管线

---

## 已解决的问题

| 问题 | 根因 | 修复 |
|------|------|------|
| 预览黑屏 | 深度处理期间渲染器切换到空的 depthLayerRenderer | 先显示原图，深度处理完成后再切换 |
| 黑色区域 | 启用混合时半透明像素与黑色帧缓冲混合 | 背景层禁用 GL_BLEND |
| SIGSEGV 崩溃 | 5层×1478×3204 纹理超出 GPU 驱动承受 | 图片缩放到 1080px |
| 线程竞争崩溃 | vertexBuffer 从服务线程修改 | pending 机制，所有状态变更在 GL 线程执行 |
| 位图回收崩溃 | newBitmap 被并发回收 | 深度处理前创建防御性副本 |

---

## 当前未解决问题（2026-07-06）

### 问题 H: MiDaS — 边缘锯齿 + 背景侵占主体
- **现象**: 主体与背景间割裂感、锯齿严重，背景会侵占主体一小部分
- **原因**: MiDaS v2.1 的深度边界不够锐利（Over-smoothing），分层阈值与实际物体边缘不对齐
- **尝试过的修复**: 增大羽化、百分位自适应阈值 — 未完全解决

### 问题 I: DAV2 — 效果仍接近平移 + 图层间割裂
- **现象**: 即使经过 Sigmoid 对比度增强，视差效果仍像整体平移，且图层间有半透明黑色粗线条
- **原因分析**:
  1. Sigmoid contrast=6 可能仍不够强，或太强导致中景层内容过少
  2. 羽化区域的 alpha 过渡产生暗色混合线条
  3. 3 层分割方案本身有局限 — 层间过渡不够自然
- **待验证**: 可视化 DAV2 后处理深度图，确认分布是否合理

### 问题 J: 图层间半透明黑色粗线条
- **现象**: 晃动手机时，图层分界处出现约 0.5cm 宽的半透明黑色不规则线条
- **原因**: 羽化(0.15)导致的 alpha 过渡区域，在混合时产生暗色
- **可能修复方向**:
  1. 去掉羽化，改用硬边界 + 更多层
  2. 或在片元着色器中做连续深度偏移（避免分层）

---

## 关键文件
- `wallpaper/GLDepthLayerRenderer.kt` — 深度图层渲染器（3层 + 顶点空间视差）
- `wallpaper/GLImageWallpaperRenderer.kt` — 简单视差渲染器
- `wallpaper/WallpaperRenderer.kt` — 渲染管理器
- `service/LiveWallpaperService.kt` — 动态壁纸服务
- `depth/DepthEstimator.kt` — 接口
- `depth/MiDaSDepthEstimator.kt` — MiDaS v2.1 TFLite 实现
- `depth/DAV2DepthEstimator.kt` — DAV2 ONNX 实现 + 后处理

---

## 下一步方向

### 短期（验证 DAV2 后处理效果）
1. 可视化 DAV2 后处理深度图（已添加 saveVisualization）
2. 如果分布仍不理想，调整 Sigmoid contrast 和百分位截断参数
3. 考虑用 MiDaS 作为默认（开箱即用效果更好），DAV2 作为高级选项

### 中期（根本性改进）
1. **增加层数到 5-7 层** + 更精细的阈值分布，减少层间割裂
2. **片元着色器方案**：不用 CPU 分图层，在 shader 中采样 depth texture 做连续偏移
3. **双边滤波参数调优**：radius 和 sigma 需要针对具体图片调试

### 长期（模型升级）
1. MiDaS v3.1 (dpt_swin2_tiny_256) — Transformer 架构，边缘更锐利
2. 深度图分辨率提升 — 518×518 太低，考虑更高分辨率输入
3. 深度图后处理流水线优化 — 可能需要针对不同图片类型自适应参数

# GitHub 绿墙小组件 · GitHub Contributions Widget

一个**极致轻量**的 Android 原生桌面小组件（AppWidget），把 GitHub 贡献热力图（绿墙）常驻到你的桌面。

- 📦 **release 包仅 ~300KB**（远小于常见的 2~3MB）
- 🔋 零常驻后台，仅每 6 小时定时刷新
- 🎨 Canvas 原生绘制圆角方块，浅色 / 深色模式自动切换
- 📶 免登录抓取 GitHub 公开贡献页数据，无需任何 token
- 👆 点击小组件直接打开 GitHub 个人主页
- 📱 支持 Android 10 及以上（minSdk 29）

## 能做什么

- 桌面显示 52 周 × 7 格的贡献热力图
- 每 6 小时自动刷新；拖动缩放小组件时按新尺寸自动重绘
- 无网时显示上次缓存画面，不闪白屏
- 主题色板可自定义（改 `ContributionRenderer.kt` 里的 `LIGHT` / `DARK` 数组）

## 安装方式

### 方式一：adb 安装（本机已连接手机）

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### 方式二：传 APK 手动安装

把 `app-release.apk` 发到手机（微信 / 网盘 / 数据线均可）→ 点开 → 允许「安装未知应用」→ 安装。

### 方式三：Android Studio

打开项目 → 手机开启「USB 调试」→ 点 Run 直装。

> 装完后：**长按桌面空白 → 小部件 → 拖入「GitHub 绿墙」**。桌面图标点开是安装说明页。

## 配置你的 GitHub 用户名

改一处即可：`app/src/main/res/values/strings.xml`

```xml
<string name="github_username" translatable="false">你的用户名</string>
```

改完重新构建打包：`./gradlew assembleRelease`。

## 自己构建

```bash
# 依赖：JDK 17 + Android SDK (platforms;android-34)
./gradlew assembleDebug     # 调试包 → app/build/outputs/apk/debug/
./gradlew assembleRelease   # 发布包 → app/build/outputs/apk/release/（已签名）
```

## 架构

```
app/src/main/java/com/cat/ghwidget/
├── GithubGraphFetcher.kt    # OkHttp 免登录抓取 + 正则解析 data-date/data-level
├── ContributionRenderer.kt  # Canvas 圆角方块绘制，浅/深双主题色板
├── WidgetStore.kt           # 数据存 SharedPreferences，最近渲染图存缓存
├── GithubWidgetProvider.kt  # 组件入口：缓存秒显 → 后台刷新 → 主线程更新
└── MainActivity.kt          # 安装说明页
```

唯一第三方依赖：`com.squareup.okhttp3:okhttp:4.12.0`。

---

English version: [README_EN.md](README_EN.md)

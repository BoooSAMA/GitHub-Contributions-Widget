# GitHub 绿墙小组件 · GitHub Contributions Widget

一个**极致轻量**的 Android 原生桌面小组件（AppWidget），把 GitHub 贡献热力图（绿墙）常驻到你的桌面。

- 📦 **release 包仅 ~300KB**（远小于常见的 2~3MB）
- 🔋 零常驻后台，仅每 6 小时定时刷新
- 🎨 Canvas 原生绘制圆角方块，浅色 / 深色模式自动切换
- 📶 免登录抓取 GitHub 公开贡献页数据，无需任何 token
- 👆 点击小组件直接打开 GitHub 个人主页
- 📱 支持 Android 10 及以上（minSdk 29）

## 能做什么

- 桌面显示 52 周 × 7 格的贡献热力图（或按时间跨度档位显示最近半年 / 三个月）
- 每 6 小时自动刷新；拖动缩放小组件时按新尺寸自动重绘
- 无网时显示上次缓存画面，不闪白屏
- 主题色板可自定义（改 `ContributionRenderer.kt` 里的 `LIGHT` / `DARK` 数组）

## 外观客制化（全部在 app 内完成，即时预览）

| 设置项 | 范围 | 说明 |
|--------|------|------|
| 主题模式 | 跟随系统 / 浅色 / 深色 | app 与小组件色板同步 |
| 时间跨度 | 自动适配 / 全年 / 半年 / 近三月 | 自动档按窗口宽高比取最近 N 周铺满 |
| 卡片圆角 | 0% ~ 30% | 0 = 直角 |
| 空格子描边 | 0% ~ 20% | 0 = 不描边 |
| 卡牌底色 | 跟随主题 / 纯透明 / 纯白 / 浅灰 / 纯黑 / 炭黑 | |
| 底色透明度 | 0% ~ 100% | 半透明卡牌透出壁纸 |
| 卡牌边距 | 0% ~ 15% | 0% = 卡牌铺满整个小组件区域 |
| 空格子对比度 | 0% ~ 100% | 提升空格子与低贡献格子（1~3 次）的可辨度 |
| 毛玻璃底框 | 显示 / 隐藏 | |
| 月份文字颜色 | 跟随主题 / 中灰 / 石板灰 / 白 / 黑 | |
| 月份字号 | 60% ~ 200% | 相对格子尺寸 |

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

装好后直接在 app 里配置，**无需改代码重新打包**：

打开「GitHub 绿墙」→ 输入你的用户名 → 点「预览并应用」→ 绿墙立即生成并刷新小组件。

> 默认用户名仍可改代码预设：`app/src/main/res/values/strings.xml` → `github_username`

## 自己构建

```bash
# 依赖：JDK 17 + Android SDK (platforms;android-34)
./gradlew assembleDebug     # 调试包 → app/build/outputs/apk/debug/
./gradlew assembleRelease   # 发布包 → app/build/outputs/apk/release/（已签名）
```

## 文件架构

```
GitHub-Contributions-Widget/
├── app/
│   ├── build.gradle.kts              # 构建配置：minSdk 29 / targetSdk 34、版本号、签名、依赖
│   ├── proguard-rules.pro            # R8 混淆规则
│   ├── signing/release.jks           # release 签名密钥（不入库）
│   └── src/main/
│       ├── AndroidManifest.xml       # 组件注册：MainActivity + AppWidgetProvider
│       ├── java/com/cat/ghwidget/
│       │   ├── GithubGraphFetcher.kt    # 抓取 github.com/users/<name>/contributions，
│       │   │                            #   正则兼容 SVG <rect> 与 HTML <td> 两种页面结构
│       │   ├── ContributionRenderer.kt  # ★ 小组件主体视觉：Canvas 绘制格子 / 圆角 / 描边 /
│       │   │                            #   月份标签 / 卡牌底 / 对比度，全部由 RenderConfig 驱动
│       │   ├── WidgetStore.kt           # SharedPreferences 持久化：格子数据、用户名、
│       │   │                            #   全部客制化设置、最近渲染图缓存
│       │   ├── GithubWidgetProvider.kt  # 小组件生命周期：缓存秒显 → 后台刷新 → 尺寸变化重绘
│       │   └── MainActivity.kt          # ★ app 界面逻辑：用户名配置、预览、
│       │                                #   主题/跨度/滑条/pill 等全部设置项
│       └── res/
│           ├── layout/activity_main.xml     # ★ app 界面布局（卡片、输入框、设置项）
│           ├── layout/widget_github.xml     # ★ 小组件容器布局（ImageView + 毛玻璃底框）
│           ├── values/colors.xml            # ★ 浅色主题语义色 token
│           ├── values-night/colors.xml      # ★ 深色主题语义色 token
│           ├── values/strings.xml           # ★ 全部界面文案与默认用户名
│           ├── drawable/
│           │   ├── widget_background.xml    # ★ 小组件毛玻璃底框
│           │   ├── bg_card.xml              # app 内卡片背景
│           │   ├── bg_input.xml             # 输入框背景
│           │   ├── bg_button_primary.xml    # 主按钮背景
│           │   ├── pill_bg.xml / pill_bg_active.xml  # 选项 pill 样式（普通 / 选中）
│           │   ├── ic_launcher.xml          # 桌面图标（108dp 对角渐变矢量）
│           │   ├── ic_launcher_bg.xml       # 自适应图标背景层
│           │   └── ic_launcher_fg.xml       # 自适应图标前景层
│           ├── mipmap-anydpi-v26/ic_launcher.xml  # 自适应图标声明
│           └── xml/github_widget_info.xml   # 小组件元信息：默认尺寸、6 小时刷新、可缩放
├── README.md / README_EN.md
└── agent.md                          # 项目定位与制作意向（未来 agent 行为约束）
```

### 想改 UI？看这张表

| 想改什么 | 改哪个文件 |
|----------|-----------|
| 小组件里绿墙本身的样子（格子、间距、圆角、描边、月份标签、配色算法） | `ContributionRenderer.kt` |
| 小组件的外框形态（毛玻璃、内边距、整体布局） | `res/layout/widget_github.xml` + `res/drawable/widget_background.xml` |
| 小组件尺寸、刷新周期、能否缩放 | `res/xml/github_widget_info.xml` |
| app 界面的控件排布 / 增加设置项 | `res/layout/activity_main.xml` + `MainActivity.kt` |
| 深浅两套配色 | `res/values/colors.xml` / `res/values-night/colors.xml` |
| 所有文案、默认用户名 | `res/values/strings.xml` |
| 按钮 / 输入框 / pill 的样式 | `res/drawable/bg_*.xml`、`pill_bg*.xml` |
| app 图标 | `res/drawable/ic_launcher*.xml` |
| 版本号 | `app/build.gradle.kts` → `versionCode` / `versionName` |

### 数据流

```
GitHub 公开页面 ──► GithubGraphFetcher（抓取 + 解析）
                          │
                          ▼
                   WidgetStore（SharedPreferences：格子 + 设置）
                          │
        ┌─────────────────┴──────────────────┐
        ▼                                    ▼
ContributionRenderer（按 RenderConfig 渲染）   MainActivity（预览 + 修改设置）
        │                                    │
        ▼                                    ▼
GithubWidgetProvider ──RemoteViews──► 桌面小组件（ImageView 显示 Bitmap）
```

唯一第三方依赖：`com.squareup.okhttp3:okhttp:4.12.0`。

---

English version: [README_EN.md](README_EN.md)

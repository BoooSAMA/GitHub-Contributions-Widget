# agent.md — 项目 Agent 指令

## 项目定位

**GitHub 贡献绿墙 Android 桌面小组件**（AppWidget）。
在 Android 桌面常驻一块 GitHub contributions 热力图（绿墙），定时自动刷新，点击跳转 GitHub 主页。

## 制作意向（核心原则，改代码前必读）

1. **极致轻量**：release 包必须保持在 1MB 以内（当前约 300KB）。禁止引入新的第三方依赖（唯一允许的网络库是 okhttp）。
2. **原生自绘**：传统 `AppWidgetProvider` + `RemoteViews` + Canvas 绘制。**不使用** Jetpack Glance / Compose，避免体积和复杂度。
3. **免登录取数**：公开抓取 `https://github.com/users/<name>/contributions`，正则解析 `<rect>` 的 `data-date` / `data-level`。不引入 Jsoup，不使用 GraphQL API。
4. **无常驻后台**：仅靠 `updatePeriodMillis`（6 小时）刷新，不使用前台服务，不申请除 INTERNET 外的任何权限。
5. **体验底线**：无网时必须显示上次缓存的图，不闪白屏；新增实例先渲染缓存再后台拉新。
6. **适配范围**：minSdk 29（Android 10+），targetSdk 34。深浅双主题跟随系统。

## 能做什么（当前功能）

- 桌面显示 52 周 × 7 格的贡献热力图，Canvas 圆角方块绘制
- 每 6 小时自动刷新；拖动缩放小组件时按新尺寸重绘
- 浅色/深色模式自动切换（色板在 `ContributionRenderer.kt`，卡片底色在 `res/values-night/`）
- 点击小组件打开用户 GitHub 主页（PendingIntent）
- 最近渲染图本地缓存，离线兜底显示
- 安装成功提示页 `MainActivity`（无业务逻辑）

## 关键文件

| 文件 | 职责 |
|---|---|
| `GithubGraphFetcher.kt` | 网络抓取 + 解析（`Cell(date, level)`） |
| `ContributionRenderer.kt` | Canvas 绘制，浅/深色板 |
| `WidgetStore.kt` | prefs 存数据、cacheDir 存最近渲染图 |
| `GithubWidgetProvider.kt` | 组件入口：先缓存秒显 → 后台刷新 → 回主线程更新 |
| `res/values/strings.xml` | **`github_username` 在这里改**（唯一必改项） |

## 构建与安装

```bash
# 构建（本机已配 local.properties 指向 SDK）
gradle assembleDebug      # 调试包
gradle assembleRelease    # 发布包（已配置签名 app/signing/release.jks，密码见 app/build.gradle.kts）

# 安装
adb install app/build/outputs/apk/release/app-release.apk
```

用户侧手动安装：把 `app-release.apk` 传到手机 → 点开 → 允许「安装未知应用」→ 装完长按桌面空白 → 小部件 → 拖入「GitHub 绿墙」。

## Agent 行为约束

- 不要新建 Activity / 界面，不要把项目变成多页应用
- 不要动 `local.properties`（含 SDK 路径）、`app/signing/`（签名密钥）
- 不要提交 `.omo/`、`.codegraph` 等 AI 工具目录（已在 .gitignore）
- 修改后用 `gradle assembleRelease` 验证构建通过、包体 < 1MB 再交付

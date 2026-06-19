# 军棋 · 翻棋版

Android 原生（Kotlin）单机军棋游戏。支持人机对战（5档难度）和本地双人对战。

---

## 给家人的安装方法（最简单）

1. 打开本仓库右侧的 **Releases** 页面
2. 下载最新版本里的 `app-debug.apk`
3. 手机上点开文件，提示"未知来源"时选择允许安装
4. 安装完成，桌面会出现"军棋"图标，直接打开玩

---

## 玩法说明

**翻棋模式**：所有棋子背面朝下随机摆在棋盘上，点击任意一颗就翻开。翻开后，如果旁边有已翻开的敌方棋子，立刻判定胜负。最终目标是吃掉对方的军旗。

**棋子等级（从强到弱）**

司令 > 军长 > 师长 > 旅长 > 团长 > 营长 > 连长 > 排长 > 工兵

**特殊规则**

- 炸弹：碰到任何棋子，双方一起消失
- 地雷：能炸死所有进攻它的棋子，唯独工兵能把它排掉
- 铁路线（图中虚线）：工兵可以沿铁路走很远，其他棋子只能走一格

---

## 给开发者：如何自己打包 APK

### 方法一：用GitHub自动打包（推荐，不需要装任何软件）

1. 把这个项目推送到你自己的GitHub仓库
2. 仓库会自动运行 Actions，几分钟后在 **Actions** 标签页能看到构建进度
3. 构建成功后，去 **Releases** 页面下载新生成的 APK

每次你修改代码并推送到 `main` 分支，都会自动重新打包一个新版本。

### 方法二：本地用Android Studio打包

1. 下载安装 [Android Studio](https://developer.android.com/studio)（免费）
2. 打开本项目文件夹
3. 等待右下角的Gradle同步完成（第一次会比较慢）
4. 点击菜单 `Build → Build Bundle(s) / APK(s) → Build APK(s)`
5. 构建完成后点击弹出提示里的 `locate`，找到 APK 文件
6. APK 路径一般在 `app/build/outputs/apk/debug/app-debug.apk`

### 用手机直接安装、不打包

也可以用USB连接手机，打开开发者模式和USB调试，在Android Studio里直接点绿色三角运行按钮，代码会自动装到手机上。

---

## 项目结构

```
app/src/main/
├── java/com/junqi/game/
│   ├── core/
│   │   ├── GameConst.kt      所有常量、棋子数据定义
│   │   ├── RuleEngine.kt     规则判定（战斗、移动、胜负）
│   │   └── BoardState.kt     棋盘状态管理
│   ├── ai/
│   │   └── AIController.kt   AI逻辑，5档难度
│   └── ui/
│       ├── BoardView.kt      棋盘绘制（Canvas）
│       ├── MenuActivity.kt   主菜单
│       └── GameActivity.kt   游戏主界面
└── res/
    ├── layout/                界面布局文件
    ├── drawable/               按钮、背景图形
    └── values/                 颜色、字符串、主题
```

---

## License

MIT License，自由使用、修改、分发。

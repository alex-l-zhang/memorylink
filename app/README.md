# 忆联 C 端 App（Flutter）

家属端移动应用：登录/注册 → 记忆档案列表/创建 → 故事问答。

## 运行

Flutter SDK 已安装（/home/dev/flutter，stable 3.47.2）。

```bash
export PATH=/home/dev/flutter/bin:$PATH
cd app
flutter pub get
flutter run
```

默认连接后端 `http://192.168.32.128:8080`；Android 模拟器访问宿主机请覆盖：

```bash
flutter run --dart-define=API_BASE=http://10.0.2.2:8080
```

## 测试

```bash
flutter analyze
flutter test
```

## 结构

```text
lib/
├── main.dart                 # 入口，可配置 API 地址
├── models.dart               # AuthResult / LovedOne / ChatResult
├── api/api_client.dart       # 登录/注册/档案/问答 HTTP 客户端
└── screens/
    ├── login_screen.dart     # 登录/注册
    ├── home_screen.dart      # 记忆档案列表/创建
    └── chat_screen.dart      # 故事问答（含 AI 标识）
```

生成平台：android、ios（如需桌面/Web 预览可再执行 `flutter create --platforms=linux,windows,macos,web .`）。

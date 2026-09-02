# 忆联 C 端 App（Flutter）

本目录是 C 端移动 App 的占位工程。当前环境未安装 Flutter SDK，首次初始化请执行：

```bash
# 1. 安装 Flutter SDK：https://docs.flutter.dev/get-started/install
# 2. 在本目录初始化工程
cd app
flutter create --project-name memorylink_app --org com.memorylink .
```

初始化完成后按需添加依赖（图片/音频上传、推送、http 等），并在 `lib/` 下按模块组织：

```text
lib/
├── main.dart
├── core/        # 网络、配置、统一响应
├── features/
│   ├── auth/    # 登录注册
│   ├── home/    # 首页/节日关怀
│   ├── archive/ # 记忆档案/记忆馆
│   ├── family/  # 家族树
│   └── chat/    # 故事问答
└── widgets/     # 公共组件
```

接口文档见服务端 `/api/v1`（启动后访问 http://localhost:8080/swagger-ui.html）。

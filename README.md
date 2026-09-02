# 忆联（MemoryLink）

帮助家庭保存和传承亲人记忆的家族记忆传承平台。MVP 包含 C 端 App（Flutter）、B 端机构后台（Web）、平台后台管理（Web）与服务端（Java/Spring Boot）。

## 目录结构

```text
MemoryLink/
├── server/    # Java 17+ / Spring Boot 3 后端 API 服务
├── app/       # C 端移动 App（Flutter，待初始化）
├── web/       # B 端机构后台（React + TypeScript + Ant Design）
├── admin/     # 平台后台管理（React + TypeScript + Ant Design）
├── docs/      # 产品、市场、项目、架构文档
└── .github/   # CI/CD
```

## 快速开始

### 1. 本地基础服务

PostgreSQL 使用本机已安装实例（端口 5432，库 memorylink，schema memorylink，用户 memorylink/memorylink），无需启动容器。

```bash
docker compose -f docker-compose.dev.yml up -d
```

启动 Redis、MinIO（S3 兼容对象存储）。

### 2. 服务端

```bash
cd server
set -a && source ../.env && set +a   # 加载 .env（含 DeepSeek API Key 等）
mvn spring-boot:run
```

接口文档（springdoc）：启动后访问 http://localhost:8080/swagger-ui.html

健康检查：http://localhost:8080/actuator/health

首次使用请先配置 API Key：

```bash
# 复制示例并填入真实 Key（.env 已被 .gitignore 忽略，不会提交）
cp .env.example .env
# 编辑 .env，修改这一行：
#   DEEPSEEK_API_KEY=sk-你的真实Key
```

### 3. C 端 App（Flutter）

需要先安装 Flutter SDK（https://docs.flutter.dev/get-started/install），然后：

```bash
cd app
flutter create --project-name memorylink_app --org com.memorylink .
flutter run
```

### 4. B 端 / 后台管理

```bash
cd web && npm install && npm run dev   # B 端机构后台（默认 5173）
cd admin && npm install && npm run dev # 平台后台管理（默认 5174）
```

后续 UI 开发时补充：`npm install antd axios react-router-dom`。

## 环境变量

复制 `.env.example` 为 `.env` 并按需修改；服务端通过环境变量读取数据库、Redis、对象存储与 DeepSeek 配置。

## 关键决策（详见《技术架构设计书 V1.4》）

- 后端：Java 17+ / Spring Boot 3（Maven）
- 移动端：Flutter
- B 端/后台：React + TypeScript + Ant Design
- 数据库：PostgreSQL + Redis；对象存储：S3 兼容（自选）
- LLM：DeepSeek（deepseek-v4-flash）
- 部署：Docker Compose，自行部署（云中立）；推送方案部署时自定

## 文档

- 《忆联（MemoryLink）市场可行性报告 V1.4》
- 《忆联（MemoryLink）产品需求说明书 V1.5》
- 《忆联（MemoryLink）试点项目计划 V1.4》
- 《忆联（MemoryLink）技术架构设计书 V1.4》
- 《忆联（MemoryLink）开发进度跟踪》（长期维护）

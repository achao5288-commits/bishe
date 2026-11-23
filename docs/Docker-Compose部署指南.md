# Docker Compose 部署指南

适用于换到已安装 Docker 的电脑后运行毕业设计。当前开发电脑没有安装 Docker，镜像构建、健康检查和容器联通仍须在目标电脑实际验证。

## 1. 准备

安装并启动 Docker Desktop，或准备 Docker Engine 和 Compose v2。电脑需要联网拉取镜像和构建依赖，并为 MySQL、Neo4j 预留内存和磁盘。获取代码后进入仓库根目录：

```powershell
git clone https://github.com/achao5288-commits/bishe.git
cd bishe
Copy-Item .env.example .env
notepad .env
```

macOS/Linux 可用 `cp .env.example .env`，再用文本编辑器打开 `.env`。必填项：

- `BISHE_MYSQL_ROOT_PASSWORD`：MySQL root 密码。
- `BISHE_MYSQL_PASSWORD`：应用专用 MySQL 账号密码，应与 root 密码不同。
- `NEO4J_PASSWORD`：Neo4j 密码，至少 8 个字符。
- `APP_JWT_SECRET`：至少 32 字节的随机密钥，用来签发登录令牌。

建议使用各不相同的长随机值，不要提交或分享 `.env` 和密码。为简化 Compose 解析，密码建议使用英文字母和数字；包含 `$` 等特殊字符时须遵循 Compose 的 `.env` 转义规则。`.env` 已被 Git 忽略。终端中同名环境变量可能覆盖 `.env` 的值。

如需初始管理员，`APP_ADMIN_USERNAME` 和 `APP_ADMIN_PASSWORD` 必须同时填写；用户名为 3–32 位字母、数字或下划线，密码至少 8 位。首次创建后修改环境变量不会修改已有账号密码，也不会提升已有同名普通用户。普通用户可以在网站注册。

`DEEPSEEK_API_KEY` 可选，不填写时不能使用需要远程模型的问答功能；使用前请确认服务商费用和额度。`WEB_PORT` 默认 8080，可改为其他未占用端口。Redis 默认关闭。

## 2. 构建与启动

在项目根目录执行：

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=50 backend
```

首次构建需要下载基础镜像与依赖，可能较慢。MySQL 和 Neo4j 健康检查通过后才启动后端，后端启动完成后才启动前端；应用会初始化数据库表及图谱种子。浏览器打开 http://127.0.0.1:8080 ，如更改了 `WEB_PORT`，改用相应端口。只有前端绑定本机地址，数据库和后端均只在 Compose 内部网络访问。

后端 `/api/health` 表示应用已完成启动；它不是持续的数据库健康探针。启动失败时使用 `docker compose ps` 和对应服务的日志排查，避免公开可能包含敏感信息的日志。不要将 `docker compose config` 的完整输出发给别人，其中可能包含密码。

## 3. 启用 Redis 缓存

在 `.env` 中设置 `REDIS_CACHE_ENABLED=true`，然后执行：

```powershell
docker compose --profile cache up -d --build
docker compose --profile cache ps
```

Redis 只在启用 `cache` profile 时启动，默认不向宿主机公开、无密码，适合本机演示；缓存故障时应用回退查询。`.env` 的 `REDIS_PASSWORD` 仅在自行修改 Redis 服务以启用认证时才填写，默认服务留空。重新关闭缓存时将 `REDIS_CACHE_ENABLED` 改回 `false` 并重新运行 `docker compose up -d`；需要同时移除缓存容器可运行 `docker compose --profile cache down` 后重新启动基础服务。缓存不是持久存储，重新创建 Redis 时缓存会丢失。

## 4. 停止、更新与数据

```powershell
docker compose --profile cache stop
docker compose --profile cache up -d --build
```

`docker compose --profile cache down` 会删除容器和网络，但保留 MySQL、Neo4j 的命名数据卷。不要添加 `-v`，除非已备份且明确要删除数据库。首次启动创建新数据卷，不会自动导入旧电脑已有账号、评分或图谱数据；迁移前请分别备份和恢复 MySQL 与 Neo4j，并核对版本兼容性。Neo4j 每次应用启动会按稳定 ID 合并种子并覆盖对应字段；自定义资料需提前备份。

**修改 `.env` 中的数据库密码不会自动修改已有数据卷里的数据库账号密码。** 应先按数据库官方方法修改已有账号密码，再更新 `.env` 并重建相关容器；不要通过删除数据卷解决登录问题。更改 JWT 密钥会使旧登录令牌失效。

## 5. 开发模式：只运行依赖服务

需要在宿主机直接运行后端时，可叠加开发配置，仅将数据库端口绑定到本机。先按第 1 节配置完整 `.env`，再执行：

```powershell
docker compose -f compose.yaml -f compose.dev.yaml up -d mysql neo4j
# 如需 Redis：
docker compose -f compose.yaml -f compose.dev.yaml --profile cache up -d redis
```

默认宿主机端口为 MySQL 3306、Neo4j 7474/7687、Redis 6379。如果已被其他程序占用，请先停止冲突程序或修改 `compose.dev.yaml` 的宿主机端口并同步修改本地后端连接配置。宿主机运行 Maven 时，Compose 的 `.env` 不会自动注入 Maven 进程，需另外在启动后端的终端设置数据库密码、JWT 密钥及其他需要的环境变量。前后端本地运行方法见 README。

## 6. 常见问题

- **端口占用**：调整 `.env` 的 `WEB_PORT` 并重新执行 `docker compose up -d`。
- **数据库长时间 unhealthy**：查看 `docker compose logs --tail=100 mysql neo4j`，检查可用内存、首次初始化和已有数据卷密码；不要直接删除数据卷。
- **后端未就绪**：查看 `docker compose logs --tail=100 backend`，核对数据库、密钥及管理员配置。
- **前端出现 502**：先检查后端健康状态和日志；后端完成初始化后再刷新。
- **缓存未启用**：同时确认 `.env` 设置为 `true` 且启动时包含 `--profile cache`；默认 Redis 无密码。
- **模型问答失败**：核对模型密钥、额度和网络；不要将密钥粘贴进公开日志或截图。

此配置面向本机开发与演示，不直接适用于公网生产环境；生产部署还需 HTTPS、密钥管理、访问控制、监控备份等措施。

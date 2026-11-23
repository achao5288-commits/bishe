# 陕北红色文旅智能推荐系统

Vue 3 + Spring Boot + Neo4j 的毕业设计项目。当前收录延安市杨家岭革命旧址、枣园革命旧址和宝塔山三个景点。每条景点资料和图谱关系均提供公开来源；图片是 AI 生成的主题插画，不是实景照片。开放时间、票价等动态信息请以景区官方发布为准。

## 已实现

- 按地区浏览景点，按地区和主题进行基础推荐。
- 景点、地区和历史事件实体及关系存储在 Neo4j；提供按景点查询知识关联的接口和页面展示。
- 每个景点单独配有插画，页面可打开资料来源。

当前推荐仅做主题匹配排序，尚未利用图谱关联计算推荐分数；尚未实现用户系统、MySQL、大模型问答、路线规划和后台管理。榆林景点尚未收录。

## 数据与来源

种子数据位于 `backend/src/main/resources/graph-seed.json`。景点及关联事实依据：

- 杨家岭与中共七大：[中央纪委国家监委网站](https://www.ccdi.gov.cn/toutiao/202105/t20210503_241354.html)。
- 枣园与中共中央书记处：[陕西档案信息网](https://daj.shaanxi.gov.cn/Article/View?id=4757)。
- 宝塔山与延安革命纪念地：[文化和旅游部数据服务](https://sjfw.mct.gov.cn/site/dataservice/culdetails?curId=10&id=35098)。

应用启动时按稳定 ID 执行 MERGE 写入种子和关系；重复启动不会重复创建节点。种子字段会覆盖数据库中同 ID 的对应字段，新增或修订资料请修改种子并核对来源。种子不会自动删除已从文件移除的旧节点或关系，删除需单独管理。勿在未备份的共享或生产数据库上运行此初始化流程。

## 本地启动

需要 Java 21、Maven 3.9+、Node.js 20+、Neo4j 5.26。请先启动 Neo4j 并创建本地开发用户 `neo4j`，示例密码为 `local-dev-password`。可使用 Neo4j Desktop 或以下 Docker Compose（安装 Docker 后）：

```bash
docker compose up -d
```

上述密码仅限本地开发，Compose 只绑定本机端口。已有数据卷首次创建后，修改环境变量不会自动修改数据库密码；请在 Neo4j 中按其管理方式更改。自定义连接通过 `NEO4J_URI`、`NEO4J_USER`、`NEO4J_PASSWORD` 环境变量配置，默认分别为 `bolt://localhost:7687`、`neo4j` 和 `local-dev-password`。确保数据库已启动、账号密码正确且有创建约束及写入权限。应用启动时直接连接并初始化图谱，数据库不可用时应用启动失败。

```bash
cd backend
mvn test
mvn spring-boot:run
```

测试会连接并写入上述配置的 Neo4j，建议专门使用本地测试数据库，不要指向生产数据。后端默认在 `http://localhost:8081`。

另开一个终端：

```bash
cd frontend
npm install
npm run dev
```

打开 Vite 输出的地址；开发服务器把 `/api` 代理至 `http://localhost:8081`。图谱接口示例：`GET /api/graph/attractions/1`。景点不存在时返回 404。

## 后续工作

扩充和审核更多陕北景点及人物事件关系；完善图谱可视化与图谱增强推荐；接入资料检索与大模型；实现用户权限和后台数据维护；开展可复现的推荐实验。总体设计见 `docs/开发设计文档.docx`，该文档是规划，具体完成情况以本 README 为准。

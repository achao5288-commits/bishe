# 陕北红色文旅智能推荐系统

Vue 3 + Spring Boot + Neo4j 的毕业设计项目。当前收录延安市和榆林市的八处红色景点，包括杨家沟革命旧址和小河会议旧址。每条景点资料和图谱关系均提供公开来源；图片是 AI 生成的主题插画，不是实景照片。开放时间、票价等动态信息请以景区官方发布为准。

## 已实现

- 按地区浏览景点，结合主题和用户选择的人物、事件、著作或文化主题进行图谱推荐。
- 景点、地区、历史事件、人物、著作和主题实体及其关系存储在 Neo4j；提供可拖动、缩放、点击和按类别筛选的关系图，也提供键盘可操作的清单。
- 每个景点单独配有插画，页面可打开资料来源。

推荐按照当前用户主动选择的条件计算，尚无登录、长期兴趣画像或行为数据；尚未实现用户系统、MySQL、大模型问答、路线规划和后台管理。目前榆林收录杨家沟革命旧址和小河会议旧址，覆盖范围仍可继续扩展。

## 基于知识图谱的推荐

用户在页面选择地区、景点类型和一项知识兴趣（人物、事件、著作或主题）；也可从图谱中选择节点后点击“用作推荐兴趣”。接口支持最多 10 个兴趣 ID。地区为硬筛选，使用 Neo4j 沿已维护的有向知识关系查找从景点出发的一跳或两跳路径；地区节点不参与兴趣计分。每个不同的兴趣只计一次，并选择最短路径：一跳加 60 分，两跳加 30 分，类型匹配加 20 分。多个不同兴趣分数相加，分数相同时按景点 ID 排序。权重是便于演示的人工规则，并未经过用户实验验证；无图谱匹配时按类型和稳定顺序返回候选景点。

推荐接口 `POST /api/recommendations` 示例：

```json
{"region":"榆林","themes":["革命旧址"],"interestIds":["event-xiaohe"],"limit":5}
```

返回的 `score` 用于排序，`graphMatches` 包含命中的实体、跳数、路径以及每段关系的来源链接；没有匹配时该列表为空。网页目前每次只选择一项知识兴趣，用户选择只用于当前请求，不保存个人行为数据。页面图谱的数据来自 `GET /api/graph`，当前该接口最多返回 500 条关系；大规模数据需要分页或按子图查询。

## 数据与来源

种子数据位于 `backend/src/main/resources/graph-seed.json`。景点及关联事实依据：

- 杨家岭与中共七大：[中央纪委国家监委网站](https://www.ccdi.gov.cn/toutiao/202105/t20210503_241354.html)。
- 枣园与中共中央书记处：[陕西档案信息网](https://daj.shaanxi.gov.cn/Article/View?id=4757)。
- 宝塔山与延安革命纪念地：[文化和旅游部数据服务](https://sjfw.mct.gov.cn/site/dataservice/culdetails?curId=10&id=35098)。
- 延安革命纪念馆：[中央纪委国家监委网站](https://m.ccdi.gov.cn/content/dd/63/5314.html)。
- 王家坪革命旧址：[彰武县人民政府](https://www.zhangwu.gov.cn/content/2018/636742.html)。
- 南泥湾革命旧址：[陕西档案信息网](https://daj.shaanxi.gov.cn/Article/View?id=4757)。
- 杨家沟与十二月会议：[榆林政法网](https://www.ylzf.gov.cn/newsinfo/1576403.html)。
- 小河会议旧址：[文化和旅游部](https://zhuanti.mct.gov.cn/history_detail/782.html)。
- 延安文艺座谈会及相关人物著作：[国家林业和草原局](https://www.forestry.gov.cn/lyj/1/dszl/20250502/623548.html)。
- 张思德追悼会及《为人民服务》：[中华人民共和国国防部](https://www.mod.gov.cn/gfbw/gfjy_index/16117605.html)。

应用启动时按稳定 ID 执行 MERGE 写入种子和关系；重复启动不会重复创建节点。种子字段会覆盖数据库中同 ID 的对应字段，新增或修订资料请修改种子并核对来源。种子不会自动删除已从文件移除的旧节点或关系，删除需单独管理。勿在未备份的共享或生产数据库上运行此初始化流程。

## 资料采集与人工审核

搜狐攻略文章可作为人工发现景点的线索，但该站点的 robots.txt 对本项目采集程序禁止抓取该文章，实际检查记录见 `data/collection-check.json`。攻略含 AI 生成内容；票价、预约、交通等信息未经核验，不纳入项目数据。新增景点和关系由人工通过官方或公共机构资料核对后录入，不自动同步到图谱。

采集工具 `tools/crawler/collector.py` 可处理用户指定的 HTTPS 页面。它先检查各站 robots.txt；禁止访问或规则不可取得时停止对应页面，不跟随重定向，仅记录标题及词表中命中的景点名称，不保存文章全文，也不自动修改 Neo4j。默认间隔两秒；只访问给定 URL，不发现或批量遍历链接。示例：

```bash
python tools/crawler/collector.py https://example.org/article --terms tools/crawler/terms.json --output data/candidates.json
python -m unittest discover -s tools/crawler/tests -v
```

运行测试前可设置 `PYTHONPATH=tools/crawler`；Windows PowerShell 使用 `$env:PYTHONPATH="tools/crawler"`。对待审核结果进行来源核实、图片版权检查和人工编辑后，才可更新种子数据。采集工具仅用于明确允许抓取的页面，不能绕过站点权限或反爬限制。

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

打开 Vite 输出的地址；开发服务器把 `/api` 代理至 `http://localhost:8081`。图谱接口示例：`GET /api/graph` 查询最多 500 条关系，`GET /api/graph/attractions/1` 查询该景点向外最多两跳的关联。景点不存在时返回 404。

## 后续工作

扩充和审核更多陕北景点及人物事件关系；完善图谱可视化与图谱增强推荐；接入资料检索与大模型；实现用户权限和后台数据维护；开展可复现的推荐实验。总体设计见 `docs/开发设计文档.docx`，该文档是规划，具体完成情况以本 README 为准。

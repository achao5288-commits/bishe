# 陕北红色文旅智能推荐系统

Vue 3 + Spring Boot + Neo4j 的毕业设计项目。当前收录延安市和榆林市的十六处红色景点，包括凤凰山、瓦窑堡、吴起、杨家沟、小河和神泉堡等。每条景点资料和图谱关系均提供公开来源；图片是 AI 生成的主题插画，不是实景照片。开放时间、票价等动态信息请以景区官方发布为准。

## 已实现

- 按地区浏览景点，结合主题和用户选择的人物、事件、著作或文化主题进行图谱推荐。
- 景点、地区、历史事件、人物、著作和主题实体及其关系存储在 Neo4j；提供可拖动、缩放、点击和按类别筛选的关系图，也提供键盘可操作的清单。
- 每个景点单独配有插画和分段历史故事；每个故事段落附有资料来源。
- 智能问答基于已整理的故事和 Neo4j 关系检索，并使用 DeepSeek 模型生成回答；展示检索来源。

推荐按照当前用户主动选择的条件计算，尚无登录、长期兴趣画像或行为数据；尚未实现用户系统、MySQL、路线规划和后台管理。目前榆林收录杨家沟、小河、神泉堡、绥德县革命历史纪念馆和郝家桥五处地点，覆盖范围仍可继续扩展。

## 基于知识图谱的推荐

用户可选择地区、景点类型，以及最多 10 项人物、事件、著作或主题；也可从图谱添加兴趣。地区是硬筛选，景点类型匹配加 20 分。对每个不同兴趣，Neo4j 查找景点出发的有向一跳或两跳路径；一跳关系的基础权重分别为：关联事件 60、关联人物 50、关联著作 45、参与事件 45、属于主题 40。两跳路径取各关系权重的最小值后减半（向上取整），取同一景点与兴趣之间得分最高的路径，每个兴趣只计一次；所有兴趣得分与类型分相加。同分按景点 ID 排序，未命中兴趣时回退到类型与稳定顺序。

例如：同时选择洛川会议与毛泽东，洛川会议纪念馆的两条直接关系分别贡献 60 和 50 分。接口返回每条路径的关系类型、分值和来源链接，页面展示实际命中的所有兴趣。权重是人工设定的演示参数，并未通过用户实验调优；用户选择只用于当前请求，不保存行为画像。

推荐接口 `POST /api/recommendations` 示例：

```json
{"region":"延安","themes":["纪念馆"],"interestIds":["event-luochuan","person-mao"],"limit":5}
```

返回的 `score` 用于排序；`graphMatches` 中的 `relationTypes`、`points`、`path` 与 `sources` 说明每条命中和来源。当前图谱接口最多返回 500 条关系；大规模数据需要分页或按子图查询。

### 离线评估与局限

启动后端和 Neo4j 后，在项目根目录运行：

```sh
node --test tools/evaluation/evaluate.test.cjs
node tools/evaluation/evaluate.cjs
```

可在第二条命令末尾传入后端地址。脚本读取 `tools/evaluation/cases.json` 中 9 个人工构造场景及分级相关性标注（0–3，未标注为 0），计算 Precision@3、Recall@3 和 NDCG@3 的逐场景与平均值。基线使用同一候选集合和图谱匹配路径，重现原先一跳 60、两跳 30、类型 20 的规则；新方法使用接口当前排序。两个方法的候选、筛选和排序平局规则一致。为防止截断造成不公平对比，单地区候选超过接口上限 20 时脚本会停止，需先实现分页。

在当前种子数据和示例标注上，基线 P@3=0.5926、R@3=1.0000、NDCG@3=0.9816；加权方法分别为 0.5926、1.0000、1.0000。**这些是开发者构造的小样本演示结果，部分判断反映设计假设，并非真实用户研究或模型泛化效果。**实际论文结论应另行收集独立用户评分，划分独立的调参和测试数据，并报告样本量、评测过程及结果。

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
- 凤凰山革命旧址：[陕西档案信息网](https://daj.shaanxi.gov.cn/Article/View?id=4757)。
- 瓦窑堡会议旧址：[中华人民共和国国防部](https://www.mod.gov.cn/gfbw/tp_214132/tsjs/4879356.html)。
- 吴起中央红军长征胜利纪念园：[延安纪检监察网](https://yanan.qinfeng.gov.cn/info/1217/25277.htm)。
- 神泉堡革命纪念馆：[中共定边县纪委](https://dingbian.qinfeng.gov.cn/info/1084/4233.htm)。
- 洛川会议纪念馆：[文化和旅游部](https://zhuanti.mct.gov.cn/dhxlsfn2022shanxi2/detail/2499.html)、[中央纪委国家监委](https://m.ccdi.gov.cn/content/0a/20/5338.html)。
- 绥德县革命历史纪念馆：[绥德县纪委监委](https://suide.qinfeng.gov.cn/info/1046/2407.htm)。
- 郝家桥红色革命教育基地：[文化和旅游部](https://www.mct.gov.cn/wlbphone/wlbydd/xxfb/qglb/sx_9787/202104/t20210426_923965.html)。
- 刘志丹烈士陵园：[陕西纪检监察网](https://www.qinfeng.gov.cn/info/4766/205366.htm)、[陕西档案信息网](https://daj.shaanxi.gov.cn/Article/View?id=3185)。
- 延安文艺座谈会及相关人物著作：[国家林业和草原局](https://www.forestry.gov.cn/lyj/1/dszl/20250502/623548.html)。
- 张思德追悼会及《为人民服务》：[中华人民共和国国防部](https://www.mod.gov.cn/gfbw/gfjy_index/16117605.html)。

应用启动时按稳定 ID 执行 MERGE 写入种子和关系；重复启动不会重复创建节点。种子字段会覆盖数据库中同 ID 的对应字段，新增或修订资料请修改种子并核对来源。种子不会自动删除已从文件移除的旧节点或关系，删除需单独管理。勿在未备份的共享或生产数据库上运行此初始化流程。

## 大模型问答

问答使用 DeepSeek 官方 API 的 `deepseek-flash`，关闭思考模式以控制响应时间和输出。**DeepSeek API 按 token 计费，不保证免费**；赠送额度与余额以账户实际情况为准，价格请查看 [DeepSeek 官方定价](https://api-docs.deepseek.com/quick_start/pricing/)。项目不包含密钥；密钥只能保存在服务器环境变量，不要提交到仓库或发给他人。

在启动后端的 PowerShell 终端中设置（输入时不回显），然后运行 `mvn spring-boot:run`：

```powershell
$env:DEEPSEEK_API_KEY = [System.Net.NetworkCredential]::new('', (Read-Host '输入 DeepSeek API 密钥' -AsSecureString)).Password
mvn spring-boot:run
```

未配置密钥时，问答明确返回不可用，景点阅读和图谱仍可使用。`POST /api/chat` 请求示例：

```json
{"question":"请讲讲杨家岭的完整故事", "attractionId":1}
```

`attractionId` 可省略，系统会按景点名称、数据中的常用简称或图谱人物/事件名称匹配，最多处理三个景点；未匹配、范围过宽或询问未核实的开放时间、票价等实时信息时，直接说明资料不足，不调用大模型。后台把已整理的故事段落及相关图谱关系发送给模型，并给检索来源编号；服务器仅接受与本次检索来源匹配的方括号编号，无法匹配的编号会标记为未核实。来源是回答所使用的资料集合，编号有效也不代表所引来源一定支持该句，不等于对每句话逐一校验；模型仍可能出错，重要历史事实应核查原始来源。当前故事是已核验资料的分段梳理，无法保证囊括所有史料。

问题和检索资料会发送到 DeepSeek，请勿输入敏感个人信息。每次请求独立，不传入先前聊天记录；页面记录仅保存在当前页面内。示例及测试不包含真实密钥；网关单元测试使用模拟响应，不代表真实服务已连通。

## 历史故事维护

每处景点的背景故事保存在 `backend/src/main/resources/attraction-stories.json`，通过 `GET /api/attractions/{id}/story` 返回。故事按背景、关键经过和后续历史或阅读线索分段，每段保存对应来源名称与链接；正文为依据资料整理的叙述，不是原文全文。现有故事覆盖十二处已收录景点，但历史材料仍可继续扩充，研究时应查看所列来源。

新增或修改景点时，应同步添加同 ID 的故事，核查时间、人物、事件与每段来源。`StoryCoverageTest` 检查景点和故事 ID 一一对应，以及每段标题、正文和 HTTPS 来源非空；该自动检查不能代替历史事实的人工核验。修改 JSON 后重启后端加载。

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

扩充和审核更多陕北景点及人物事件关系；继续优化检索、可视化与图谱增强推荐；实现用户权限和后台数据维护；开展可复现的推荐实验。总体设计见 `docs/开发设计文档.docx`，该文档是规划，具体完成情况以本 README 为准。

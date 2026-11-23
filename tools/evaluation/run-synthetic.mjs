import fs from 'node:fs';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { buildProfile } from '../../frontend/src/profile.js';
const require=createRequire(import.meta.url),{summarize}=require('./synthetic.cjs');
const fixture=JSON.parse(fs.readFileSync(new URL('./synthetic-users.json',import.meta.url),'utf8').replace(/^\uFEFF/,''));
const baseline=JSON.parse(fs.readFileSync(new URL('./coverage-baseline.json',import.meta.url),'utf8'));
const hash=x=>crypto.createHash('sha256').update(JSON.stringify(x)).digest('hex');
const seed=JSON.parse(fs.readFileSync(new URL('../../backend/src/main/resources/graph-seed.json',import.meta.url),'utf8'));
if(hash(fixture)!==baseline.fixtureHash || hash(seed)!==baseline.graphSeedHash)throw Error('模拟场景或图谱种子与固定基线不同，不能直接比较');
const canonicalGraph=g=>({nodes:[...g.nodes].sort((a,b)=>a.id.localeCompare(b.id)),edges:[...g.edges].sort((a,b)=>`${a.source}|${a.type}|${a.target}|${a.sourceUrl}`.localeCompare(`${b.source}|${b.type}|${b.target}|${b.sourceUrl}`))});
const base=process.argv[2] || 'http://127.0.0.1:8081',k=3;
async function api(path, options){const r=await fetch(base+path,options);if(!r.ok)throw Error(`${path}: HTTP ${r.status}`);return r.json();}
const [graph,attractions]=await Promise.all([api('/api/graph'),api('/api/attractions')]);
if(attractions.length>20)throw Error('候选景点超过20项，请先实现分页评估');
if(hash(canonicalGraph(graph))!==baseline.liveGraphHash || hash([...attractions].sort((a,b)=>a.id-b.id))!==baseline.attractionsHash)throw Error('运行中的图谱或候选景点与固定基线不一致，不能直接比较');
if(fixture.users.length!==baseline.users || attractions.length!==baseline.attractions)throw Error('固定基线样本数量不一致');
const validIds=new Set(attractions.map(x=>x.id)), rows=[], ratingRows=[];
for(const user of fixture.users){
 if([...user.favorites,...user.history,...Object.keys(user.ratings).map(Number)].some(id=>!validIds.has(id)))throw Error(`${user.id} 存在未知景点`);
 const ratings=Object.fromEntries(attractions.map(a=>[a.id,user.ratings[a.id] ?? fixture.defaultRating]));
 const seen=new Set([...user.favorites,...user.history]);
 ratingRows.push(...attractions.map(a=>({userId:user.id,attractionId:a.id,rating:ratings[a.id],isHistory:seen.has(a.id),origin:"synthetic"})));
 if(!attractions.some(a=>!seen.has(a.id)&&ratings[a.id]>=4))throw Error(`${user.id} 没有留出的正面评分`);
 const profileInterests=Object.fromEntries(buildProfile(user,graph).map(({id,weight})=>[id,weight]));
 const body={region:'',themes:[],interestIds:[],profileInterests,favoriteIds:user.favorites,historyIds:user.history,limit:20};
 const ranked=await api('/api/recommendations',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
 if(ranked.length!==attractions.length)throw Error(`${user.id} 候选集不完整`);
 const before=[...ranked].sort((a,b)=>b.score-a.score||a.id-b.id);
 const previous=baseline.cases.find(c=>c.id===user.id)?.result;
 if(!previous)throw Error(`固定基线缺少模拟用户 ${user.id}`);
 rows.push({id:user.id,scenario:user.scenario,favorites:user.favorites,history:user.history,before:summarize(before,ratings,seen,k),previous,after:summarize(ranked,ratings,seen,k)});
}
const keys=['precision','recall','ndcg','repeated','themeDiversity','regionDiversity'];
const mean=method=>Object.fromEntries(keys.map(key=>[key,Number((rows.reduce((sum,row)=>sum+row[method][key],0)/rows.length).toFixed(4))]));
const coverage=method=>Number((new Set(rows.flatMap(row=>row[method].top.map(x=>x.id))).size/attractions.length).toFixed(4));
const report={description:fixture.description,method:'同一组模拟用户和候选景点；基础排序按画像基础分，上版使用去重多样性重排，本版在平分时按兴趣和行为稳定分散。收藏和浏览景点不算作留出的正面目标。',ratingRule:'未单独设定的景点评为模拟3分；4–5分为正面，收藏和浏览景点不计入留出目标。',k,users:rows.length,attractions:attractions.length,simulatedRatings:rows.length*attractions.length,mean:{before:{...mean('before'),coverage:coverage('before')},previous:{...mean('previous'),coverage:coverage('previous')},after:{...mean('after'),coverage:coverage('after')}},cases:rows};
const out=new URL('../../frontend/public/data/synthetic-report.json',import.meta.url);fs.mkdirSync(new URL('../../frontend/public/data/',import.meta.url),{recursive:true});fs.writeFileSync(out,JSON.stringify(report,null,2)+'\n');fs.writeFileSync(new URL('./synthetic-ratings.json',import.meta.url),JSON.stringify({description:"模拟评分，不是真实用户数据",ratings:ratingRows},null,2)+'\n');console.log(JSON.stringify({users:report.users,simulatedRatings:report.simulatedRatings,mean:report.mean},null,2));

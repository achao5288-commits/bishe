import fs from 'node:fs';
import { createRequire } from 'node:module';
import { buildProfile } from '../../frontend/src/profile.js';
const require=createRequire(import.meta.url),{summarize}=require('./synthetic.cjs');
const fixture=JSON.parse(fs.readFileSync(new URL('./synthetic-users.json',import.meta.url),'utf8').replace(/^\uFEFF/,''));
const base=process.argv[2] || 'http://127.0.0.1:8081',k=3;
async function api(path, options){const r=await fetch(base+path,options);if(!r.ok)throw Error(`${path}: HTTP ${r.status}`);return r.json();}
const [graph,attractions]=await Promise.all([api('/api/graph'),api('/api/attractions')]);
if(attractions.length>20)throw Error('候选景点超过20项，请先实现分页评估');
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
 rows.push({id:user.id,scenario:user.scenario,favorites:user.favorites,history:user.history,before:summarize(before,ratings,seen,k),after:summarize(ranked,ratings,seen,k)});
}
const keys=['precision','recall','ndcg','repeated','themeDiversity','regionDiversity'];
const mean=method=>Object.fromEntries(keys.map(key=>[key,Number((rows.reduce((sum,row)=>sum+row[method][key],0)/rows.length).toFixed(4))]));
const coverage=method=>Number((new Set(rows.flatMap(row=>row[method].top.map(x=>x.id))).size/attractions.length).toFixed(4));
const report={description:fixture.description,method:'同一组模拟用户和候选景点；优化前按画像基础分排序，优化后加入去重与多样性重排。收藏和浏览景点不算作留出的正面目标。',ratingRule:'未单独设定的景点评为模拟3分；4–5分为正面，收藏和浏览景点不计入留出目标。',k,users:rows.length,attractions:attractions.length,simulatedRatings:rows.length*attractions.length,mean:{before:{...mean('before'),coverage:coverage('before')},after:{...mean('after'),coverage:coverage('after')}},cases:rows};
const out=new URL('../../frontend/public/data/synthetic-report.json',import.meta.url);fs.mkdirSync(new URL('../../frontend/public/data/',import.meta.url),{recursive:true});fs.writeFileSync(out,JSON.stringify(report,null,2)+'\n');fs.writeFileSync(new URL('./synthetic-ratings.json',import.meta.url),JSON.stringify({description:"模拟评分，不是真实用户数据",ratings:ratingRows},null,2)+'\n');console.log(JSON.stringify({users:report.users,simulatedRatings:report.simulatedRatings,mean:report.mean},null,2));

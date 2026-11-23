import fs from 'node:fs';
import { createRequire } from 'node:module';
import { buildProfile } from '../../frontend/src/profile.js';
const require=createRequire(import.meta.url), {metrics}=require('./evaluate.cjs');
const {description,cases}=JSON.parse(fs.readFileSync(new URL('./profile-cases.json',import.meta.url),'utf8').replace(/^\uFEFF/,''));
const base=process.argv[2] || 'http://127.0.0.1:8081', k=3;
async function get(path,options){const r=await fetch(base+path,options);if(!r.ok)throw Error(`${path}: HTTP ${r.status}`);return r.json()}
const graph=await get('/api/graph'), attractions=await get('/api/attractions');
if(attractions.length>20)throw Error('超过 20 个候选景点，需要分页后评估');
let rows=[];
for(const c of cases){
 const profile=Object.fromEntries(buildProfile(c,graph).map(({id,weight})=>[id,weight]));
 const body={region:'',themes:[],interestIds:[],limit:20};
 const recommend=profileInterests=>get('/api/recommendations',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,profileInterests})});
 const baseline=await recommend({}), personalized=await recommend(profile);
 if(baseline.length!==attractions.length||personalized.length!==attractions.length)throw Error('候选集合不完整');
 const train=new Set([...c.favorites,...c.history]);
 const summarize=items=>({ ...metrics(items,c.relevance), top:items.slice(0,k).map(x=>({id:x.id,name:x.name})), repeated:items.slice(0,k).filter(x=>train.has(x.id)).length/k });
 rows.push({id:c.id,profile,baseline:summarize(baseline),personalized:summarize(personalized)});
}
const mean=method=>Object.fromEntries(['precision','recall','ndcg','repeated'].map(key=>[key,Number((rows.reduce((s,r)=>s+r[method][key],0)/rows.length).toFixed(4))]));
const coverage=method=>Number((new Set(rows.flatMap(r=>r[method].top.map(x=>x.id))).size/attractions.length).toFixed(4));
console.log(JSON.stringify({description,k,cases:rows,mean:{baseline:{...mean('baseline'),coverage:coverage('baseline')},personalized:{...mean('personalized'),coverage:coverage('personalized')}}},null,2));

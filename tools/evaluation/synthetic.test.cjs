const test=require('node:test');
const assert=require('node:assert/strict');
const {summarize}=require('./synthetic.cjs');
test('scores held-out ratings and does not count known visits as new discoveries',()=>{
 const items=[{id:1,theme:'A',region:'X'},{id:2,theme:'A',region:'X'},{id:3,theme:'B',region:'Y'}];
 const result=summarize(items,{'1':5,'2':4,'3':3},new Set([1]));
 assert.equal(result.precision,1/3); assert.equal(result.recall,1); assert.equal(result.repeated,1/3);
 assert.equal(result.themeDiversity,2/3); assert.equal(result.regionDiversity,2/3);
 assert.ok(result.ndcg>0 && result.ndcg<1);
});
test('missing rating must not silently become a negative label',()=>{
 assert.throws(()=>summarize([{id:1,theme:'A',region:'X'}],{},new Set()),/评分/);
});
test('published simulated data is complete and matches the report',()=>{
 const fs=require('node:fs'), path=require('node:path');
 const fixture=JSON.parse(fs.readFileSync(path.join(__dirname,'synthetic-users.json'),'utf8').replace(/^\uFEFF/,''));
 const ratings=JSON.parse(fs.readFileSync(path.join(__dirname,'synthetic-ratings.json'),'utf8'));
 const report=JSON.parse(fs.readFileSync(path.join(__dirname,'../../frontend/public/data/synthetic-report.json'),'utf8'));
 assert.equal(fixture.users.length,12); assert.equal(ratings.ratings.length,report.users*report.attractions);
 assert.equal(report.simulatedRatings,ratings.ratings.length);
 const keys=new Set(ratings.ratings.map(r=>`${r.userId}|${r.attractionId}`));
 assert.equal(keys.size,ratings.ratings.length);
 assert.ok(ratings.ratings.every(r=>r.origin==='synthetic' && r.rating>=1 && r.rating<=5));
});

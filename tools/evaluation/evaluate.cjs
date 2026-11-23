// Compare the current API ranking with the previous 60/30 rule on a small, labeled demonstration set.
const fs = require('node:fs');
const path = require('node:path');
const { cases, description } = JSON.parse(fs.readFileSync(path.join(__dirname, 'cases.json'), 'utf8').replace(/^\uFEFF/, ''));
const base = process.argv[2] || 'http://127.0.0.1:8081';
const k = 3;
function metrics(ranked, relevance) {
  const rel = id => Number(relevance[id] || 0);
  const relevant = Object.values(relevance).filter(v => v > 0).length;
  const top = ranked.slice(0, k), hits = top.filter(item => rel(item.id) > 0).length;
  const dcg = grades => grades.reduce((sum, grade, i) => sum + (Math.pow(2, grade) - 1) / Math.log2(i + 2), 0);
  const ideal = Object.values(relevance).map(Number).sort((a,b) => b-a).slice(0,k);
  const idealDcg = dcg(ideal);
  return { precision: hits/k, recall: relevant ? hits/relevant : 0, ndcg: idealDcg ? dcg(top.map(item => rel(item.id)))/idealDcg : 0 };
}
async function run() {
  const rows = [];
  for (const c of cases) {
    const listResponse = await fetch(`${base}/api/attractions?${new URLSearchParams({region:c.region})}`);
    if (!listResponse.ok) throw new Error(`case ${c.id}: cannot load attractions`);
    const available = await listResponse.json();
    if (available.length > 20) throw new Error(`case ${c.id}: more than 20 candidates; evaluation requires pagination`);
    const response = await fetch(`${base}/api/recommendations`, { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({region:c.region, themes:c.themes, interestIds:c.interestIds, limit:20}) });
    if (!response.ok) throw new Error(`case ${c.id}: HTTP ${response.status}`);
    const current = (await response.json()).sort((a,b) => b.score-a.score || a.id-b.id);
    if (current.length !== available.length) throw new Error(`case ${c.id}: incomplete candidate list`);
    const baseline = [...current].sort((a,b) => {
      const score = x => (c.themes.includes(x.theme) ? 20 : 0) + x.graphMatches.reduce((sum,m) => sum + (m.distance === 1 ? 60 : 30),0);
      return score(b)-score(a) || a.id-b.id;
    });
    rows.push({id:c.id, baseline:metrics(baseline,c.relevance), weighted:metrics(current,c.relevance), baselineTop:baseline.slice(0,k).map(x=>x.name), weightedTop:current.slice(0,k).map(x=>x.name)});
  }
  const mean = method => Object.fromEntries(['precision','recall','ndcg'].map(m => [m, Number((rows.reduce((s,r)=>s+r[method][m],0)/rows.length).toFixed(4))]));
  console.log(JSON.stringify({description,k,cases:rows,mean:{baseline:mean('baseline'),weighted:mean('weighted')}},null,2));
}
if (require.main === module) run().catch(error => { console.error(error.message); process.exitCode=1; });
function withinListDiversity(items, key, k=3) {
  const top=items.slice(0,k);
  return top.length ? new Set(top.map(item=>item[key])).size/top.length : 0;
}
module.exports = { metrics, withinListDiversity };


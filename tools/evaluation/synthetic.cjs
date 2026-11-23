const { metrics, withinListDiversity } = require('./evaluate.cjs');
function summarize(items, ratings, seen, k=3) {
 const relevance={};
 for(const item of items) {
  const rating=ratings[item.id];
  if(!Number.isInteger(rating)||rating<1||rating>5) throw Error(`景点 ${item.id} 缺少有效的模拟评分`);
  if(!seen.has(item.id) && rating>=4) relevance[item.id]=rating-3;
 }
 const top=items.slice(0,k);
 return {...metrics(items,relevance), repeated:top.filter(x=>seen.has(x.id)).length/k,
  themeDiversity:withinListDiversity(items,'theme',k),regionDiversity:withinListDiversity(items,'region',k),
  top:top.map(x=>({id:x.id,name:x.name,theme:x.theme,region:x.region,rating:ratings[x.id],seen:seen.has(x.id)}))};
}
module.exports = { summarize };

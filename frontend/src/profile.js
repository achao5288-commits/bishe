const validId = id => Number.isSafeInteger(id) && id > 0;
export function normalizeState(value) {
  const v = value && typeof value === 'object' ? value : {};
  return { favorites: [...new Set((Array.isArray(v.favorites) ? v.favorites : []).filter(validId))].slice(0,100),
    history: [...new Set((Array.isArray(v.history) ? v.history : []).filter(validId))].slice(0,50) };
}
export function toggleFavorite(state, id) {
  const v=normalizeState(state); if (!validId(id)) return v;
  v.favorites = v.favorites.includes(id) ? v.favorites.filter(x=>x!==id) : [id,...v.favorites].slice(0,100);
  return v;
}
export function recordView(state,id) {
  const v=normalizeState(state); if(validId(id)) v.history=[id,...v.history.filter(x=>x!==id)].slice(0,50); return v;
}
export function buildProfile(state, graph) {
  const v=normalizeState(state), values=new Map();
  const allowed=new Set(['RELATED_TO_PERSON','RELATED_TO_EVENT','BELONGS_TO_THEME']);
  const nodes=new Map((graph?.nodes || []).filter(n=>['人物','历史事件','主题','著作'].includes(n.label)).map(n=>[n.id,n]));
  const ids=new Set([...v.favorites,...v.history]);
  for(const id of ids) {
    const contribution=(v.favorites.includes(id)?2:0)+(v.history.includes(id)?1:0);
    const targets=new Set((graph?.edges || []).filter(e=>e.source===`attraction-${id}` && allowed.has(e.type) && nodes.has(e.target)).map(e=>e.target));
    for(const target of targets) values.set(target,Math.min(3,(values.get(target)||0)+contribution));
  }
  return [...values].map(([id,weight])=>({id,name:nodes.get(id).name,weight})).sort((a,b)=>b.weight-a.weight || a.id.localeCompare(b.id)).slice(0,10);
}

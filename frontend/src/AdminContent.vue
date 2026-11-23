<script setup>
import { ref } from 'vue'
const props = defineProps({ request: { type: Function, required: true } })
const emit = defineEmits(['changed'])
const tab = ref('places'), places = ref([]), entities = ref([]), relations = ref([]), message = ref(''), error = ref(''), busy = ref(false)
const emptyPlace = () => ({ id: null, name: '', region: '延安', theme: '革命旧址', description: '', image: '/images/', sourceName: '', sourceUrl: '', aliases: [] })
const emptyStory = id => ({ attractionId: id, title: '', sections: [{ title: '', content: '', sources: [{ name: '', url: '' }] }] })
const place = ref(emptyPlace()), aliases = ref(''), originalPlaceId = ref(null), story = ref(null)
const entity = ref({ id: '', name: '', kind: 'Event', sourceUrl: '' }), originalEntityId = ref(null)
const relation = ref({ source: '', target: '', type: 'RELATED_TO_EVENT', sourceUrl: '' })
const kinds = { Event:'历史事件', Person:'人物', Work:'著作', Theme:'主题' }
const types = { RELATED_TO_EVENT:'关联事件', RELATED_TO_PERSON:'关联人物', RELATED_TO_WORK:'关联著作', BELONGS_TO_THEME:'属于主题', PARTICIPATED_IN:'参与事件' }
async function call(path, options = {}) {
  const res = await props.request('/api/admin/content' + path, options)
  if (!res.ok) { let detail = ''; try { detail = (await res.json()).message || '' } catch {} throw Error(detail || `操作失败（${res.status}），请检查输入和权限。`) }
  return res.status === 204 ? null : res.json()
}
const json = (method, data) => ({ method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })
async function load() { busy.value=true; error.value=''; try { [places.value,entities.value,relations.value] = await Promise.all([call('/attractions'),call('/entities'),call('/relations')]) } catch(e){error.value=e.message} finally {busy.value=false} }
async function action(fn, done='保存成功') { error.value=''; message.value=''; busy.value=true; try { await fn(); message.value=done; await load(); emit('changed') } catch(e){error.value=e.message} finally {busy.value=false} }
function editPlace(p) { place.value=JSON.parse(JSON.stringify(p)); aliases.value=p.aliases.join('，'); originalPlaceId.value=p.id; story.value=null }
function newPlace() { place.value=emptyPlace(); aliases.value=''; originalPlaceId.value=null; story.value=null }
async function savePlace() { const data={...place.value,id:Number(place.value.id),aliases:aliases.value.split(/[,，\n]/).map(s=>s.trim()).filter(Boolean)}; if (!originalPlaceId.value && places.value.some(p=>p.id===data.id)) { error.value='景点编号已存在，请选择现有景点编辑或使用其他编号。'; return } await action(async()=>{await call('/attractions/'+data.id,json('PUT',data));editPlace(data)}) }
async function removePlace(p) { if(!confirm(`删除“${p.name}”及其故事和关联？此操作不可撤销。`))return; await action(async()=>{await call('/attractions/'+p.id,{method:'DELETE'});newPlace()},'景点已删除') }
async function editStory(p) { error.value=''; try { story.value=await call('/stories/'+p.id) } catch(e) { if(e.message.includes('404')) story.value=emptyStory(p.id); else error.value=e.message } if(story.value && !story.value.title)story.value.title=p.name }
async function saveStory() { await action(()=>call('/stories/'+story.value.attractionId,json('PUT',story.value))) }
async function removeStory() { if(!confirm('删除此故事？此操作不可撤销。'))return; await action(async()=>{await call('/stories/'+story.value.attractionId,{method:'DELETE'});story.value=null},'故事已删除') }
function editEntity(e) {entity.value=JSON.parse(JSON.stringify(e));originalEntityId.value=e.id}
function newEntity(){entity.value={id:'',name:'',kind:'Event',sourceUrl:''};originalEntityId.value=null}
async function saveEntity(){await action(async()=>{await call('/entities/'+encodeURIComponent(entity.value.id),json('PUT',entity.value));originalEntityId.value=entity.value.id})}
async function removeEntity(e){if(!confirm(`删除实体“${e.name}”及所有关联？此操作不可撤销。`))return;await action(async()=>{await call('/entities/'+encodeURIComponent(e.id),{method:'DELETE'});newEntity()},'实体已删除')}
function editRelation(r){relation.value=JSON.parse(JSON.stringify(r))}
async function saveRelation(){await action(()=>call('/relations',json('PUT',relation.value)))}
async function removeRelation(r){if(!confirm('删除这条图谱关系？'))return;await action(()=>call('/relations',json('DELETE',r)),'关系已删除')}
function display(id){return places.value.find(p=>'attraction-'+p.id===id)?.name || entities.value.find(e=>e.id===id)?.name || id}
</script>
<template>
<details class="content-admin" @toggle="$event.target.open && load()">
  <summary>景点与知识图谱管理</summary>
  <p>维护景点、分段故事与每段来源，以及图谱实体和关系。保存后立即更新本服务的缓存；删除不可撤销。</p>
  <nav class="admin-tabs" aria-label="内容管理分类"><button v-for="item in [{id:'places',name:'景点与故事'},{id:'entities',name:'人物与事件'},{id:'relations',name:'图谱关系'}]" :key="item.id" type="button" :class="{active:tab===item.id}" @click="tab=item.id">{{item.name}}</button></nav>
  <p v-if="message" role="status">{{message}}</p><p v-if="error" role="alert" class="error">{{error}}</p><p v-if="busy" role="status">正在处理…</p>
  <section v-if="tab==='places'" class="admin-layout">
    <div><h3>景点列表</h3><button type="button" @click="newPlace">新增景点</button><ul class="admin-list"><li v-for="p in places" :key="p.id"><span>{{p.name}} <small>#{{p.id}}</small></span><div><button type="button" @click="editPlace(p)">编辑</button><button type="button" @click="editStory(p)">故事</button><button type="button" @click="removePlace(p)">删除</button></div></li></ul></div>
    <div><h3>{{originalPlaceId ? '编辑景点' : '新增景点'}}</h3><form class="admin-form" @submit.prevent="savePlace"><label>景点编号<input v-model="place.id" type="number" min="1" required :disabled="!!originalPlaceId"></label><label>景点名称<input v-model.trim="place.name" required maxlength="150"></label><label>地区<input v-model.trim="place.region" required maxlength="100"></label><label>类型<input v-model.trim="place.theme" required maxlength="100"></label><label>简介<textarea v-model.trim="place.description" required maxlength="4000"></textarea></label><label>插画路径或图片网址<input v-model.trim="place.image" required><small>使用 /images/ 下的文件或 HTTP(S) 图片链接。</small></label><label>来源名称<input v-model.trim="place.sourceName" required></label><label>来源网址<input v-model.trim="place.sourceUrl" type="url" required></label><label>别名（以逗号分隔）<input v-model="aliases"></label><button :disabled="busy">保存景点</button></form>
      <div v-if="story" class="admin-story"><h3>历史故事 · {{story.title}}</h3><form class="admin-form" @submit.prevent="saveStory"><label>故事标题<input v-model.trim="story.title" required></label><fieldset v-for="(section,i) in story.sections" :key="i"><legend>章节 {{i+1}}</legend><label>章节标题<input v-model.trim="section.title" required></label><label>内容<textarea v-model.trim="section.content" rows="5" required></textarea></label><div v-for="(source,j) in section.sources" :key="j" class="admin-source"><label>来源名称<input v-model.trim="source.name" required></label><label>来源网址<input v-model.trim="source.url" type="url" required></label><button type="button" @click="section.sources.splice(j,1)">移除来源</button></div><button type="button" @click="section.sources.push({name:'',url:''})">添加来源</button> <button type="button" @click="story.sections.splice(i,1)">删除章节</button></fieldset><button type="button" @click="story.sections.push({title:'',content:'',sources:[{name:'',url:''}]})">添加章节</button><button :disabled="busy">保存故事</button><button type="button" @click="removeStory">删除故事</button></form></div>
    </div>
  </section>
  <section v-if="tab==='entities'" class="admin-layout"><div><h3>图谱实体</h3><button type="button" @click="newEntity">新增实体</button><ul class="admin-list"><li v-for="e in entities" :key="e.id"><span>{{e.name}} · {{kinds[e.kind]}}</span><div><button type="button" @click="editEntity(e)">编辑</button><button type="button" @click="removeEntity(e)">删除</button></div></li></ul></div><form class="admin-form" @submit.prevent="saveEntity"><h3>{{originalEntityId ? '编辑实体' : '新增实体'}}</h3><label>编号（英文、数字、下划线或连字符）<input v-model.trim="entity.id" required :disabled="!!originalEntityId" pattern="[A-Za-z0-9_-]+"></label><label>名称<input v-model.trim="entity.name" required></label><label>类型<select v-model="entity.kind" :disabled="!!originalEntityId"><option v-for="(name,key) in kinds" :key="key" :value="key">{{name}}</option></select></label><label>来源网址<input v-model.trim="entity.sourceUrl" type="url" required></label><button :disabled="busy">保存实体</button></form></section>
  <section v-if="tab==='relations'" class="admin-layout"><div><h3>图谱关系</h3><ul class="admin-list"><li v-for="r in relations" :key="r.source+r.type+r.target"><span>{{display(r.source)}} → {{types[r.type]}} → {{display(r.target)}}</span><div><button type="button" @click="editRelation(r)">编辑</button><button type="button" @click="removeRelation(r)">删除</button></div></li></ul></div><form class="admin-form" @submit.prevent="saveRelation"><h3>新增或更新关系</h3><label>起点<select v-model="relation.source" required><option value="">请选择</option><option v-for="p in places" :key="p.id" :value="'attraction-'+p.id">景点 · {{p.name}}</option><option v-for="e in entities" :key="e.id" :value="e.id">{{kinds[e.kind]}} · {{e.name}}</option></select></label><label>关系<select v-model="relation.type"><option v-for="(name,key) in types" :key="key" :value="key">{{name}}</option></select></label><label>终点<select v-model="relation.target" required><option value="">请选择</option><option v-for="e in entities" :key="e.id" :value="e.id">{{kinds[e.kind]}} · {{e.name}}</option></select></label><label>关系来源网址<input v-model.trim="relation.sourceUrl" type="url" required></label><p>要更改关系两端或类型，请先删除原关系，再新增。</p><button :disabled="busy">保存关系</button></form></section>
</details>
</template>

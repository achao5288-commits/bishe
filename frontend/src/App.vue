<script setup>
import { ref, computed, onMounted, nextTick, defineAsyncComponent, watch } from 'vue'
import { normalizeState, toggleFavorite, recordView, buildProfile } from './profile.js'
const AdminUsers = defineAsyncComponent(() => import('./AdminUsers.vue'))
const AdminContent = defineAsyncComponent(() => import('./AdminContent.vue'))
const AccountPanel = defineAsyncComponent(() => import('./AccountPanel.vue'))
const GraphView = defineAsyncComponent(() => import('./GraphView.vue'))
const ChatPanel = defineAsyncComponent(() => import('./ChatPanel.vue'))
const EvaluationPanel = defineAsyncComponent(() => import('./EvaluationPanel.vue'))
const attractions = ref([]), recommendations = ref([]), region = ref('延安'), theme = ref('革命旧址'), error = ref(''), loading = ref(false), searched = ref(false), graph = ref(null), graphError = ref(''), graphLoading = ref(false), graphTitle = ref(''), interestIds = ref([]), interestNodes = ref([]), interestsError = ref(false), story = ref(null), storyLoading = ref(false), storyError = ref('')
const storageKey = 'shanbei-profile-v1', sessionKey='shanbei-session-v1'
const behavior = ref({ favorites: [], history: [] }), allPlaces = ref([]), interestGraph = ref(null), useProfile = ref(true), storageError = ref(false), auth = ref(null), accountError=ref('')
function readLocal(){try{return normalizeState(JSON.parse(localStorage.getItem(storageKey) || '{}'))}catch{storageError.value=true;return normalizeState({})}}
behavior.value=readLocal()
try{const saved=JSON.parse(sessionStorage.getItem(sessionKey)||'null');if(saved?.token && saved?.userId)auth.value=saved}catch{}
const profile = computed(() => buildProfile(behavior.value, interestGraph.value))
const placeNames = computed(() => new Map(allPlaces.value.map(a => [a.id,a.name])))
function storeLocal(next){behavior.value=next;try{localStorage.setItem(storageKey,JSON.stringify(next));storageError.value=false}catch{storageError.value=true}}
async function signedFetch(url,options={}){const session=auth.value;if(!session?.token)throw Error('请先登录');const headers=new Headers(options.headers||{});headers.set('Authorization','Bearer '+session.token);const r=await fetch(url,{...options,headers});if(r.status===401 && auth.value?.token===session.token){signOut();accountError.value='登录已过期，请重新登录。'}return r}
async function refreshProfile(session=auth.value){if(!session)return;const [r,a]=await Promise.all([signedFetch('/api/me/profile'),signedFetch('/api/me/account')]);if(!r.ok||!a.ok)throw Error('读取账号记录失败');const [data,account]=await Promise.all([r.json(),a.json()]);if(auth.value?.token===session.token){behavior.value=normalizeState(data);if(auth.value.role!==account.role){auth.value={...auth.value,role:account.role};try{sessionStorage.setItem(sessionKey,JSON.stringify(auth.value))}catch{}}}}
async function signedIn(session){auth.value=session;accountError.value='';try{sessionStorage.setItem(sessionKey,JSON.stringify(session))}catch{accountError.value='浏览器不支持保存登录状态'};behavior.value=normalizeState({});try{await refreshProfile(session)}catch{if(auth.value?.token===session.token)accountError.value='账号记录加载失败，请刷新页面重试。'}}
function signOut(){auth.value=null;sessionStorage.removeItem(sessionKey);behavior.value=readLocal();collectExposures.value=false;feedbackTarget.value=null;exposureId.value=null}
async function changeProfile(path,method){const session=auth.value;try{const r=await signedFetch(path,{method});if(!r.ok)throw Error();await refreshProfile(session);accountError.value=''}catch{if(auth.value?.token===session?.token)accountError.value='账号记录保存失败，请重试。'}}
function favorite(id){if(!auth.value){storeLocal(toggleFavorite(behavior.value,id));return}return changeProfile('/api/me/favorites/'+id,behavior.value.favorites.includes(id)?'DELETE':'PUT')}
function viewed(id){if(!auth.value){storeLocal(recordView(behavior.value,id));return}return changeProfile('/api/me/history/'+id,'POST')}
function clearBehavior(type){if(!auth.value){storeLocal({...behavior.value,[type]:[]});return}return changeProfile('/api/me/'+type,'DELETE')}
const collectExposures = ref(false), exposureId = ref(null), exposureError = ref(''), exposurePending = ref(false), feedbackTarget = ref(null)
function feedbackUserId(){if(!auth.value)throw Error('请先登录');return auth.value.userId}
async function registerExposure(items) { const r=await signedFetch('/api/evaluation/exposures',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({userId:feedbackUserId(),attractionIds:items.map(x=>x.id)})}); if(!r.ok)throw Error(); return (await r.json()).id }
async function openFeedback(item,fromRecommendation=false) { feedbackTarget.value={id:item.id,name:item.name,exposureId:fromRecommendation?exposureId.value:null}; await nextTick(); document.getElementById('evaluation')?.scrollIntoView({behavior:'smooth',block:'start'}) }
let recommendationVersion = 0
function clearRecommendations() { recommendationVersion++; recommendations.value = []; searched.value = false; loading.value = false; exposureId.value = null; exposurePending.value = false }
watch([theme, interestIds, behavior, useProfile, auth], clearRecommendations, { deep: true })
async function load() {
  error.value = ''; graph.value = null; closeStory(); storyError.value = ''; clearRecommendations()
  try { const r = await fetch('/api/attractions?' + new URLSearchParams({region: region.value})); if (!r.ok) throw Error(); attractions.value = await r.json() }
  catch { attractions.value = []; error.value = '景点加载失败，请检查后端是否运行。' }
}
async function recommend() {
  const version = ++recommendationVersion
  loading.value = true; error.value = ''
  try { const r = await fetch('/api/recommendations', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({region:region.value, themes: theme.value ? [theme.value] : [], interestIds: interestIds.value, profileInterests: useProfile.value ? Object.fromEntries(profile.value.map(x => [x.id,x.weight])) : {}, favoriteIds: useProfile.value ? behavior.value.favorites : [], historyIds: useProfile.value ? behavior.value.history : [], limit:5})}); if (!r.ok) throw Error(); const data = await r.json(); exposureError.value=''; if (version === recommendationVersion) { const shouldRecord=collectExposures.value && data.length>0; exposureId.value=null; exposurePending.value=shouldRecord; recommendations.value=data; searched.value=true; await nextTick(); if (shouldRecord && version === recommendationVersion) { try { const linked=await registerExposure(data); if(version === recommendationVersion) exposureId.value=linked } catch { if(version === recommendationVersion) exposureError.value='推荐展示记录保存失败；仍可使用推荐，评分不会关联本次展示。' } finally { if(version === recommendationVersion) exposurePending.value=false } } } }
  catch { if (version === recommendationVersion) { recommendations.value = []; error.value = '推荐请求失败，请稍后重试。' } }
  finally { if (version === recommendationVersion) loading.value = false }
}
const relationNames = { LOCATED_IN: '位于', RELATED_TO_EVENT: '关联事件', RELATED_TO_PERSON: '关联人物', PARTICIPATED_IN: '参与', RELATED_TO_WORK: '关联著作', BELONGS_TO_THEME: '属于主题' }
let storyRequest = 0
async function showStory(id) {
  const request = ++storyRequest; story.value = null; storyError.value = ''; storyLoading.value = true
  try { const response = await fetch(`/api/attractions/${id}/story`); if (!response.ok) throw Error(); const data = await response.json(); if (request !== storyRequest) return; story.value = data; viewed(id); await nextTick(); document.getElementById('story-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
  catch { if (request === storyRequest) storyError.value = '故事加载失败，请稍后重试。' }
  finally { if (request === storyRequest) storyLoading.value = false }
}
function closeStory() { storyRequest++; story.value = null; storyLoading.value = false }
async function showGraph(id) {
  graphError.value = ''; graphLoading.value = true; graph.value = null
  graphTitle.value = id == null ? '陕北红色文旅知识图谱' : `${attractions.value.find(a => a.id === id)?.name || '景点'} · 知识关联`
  try { const r = await fetch(id == null ? '/api/graph' : `/api/graph/attractions/${id}`); if (!r.ok) throw Error(); graph.value = await r.json(); if (id != null) viewed(id); await nextTick(); document.getElementById('graph-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
  catch { graphError.value = '知识关联加载失败，请稍后重试。' }
  finally { graphLoading.value = false }
}
async function loadInterests() {
  try { const r = await fetch('/api/graph'); if (!r.ok) throw Error(); const data = await r.json(); interestGraph.value = data; interestNodes.value = data.nodes.filter(n => ['人物', '历史事件', '著作', '主题'].includes(n.label)).sort((a,b) => a.name.localeCompare(b.name, 'zh-CN')); interestsError.value = false }
  catch { interestNodes.value = []; interestsError.value = true }
}
function chooseInterest(id) { if (!interestIds.value.includes(id) && interestIds.value.length < 10) interestIds.value = [...interestIds.value, id]; document.getElementById('recommend')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
function contentChanged() { load(); loadInterests(); fetch('/api/attractions').then(r => { if (!r.ok) throw Error(); return r.json() }).then(data => { allPlaces.value = data }).catch(() => {}) }
onMounted(() => { if(auth.value) { const session=auth.value; refreshProfile(session).catch(()=>{ if(auth.value?.token===session.token)accountError.value='账号记录加载失败，请刷新页面重试。' }) } load(); loadInterests(); fetch('/api/attractions').then(r => { if (!r.ok) throw Error(); return r.json() }).then(data => { allPlaces.value = data }).catch(() => {}) })
</script>
<template>
  <header class="site-header"><div class="container nav"><a class="brand" href="#top" aria-label="陕北红色文旅首页"><span class="brand-mark">陕</span><span><strong>陕北红色文旅</strong><small>SHANBEI · MEMORY</small></span></a><nav aria-label="页面导航"><a href="#discover">发现陕北</a><a href="#recommend">个性推荐</a><a href="#ask">智能问答</a><span class="nav-note">毕业设计 · 演示版</span></nav></div></header>
  <main id="top">
    <section class="hero"><div class="hero-shade"></div><div class="container hero-content"><div class="hero-kicker"><span></span> 红色记忆 · 山河之间</div><h1>循着历史的足迹<br><em>遇见陕北</em></h1><p>黄土为卷，岁月为墨。<br>从一处处红色印记出发，读懂这片土地的故事。</p><a class="hero-link" href="#recommend">开启探索 <span>↗</span></a><div class="hero-caption">陕北风貌主题插画 · 非真实景点照片</div></div><div class="hero-side">LAND OF MEMORY <span> / </span> NORTHERN SHAANXI</div></section>
    <div class="intro-strip"><div class="container intro-inner"><p><span class="strip-dot"></span> 每一次出发，都是一次与历史的相遇</p><span>探索 · 发现 · 记忆</span></div></div>
    <div class="container main-content"><AccountPanel :session="auth" @signed-in="signedIn" @sign-out="signOut" /><AdminUsers v-if="auth?.role==='ADMIN'" :session="auth" :request="signedFetch" /><AdminContent v-if="auth?.role==='ADMIN'" :request="signedFetch" @changed="contentChanged" /><p v-if="accountError" role="alert" class="error">{{ accountError }}</p>
      <section id="recommend" class="recommend-section"><div class="section-heading"><div><p class="overline">YOUR JOURNEY <span> / 01</span></p><h2>找到属于你的旅程</h2><p class="section-desc">选择目的地与感兴趣的主题，发现适合你的红色文化之旅。</p></div><span class="heading-symbol">旅</span></div>
        <div class="search-panel"><div class="panel-intro"><span class="panel-number">01 / 旅程偏好</span><strong>从这里出发</strong><small>选择你想了解的地方与故事</small></div><div class="controls"><label><span>目的地</span><select v-model="region" @change="load"><option v-for="name in [...new Set(allPlaces.map(p=>p.region))].sort()" :key="name" :value="name">{{name}}</option><option value="">全部地区</option></select></label><label><span>感兴趣的主题</span><select v-model="theme"><option v-for="name in [...new Set(allPlaces.map(p=>p.theme))].sort()" :key="name" :value="name">{{name}}</option><option value="">不限主题</option></select></label><div class="interest-picker"><span id="interest-label">历史人物、事件或著作</span><details><summary aria-labelledby="interest-label">已选 {{ interestIds.length }} 项（最多 10 项）</summary><fieldset :disabled="interestsError"><legend>选择知识兴趣</legend><label v-for="node in interestNodes" :key="node.id"><input type="checkbox" v-model="interestIds" :value="node.id" :disabled="interestIds.length >= 10 && !interestIds.includes(node.id)"><span>{{ node.name }} · {{ node.label }}</span></label></fieldset></details></div><button @click="recommend" :disabled="loading">{{ loading ? '正在推荐…' : '为我推荐' }} <span>↗</span></button></div></div>
        <div class="profile-panel"><div class="profile-heading"><div><p class="overline">YOUR INTERESTS</p><h3>我的兴趣画像</h3><p>收藏和浏览会影响推荐；登录后记录保存在账号中，未登录时保存在本机。推荐时使用所选兴趣及相关景点记录。</p></div><label class="profile-switch"><input type="checkbox" v-model="useProfile"> 推荐时使用画像和去重</label></div><p v-if="storageError" role="alert" class="error">浏览器存储不可用，当前记录可能无法保留。</p><div class="profile-columns"><div><strong>收藏（{{ behavior.favorites.length }}）</strong><button v-if="behavior.favorites.length" @click="clearBehavior('favorites')">清空收藏</button><ul><li v-for="id in behavior.favorites" :key="id">{{ placeNames.get(id) || ('景点 ' + id) }} <button @click="favorite(id)">取消收藏</button></li></ul><small v-if="!behavior.favorites.length">暂未收藏景点</small></div><div><strong>最近浏览（{{ behavior.history.length }}）</strong><button v-if="behavior.history.length" @click="clearBehavior('history')">清空浏览</button><ul><li v-for="id in behavior.history" :key="id">{{ placeNames.get(id) || ('景点 ' + id) }}</li></ul><small v-if="!behavior.history.length">打开景点故事或图谱后记录</small></div><div><strong>推导出的知识兴趣</strong><ul><li v-for="item in profile" :key="item.id">{{ item.name }}</li></ul><small v-if="!profile.length">收藏或浏览景点后生成</small></div></div></div>
        <label class="exposure-consent"><input type="checkbox" v-model="collectExposures" :disabled="!auth"> 同意保存每次推荐的景点列表，用于评估（登录后可开启）</label><p v-if="exposureError" role="alert" class="error">{{ exposureError }}</p><p v-if="error" role="alert" class="error">{{ error }}</p><p v-if="interestsError" role="alert" class="error">知识兴趣加载失败，仍可按地区和主题推荐。</p>
        <div v-if="searched" class="results"><div class="result-title"><div><p class="overline">CURATED FOR YOU</p><h3>为你推荐</h3></div><span>根据你的选择生成</span></div><div v-if="recommendations.length" class="result-grid"><article v-for="item in recommendations" :key="item.id" class="result-card"><span class="tag">{{ item.theme }}</span><h4>{{ item.name }}</h4><p class="location">{{ item.region }} · 陕北</p><button class="feedback-link" :disabled="exposurePending" @click="openFeedback(item,true)">评分与反馈 ↗</button><p class="reason">{{ item.reason }}</p><div v-if="item.profileMatches.length" class="profile-result">根据你的兴趣<div v-for="match in item.profileMatches" :key="match.id" class="recommend-evidence"><span>画像关联：{{ match.path.join(' → ' ) }}</span><div><a v-for="(url, index) in match.sources" :key="index" :href="url" target="_blank" rel="noopener noreferrer">依据{{ index + 1 }} ↗</a></div></div></div><div v-for="match in item.graphMatches" :key="match.id" class="recommend-evidence"><span>关联路径：{{ match.path.join(' → ') }}</span><small>关系类型：{{ match.relationTypes.map(type => relationNames[type] || type).join(' → ') }}</small><div><a v-for="(url, index) in match.sources" :key="index" :href="url" target="_blank" rel="noopener noreferrer">依据{{ index + 1 }} ↗</a></div></div></article></div><p v-else class="empty">暂无符合条件的推荐，请尝试其他地区。</p></div>
      </section>
      <section id="discover" class="discover-section"><div class="section-heading"><div><p class="overline">EXPLORE SHANBEI <span> / 02</span></p><h2>在陕北，发现故事</h2><p class="section-desc">山川有迹，历史有声。以旅行为线索，重新认识红色文化。</p></div><div class="section-aside"><span>SELECTED PLACES</span><strong>地方记忆</strong></div></div>
        <button class="all-graph-button" @click="showGraph(null)">查看完整知识图谱 <span>↗</span></button><div class="demo-note"><span>资料说明</span> 景点内容参考公开资料；配图为 AI 主题插画，并非实景照片。开放信息请以景区官方发布为准。</div>
        <div v-if="attractions.length" class="place-grid"><article v-for="(item, index) in attractions" :key="item.id" class="place-card"><div class="place-image" :style="{ backgroundImage: `linear-gradient(0deg,rgba(49,34,27,.3),transparent 55%), url(${item.image})` }"><span class="place-index">{{ String(index + 1).padStart(2, '0') }}</span><span class="image-label">AI 主题插画</span></div><div class="place-body"><div class="place-meta"><span>{{ item.region }} · 陕北</span><span>{{ item.theme }}</span></div><h3>{{ item.name }}</h3><p>{{ item.description }}</p><a class="source-link" :href="item.sourceUrl" target="_blank" rel="noopener noreferrer">资料来源：{{ item.sourceName }} ↗</a><div class="place-bottom place-actions"><button class="favorite-button" @click="favorite(item.id)" :aria-pressed="behavior.favorites.includes(item.id)">{{ behavior.favorites.includes(item.id) ? '♥ 已收藏' : '♡ 收藏景点' }}</button><button class="feedback-link" @click="openFeedback(item)">评分与反馈 ↗</button><button class="story-button" @click="showStory(item.id)">阅读历史故事 <span>↗</span></button><button class="graph-button" @click="showGraph(item.id)">查看知识关联 <span>↗</span></button></div></div></article></div><p v-else-if="!error" class="empty">当前地区暂无已收录景点。</p>
        <p v-if="storyLoading" class="empty" role="status">正在加载历史故事…</p><p v-if="storyError" class="error" role="alert">{{ storyError }}</p>
        <article v-if="story" id="story-results" class="story-panel" aria-live="polite"><div class="story-heading"><div><p class="overline">THE STORY BEHIND</p><h3>{{ story.title }} · 历史故事</h3><p>从历史背景、关键经过到后续记忆，按资料梳理。</p></div><button class="graph-close" @click="closeStory" aria-label="关闭历史故事">×</button></div><section v-for="(section, index) in story.sections" :key="section.title" class="story-section"><div class="story-number">{{ String(index + 1).padStart(2, '0') }}</div><div><h4>{{ section.title }}</h4><p>{{ section.content }}</p><div class="story-sources"><a v-for="source in section.sources" :key="source.url" :href="source.url" target="_blank" rel="noopener noreferrer">资料来源：{{ source.name }} ↗</a></div></div></section><p class="story-note">故事依据公开资料整理；如需进一步研究，请查阅所列原始资料。</p></article>
        <p v-if="graphLoading" class="empty" role="status">正在加载知识关联…</p><p v-if="graphError" class="error" role="alert">{{ graphError }}</p>
        <div v-if="graph" id="graph-results" class="graph-panel" aria-live="polite"><div class="graph-heading"><div><p class="overline">KNOWLEDGE GRAPH</p><h3>{{ graphTitle }}</h3></div><button class="graph-close" @click="graph = null" aria-label="关闭知识关联">×</button></div><GraphView :graph="graph" :relation-names="relationNames" @choose-interest="chooseInterest" /></div>
      </section>
      <EvaluationPanel :target="feedbackTarget" :user-id="feedbackUserId" :logged-in="!!auth" :request="signedFetch" @close="feedbackTarget = null" />
      <ChatPanel />
      <section class="closing"><span class="closing-mark">“</span><div><p>山河为证，记忆常新。</p><span>让每一段旅程，都有可以追寻的故事。</span></div><span class="closing-en">A JOURNEY THROUGH<br>HISTORY & MEMORY</span></section>
    </div>
  </main>
  <footer><div class="container footer-inner"><div><strong>陕北红色文旅</strong><p>在行走中，触摸历史的温度。</p></div><div class="footer-right">毕业设计项目<br><span>景点内容附资料来源</span></div></div></footer>
</template>



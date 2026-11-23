<script setup>
import { ref, onMounted, nextTick, defineAsyncComponent, watch } from 'vue'
const GraphView = defineAsyncComponent(() => import('./GraphView.vue'))
const ChatPanel = defineAsyncComponent(() => import('./ChatPanel.vue'))
const attractions = ref([]), recommendations = ref([]), region = ref('延安'), theme = ref('革命旧址'), error = ref(''), loading = ref(false), searched = ref(false), graph = ref(null), graphError = ref(''), graphLoading = ref(false), graphTitle = ref(''), interestId = ref(''), interestNodes = ref([]), interestsError = ref(false), story = ref(null), storyLoading = ref(false), storyError = ref('')
let recommendationVersion = 0
function clearRecommendations() { recommendationVersion++; recommendations.value = []; searched.value = false; loading.value = false }
watch([theme, interestId], clearRecommendations)
async function load() {
  error.value = ''; graph.value = null; closeStory(); storyError.value = ''; clearRecommendations()
  try { const r = await fetch('/api/attractions?' + new URLSearchParams({region: region.value})); if (!r.ok) throw Error(); attractions.value = await r.json() }
  catch { attractions.value = []; error.value = '景点加载失败，请检查后端是否运行。' }
}
async function recommend() {
  const version = ++recommendationVersion
  loading.value = true; error.value = ''
  try { const r = await fetch('/api/recommendations', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({region:region.value, themes: theme.value ? [theme.value] : [], interestIds: interestId.value ? [interestId.value] : [], limit:5})}); if (!r.ok) throw Error(); const data = await r.json(); if (version === recommendationVersion) { recommendations.value = data; searched.value = true } }
  catch { if (version === recommendationVersion) { recommendations.value = []; error.value = '推荐请求失败，请稍后重试。' } }
  finally { if (version === recommendationVersion) loading.value = false }
}
const relationNames = { LOCATED_IN: '位于', RELATED_TO_EVENT: '关联事件', RELATED_TO_PERSON: '关联人物', PARTICIPATED_IN: '参与', RELATED_TO_WORK: '关联著作', BELONGS_TO_THEME: '属于主题' }
let storyRequest = 0
async function showStory(id) {
  const request = ++storyRequest; story.value = null; storyError.value = ''; storyLoading.value = true
  try { const response = await fetch(`/api/attractions/${id}/story`); if (!response.ok) throw Error(); const data = await response.json(); if (request !== storyRequest) return; story.value = data; await nextTick(); document.getElementById('story-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
  catch { if (request === storyRequest) storyError.value = '故事加载失败，请稍后重试。' }
  finally { if (request === storyRequest) storyLoading.value = false }
}
function closeStory() { storyRequest++; story.value = null; storyLoading.value = false }
async function showGraph(id) {
  graphError.value = ''; graphLoading.value = true; graph.value = null
  graphTitle.value = id == null ? '陕北红色文旅知识图谱' : `${attractions.value.find(a => a.id === id)?.name || '景点'} · 知识关联`
  try { const r = await fetch(id == null ? '/api/graph' : `/api/graph/attractions/${id}`); if (!r.ok) throw Error(); graph.value = await r.json(); await nextTick(); document.getElementById('graph-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
  catch { graphError.value = '知识关联加载失败，请稍后重试。' }
  finally { graphLoading.value = false }
}
async function loadInterests() {
  try { const r = await fetch('/api/graph'); if (!r.ok) throw Error(); const data = await r.json(); interestNodes.value = data.nodes.filter(n => ['人物', '历史事件', '著作', '主题'].includes(n.label)).sort((a,b) => a.name.localeCompare(b.name, 'zh-CN')); interestsError.value = false }
  catch { interestNodes.value = []; interestsError.value = true }
}
function chooseInterest(id) { interestId.value = id; document.getElementById('recommend')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
onMounted(() => { load(); loadInterests() })
</script>
<template>
  <header class="site-header"><div class="container nav"><a class="brand" href="#top" aria-label="陕北红色文旅首页"><span class="brand-mark">陕</span><span><strong>陕北红色文旅</strong><small>SHANBEI · MEMORY</small></span></a><nav aria-label="页面导航"><a href="#discover">发现陕北</a><a href="#recommend">个性推荐</a><a href="#ask">智能问答</a><span class="nav-note">毕业设计 · 演示版</span></nav></div></header>
  <main id="top">
    <section class="hero"><div class="hero-shade"></div><div class="container hero-content"><div class="hero-kicker"><span></span> 红色记忆 · 山河之间</div><h1>循着历史的足迹<br><em>遇见陕北</em></h1><p>黄土为卷，岁月为墨。<br>从一处处红色印记出发，读懂这片土地的故事。</p><a class="hero-link" href="#recommend">开启探索 <span>↗</span></a><div class="hero-caption">陕北风貌主题插画 · 非真实景点照片</div></div><div class="hero-side">LAND OF MEMORY <span> / </span> NORTHERN SHAANXI</div></section>
    <div class="intro-strip"><div class="container intro-inner"><p><span class="strip-dot"></span> 每一次出发，都是一次与历史的相遇</p><span>探索 · 发现 · 记忆</span></div></div>
    <div class="container main-content">
      <section id="recommend" class="recommend-section"><div class="section-heading"><div><p class="overline">YOUR JOURNEY <span> / 01</span></p><h2>找到属于你的旅程</h2><p class="section-desc">选择目的地与感兴趣的主题，发现适合你的红色文化之旅。</p></div><span class="heading-symbol">旅</span></div>
        <div class="search-panel"><div class="panel-intro"><span class="panel-number">01 / 旅程偏好</span><strong>从这里出发</strong><small>选择你想了解的地方与故事</small></div><div class="controls"><label><span>目的地</span><select v-model="region" @change="load"><option>延安</option><option>榆林</option><option value="">全部地区</option></select></label><label><span>感兴趣的主题</span><select v-model="theme"><option>革命旧址</option><option>红色地标</option><option>纪念馆</option><option value="">不限主题</option></select></label><label><span>历史人物、事件或著作</span><select v-model="interestId" :disabled="interestsError"><option value="">不限知识兴趣</option><option v-for="node in interestNodes" :key="node.id" :value="node.id">{{ node.name }} · {{ node.label }}</option></select></label><button @click="recommend" :disabled="loading">{{ loading ? '正在推荐…' : '为我推荐' }} <span>↗</span></button></div></div>
        <p v-if="error" role="alert" class="error">{{ error }}</p><p v-if="interestsError" role="alert" class="error">知识兴趣加载失败，仍可按地区和主题推荐。</p>
        <div v-if="searched" class="results"><div class="result-title"><div><p class="overline">CURATED FOR YOU</p><h3>为你推荐</h3></div><span>根据你的选择生成</span></div><div v-if="recommendations.length" class="result-grid"><article v-for="item in recommendations" :key="item.id" class="result-card"><span class="tag">{{ item.theme }}</span><h4>{{ item.name }}</h4><p class="location">{{ item.region }} · 陕北</p><p class="reason">{{ item.reason }}</p><div v-for="match in item.graphMatches" :key="match.id" class="recommend-evidence"><span>关联路径：{{ match.path.join(' → ') }}（{{ match.distance }} 跳）</span><div><a v-for="(url, index) in match.sources" :key="index" :href="url" target="_blank" rel="noopener noreferrer">依据{{ index + 1 }} ↗</a></div></div></article></div><p v-else class="empty">暂无符合条件的推荐，请尝试其他地区。</p></div>
      </section>
      <section id="discover" class="discover-section"><div class="section-heading"><div><p class="overline">EXPLORE SHANBEI <span> / 02</span></p><h2>在陕北，发现故事</h2><p class="section-desc">山川有迹，历史有声。以旅行为线索，重新认识红色文化。</p></div><div class="section-aside"><span>SELECTED PLACES</span><strong>地方记忆</strong></div></div>
        <button class="all-graph-button" @click="showGraph(null)">查看完整知识图谱 <span>↗</span></button><div class="demo-note"><span>资料说明</span> 景点内容参考公开资料；配图为 AI 主题插画，并非实景照片。开放信息请以景区官方发布为准。</div>
        <div v-if="attractions.length" class="place-grid"><article v-for="(item, index) in attractions" :key="item.id" class="place-card"><div class="place-image" :style="{ backgroundImage: `linear-gradient(0deg,rgba(49,34,27,.3),transparent 55%), url(${item.image})` }"><span class="place-index">0{{ index + 1 }}</span><span class="image-label">AI 主题插画</span></div><div class="place-body"><div class="place-meta"><span>{{ item.region }} · 陕北</span><span>{{ item.theme }}</span></div><h3>{{ item.name }}</h3><p>{{ item.description }}</p><a class="source-link" :href="item.sourceUrl" target="_blank" rel="noopener noreferrer">资料来源：{{ item.sourceName }} ↗</a><div class="place-bottom place-actions"><button class="story-button" @click="showStory(item.id)">阅读历史故事 <span>↗</span></button><button class="graph-button" @click="showGraph(item.id)">查看知识关联 <span>↗</span></button></div></div></article></div><p v-else-if="!error" class="empty">当前地区暂无已收录景点。</p>
        <p v-if="storyLoading" class="empty" role="status">正在加载历史故事…</p><p v-if="storyError" class="error" role="alert">{{ storyError }}</p>
        <article v-if="story" id="story-results" class="story-panel" aria-live="polite"><div class="story-heading"><div><p class="overline">THE STORY BEHIND</p><h3>{{ story.title }} · 历史故事</h3><p>从历史背景、关键经过到后续记忆，按资料梳理。</p></div><button class="graph-close" @click="closeStory" aria-label="关闭历史故事">×</button></div><section v-for="(section, index) in story.sections" :key="section.title" class="story-section"><div class="story-number">0{{ index + 1 }}</div><div><h4>{{ section.title }}</h4><p>{{ section.content }}</p><div class="story-sources"><a v-for="source in section.sources" :key="source.url" :href="source.url" target="_blank" rel="noopener noreferrer">资料来源：{{ source.name }} ↗</a></div></div></section><p class="story-note">故事依据公开资料整理；如需进一步研究，请查阅所列原始资料。</p></article>
        <p v-if="graphLoading" class="empty" role="status">正在加载知识关联…</p><p v-if="graphError" class="error" role="alert">{{ graphError }}</p>
        <div v-if="graph" id="graph-results" class="graph-panel" aria-live="polite"><div class="graph-heading"><div><p class="overline">KNOWLEDGE GRAPH</p><h3>{{ graphTitle }}</h3></div><button class="graph-close" @click="graph = null" aria-label="关闭知识关联">×</button></div><GraphView :graph="graph" :relation-names="relationNames" @choose-interest="chooseInterest" /></div>
      </section>
      <ChatPanel />
      <section class="closing"><span class="closing-mark">“</span><div><p>山河为证，记忆常新。</p><span>让每一段旅程，都有可以追寻的故事。</span></div><span class="closing-en">A JOURNEY THROUGH<br>HISTORY & MEMORY</span></section>
    </div>
  </main>
  <footer><div class="container footer-inner"><div><strong>陕北红色文旅</strong><p>在行走中，触摸历史的温度。</p></div><div class="footer-right">毕业设计项目<br><span>景点内容附资料来源</span></div></div></footer>
</template>



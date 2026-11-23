<script setup>
import { ref, onMounted, nextTick } from 'vue'
const attractions = ref([]), recommendations = ref([]), region = ref('延安'), theme = ref('革命旧址'), error = ref(''), loading = ref(false), searched = ref(false), graph = ref(null), graphError = ref(''), graphLoading = ref(false), graphTitle = ref('')
async function load() {
  error.value = ''; graph.value = null; recommendations.value = []; searched.value = false
  try { const r = await fetch('/api/attractions?' + new URLSearchParams({region: region.value})); if (!r.ok) throw Error(); attractions.value = await r.json() }
  catch { attractions.value = []; error.value = '景点加载失败，请检查后端是否运行。' }
}
async function recommend() {
  loading.value = true; error.value = ''
  try { const r = await fetch('/api/recommendations', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({region:region.value, themes: theme.value ? [theme.value] : [], limit:5})}); if (!r.ok) throw Error(); recommendations.value = await r.json(); searched.value = true }
  catch { recommendations.value = []; error.value = '推荐请求失败，请稍后重试。' }
  finally { loading.value = false }
}
const relationNames = { LOCATED_IN: '位于', RELATED_TO_EVENT: '关联事件', RELATED_TO_PERSON: '关联人物', PARTICIPATED_IN: '参与', RELATED_TO_WORK: '关联著作', BELONGS_TO_THEME: '属于主题' }
async function showGraph(id) {
  graphError.value = ''; graphLoading.value = true; graph.value = null
  graphTitle.value = id == null ? '陕北红色文旅知识图谱' : `${attractions.value.find(a => a.id === id)?.name || '景点'} · 知识关联`
  try { const r = await fetch(id == null ? '/api/graph' : `/api/graph/attractions/${id}`); if (!r.ok) throw Error(); graph.value = await r.json(); await nextTick(); document.getElementById('graph-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
  catch { graphError.value = '知识关联加载失败，请稍后重试。' }
  finally { graphLoading.value = false }
}
onMounted(load)
</script>
<template>
  <header class="site-header"><div class="container nav"><a class="brand" href="#top" aria-label="陕北红色文旅首页"><span class="brand-mark">陕</span><span><strong>陕北红色文旅</strong><small>SHANBEI · MEMORY</small></span></a><nav aria-label="页面导航"><a href="#discover">发现陕北</a><a href="#recommend">个性推荐</a><span class="nav-note">毕业设计 · 演示版</span></nav></div></header>
  <main id="top">
    <section class="hero"><div class="hero-shade"></div><div class="container hero-content"><div class="hero-kicker"><span></span> 红色记忆 · 山河之间</div><h1>循着历史的足迹<br><em>遇见陕北</em></h1><p>黄土为卷，岁月为墨。<br>从一处处红色印记出发，读懂这片土地的故事。</p><a class="hero-link" href="#recommend">开启探索 <span>↗</span></a><div class="hero-caption">陕北风貌主题插画 · 非真实景点照片</div></div><div class="hero-side">LAND OF MEMORY <span> / </span> NORTHERN SHAANXI</div></section>
    <div class="intro-strip"><div class="container intro-inner"><p><span class="strip-dot"></span> 每一次出发，都是一次与历史的相遇</p><span>探索 · 发现 · 记忆</span></div></div>
    <div class="container main-content">
      <section id="recommend" class="recommend-section"><div class="section-heading"><div><p class="overline">YOUR JOURNEY <span> / 01</span></p><h2>找到属于你的旅程</h2><p class="section-desc">选择目的地与感兴趣的主题，发现适合你的红色文化之旅。</p></div><span class="heading-symbol">旅</span></div>
        <div class="search-panel"><div class="panel-intro"><span class="panel-number">01 / 旅程偏好</span><strong>从这里出发</strong><small>选择你想了解的地方与故事</small></div><div class="controls"><label><span>目的地</span><select v-model="region" @change="load"><option>延安</option><option>榆林</option><option value="">全部地区</option></select></label><label><span>感兴趣的主题</span><select v-model="theme"><option>革命旧址</option><option>红色地标</option><option>纪念馆</option><option value="">不限主题</option></select></label><button @click="recommend" :disabled="loading">{{ loading ? '正在推荐…' : '为我推荐' }} <span>↗</span></button></div></div>
        <p v-if="error" role="alert" class="error">{{ error }}</p>
        <div v-if="searched" class="results"><div class="result-title"><div><p class="overline">CURATED FOR YOU</p><h3>为你推荐</h3></div><span>根据你的选择生成</span></div><div v-if="recommendations.length" class="result-grid"><article v-for="item in recommendations" :key="item.id" class="result-card"><span class="tag">{{ item.theme }}</span><h4>{{ item.name }}</h4><p class="location">{{ item.region }} · 陕北</p><p class="reason">{{ item.reason }}</p></article></div><p v-else class="empty">暂无符合条件的推荐，请尝试其他地区。</p></div>
      </section>
      <section id="discover" class="discover-section"><div class="section-heading"><div><p class="overline">EXPLORE SHANBEI <span> / 02</span></p><h2>在陕北，发现故事</h2><p class="section-desc">山川有迹，历史有声。以旅行为线索，重新认识红色文化。</p></div><div class="section-aside"><span>SELECTED PLACES</span><strong>地方记忆</strong></div></div>
        <button class="all-graph-button" @click="showGraph(null)">查看完整知识图谱 <span>↗</span></button><div class="demo-note"><span>资料说明</span> 景点内容参考公开资料；配图为 AI 主题插画，并非实景照片。开放信息请以景区官方发布为准。</div>
        <div v-if="attractions.length" class="place-grid"><article v-for="(item, index) in attractions" :key="item.id" class="place-card"><div class="place-image" :style="{ backgroundImage: `linear-gradient(0deg,rgba(49,34,27,.3),transparent 55%), url(${item.image})` }"><span class="place-index">0{{ index + 1 }}</span><span class="image-label">AI 主题插画</span></div><div class="place-body"><div class="place-meta"><span>{{ item.region }} · 陕北</span><span>{{ item.theme }}</span></div><h3>{{ item.name }}</h3><p>{{ item.description }}</p><a class="source-link" :href="item.sourceUrl" target="_blank" rel="noopener noreferrer">资料来源：{{ item.sourceName }} ↗</a><div class="place-bottom"><button class="graph-button" @click="showGraph(item.id)">查看知识关联 <span>↗</span></button></div></div></article></div><p v-else-if="!error" class="empty">当前地区暂无已收录景点。</p>
        <p v-if="graphLoading" class="empty" role="status">正在加载知识关联…</p><p v-if="graphError" class="error" role="alert">{{ graphError }}</p>
        <div v-if="graph" id="graph-results" class="graph-panel" aria-live="polite"><div class="graph-heading"><div><p class="overline">KNOWLEDGE GRAPH</p><h3>{{ graphTitle }}</h3></div><button class="graph-close" @click="graph = null" aria-label="关闭知识关联">×</button></div><p class="graph-caption">以下关联来自 Neo4j 知识图谱，点击来源可查阅依据。</p><div class="graph-relations"><div v-for="edge in graph.edges" :key="edge.source + edge.type + edge.target" class="graph-relation"><span class="graph-node main-node">{{ graph.nodes.find(n => n.id === edge.source)?.name }}</span><span class="graph-arrow">{{ relationNames[edge.type] || edge.type }} →</span><span class="graph-node">{{ graph.nodes.find(n => n.id === edge.target)?.name }} <small>· {{ graph.nodes.find(n => n.id === edge.target)?.label }}</small></span><a :href="edge.sourceUrl" target="_blank" rel="noopener noreferrer">查看来源 ↗</a></div></div></div>
      </section>
      <section class="closing"><span class="closing-mark">“</span><div><p>山河为证，记忆常新。</p><span>让每一段旅程，都有可以追寻的故事。</span></div><span class="closing-en">A JOURNEY THROUGH<br>HISTORY & MEMORY</span></section>
    </div>
  </main>
  <footer><div class="container footer-inner"><div><strong>陕北红色文旅</strong><p>在行走中，触摸历史的温度。</p></div><div class="footer-right">毕业设计项目<br><span>景点内容附资料来源</span></div></div></footer>
</template>


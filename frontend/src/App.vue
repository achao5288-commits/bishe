<script setup>
import { ref, onMounted } from 'vue'
const attractions = ref([]), recommendations = ref([]), region = ref('延安'), theme = ref('革命旧址'), error = ref(''), loading = ref(false)
async function load() {
  error.value = ''
  try { const r = await fetch('/api/attractions?' + new URLSearchParams({region: region.value})); if (!r.ok) throw Error(); attractions.value = await r.json() }
  catch { attractions.value = []; error.value = '景点加载失败，请检查后端是否运行。' }
}
async function recommend() {
  loading.value = true; error.value = ''
  try { const r = await fetch('/api/recommendations', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({region:region.value, themes: theme.value ? [theme.value] : [], limit:5})}); if (!r.ok) throw Error(); recommendations.value = await r.json() }
  catch { recommendations.value = []; error.value = '推荐请求失败，请稍后重试。' }
  finally { loading.value = false }
}
onMounted(load)
</script>
<template>
  <header><div class="wrap brand">陕北红色文旅 <span>毕业设计演示</span></div></header>
  <main class="wrap"><section class="hero"><p class="eyebrow">RED TOURISM · DISCOVER</p><h1>循着历史，发现陕北</h1><p>选择感兴趣的地区和主题，探索与之相关的文旅资源。</p><p class="notice">当前为功能演示，景点名称及内容均为虚构数据。</p></section>
    <section class="panel"><h2>选择你的兴趣</h2><div class="controls"><label>地区<select v-model="region" @change="load"><option>延安</option><option>榆林</option><option value="">全部地区</option></select></label><label>主题<select v-model="theme"><option>革命旧址</option><option>纪念馆</option><option value="">不限主题</option></select></label><button @click="recommend" :disabled="loading">{{ loading ? '正在推荐…' : '获取推荐' }}</button></div><p v-if="error" role="alert" class="error">{{ error }}</p></section>
    <section v-if="recommendations.length"><h2>为你推荐</h2><div class="grid"><article v-for="item in recommendations" :key="item.id" class="card"><span class="tag">{{ item.theme }}</span><h3>{{ item.name }}</h3><p>{{ item.region }}</p><p class="reason">{{ item.reason }}</p></article></div></section>
    <section><div class="section-title"><h2>探索景点</h2><span>演示数据</span></div><div class="grid"><article v-for="item in attractions" :key="item.id" class="card"><span class="tag">{{ item.theme }}</span><h3>{{ item.name }}</h3><p>{{ item.region }}</p><p>{{ item.description }}</p></article></div><p v-if="!attractions.length && !error">当前地区暂无景点。</p></section>
  </main><footer class="wrap">陕北红色文旅智能推荐系统 · 开发中</footer>
</template>

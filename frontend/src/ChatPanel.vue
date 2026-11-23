<script setup>
import { onMounted, ref, nextTick } from 'vue'
const question = ref(''), selectedId = ref(''), places = ref([]), messages = ref([]), busy = ref(false), error = ref('')
const examples = [
  { label: '杨家岭的故事', question: '请讲讲杨家岭背后的完整历史故事。' },
  { label: '南泥湾与大生产', question: '南泥湾与大生产运动有什么关系？' },
  { label: '小河会议', question: '小河会议发生了什么？它的历史背景是什么？' }
]
onMounted(async () => { try { const response=await fetch('/api/attractions'); if (response.ok) places.value=await response.json() } catch { /* 提问仍可通过名称识别 */ } })
function chooseExample(value) { question.value=value; selectedId.value=''; error.value='' }
async function send() {
  const text=question.value.trim()
  if (!text) { error.value='请输入你想了解的问题。'; return }
  if (busy.value) return
  busy.value=true; error.value=''; messages.value.push({role:'user',text}); question.value=''
  try {
    const body={question:text}; if(selectedId.value) body.attractionId=Number(selectedId.value)
    const response=await fetch('/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),signal:AbortSignal.timeout(70000)})
    const data=await response.json().catch(()=>({}))
    if (!response.ok) throw new Error(data.message || '问答服务暂时不可用，请稍后重试。')
    messages.value.push({role:'assistant',text:data.answer,status:data.status,sources:data.sources || [],graphCount:(data.graphEdges || []).length,places:data.attractions || [],citationWarning:!!data.citationWarning})
  } catch(e) { messages.value.push({role:'assistant',text:e.name==='TimeoutError'?'回答超时，请稍后重试。':(e.message || '问答服务连接失败，请稍后重试。'),status:'unavailable',sources:[]}) }
  finally { busy.value=false; await nextTick(); document.getElementById('chat-latest')?.scrollIntoView({behavior:'smooth',block:'nearest'}) }
}
</script>
<template>
  <section id="ask" class="chat-section"><div class="section-heading"><div><p class="overline">ASK THE HISTORY <span> / 03</span></p><h2>听一听，背后的故事</h2><p class="section-desc">结合已核验的景点故事与知识图谱，回答你对陕北红色文化的提问。</p></div><span class="heading-symbol">问</span></div>
    <div class="chat-shell"><div class="chat-intro"><div><span class="chat-tag">智能问答</span><h3>从一个问题开始</h3><p>可输入景点名称，或选择景点后询问。回答依据当前资料库，资料不足时会提示。</p></div><span class="chat-intro-mark">✳</span></div>
      <div v-if="!messages.length" class="chat-examples"><span>试着问</span><div><button v-for="item in examples" :key="item.label" @click="chooseExample(item.question)">{{ item.label }} ↗</button></div></div>
      <div v-if="messages.length" class="chat-messages" role="log" aria-label="问答记录" aria-live="polite"><article v-for="(message,index) in messages" :key="index" :class="['chat-message',message.role]"><span class="message-role">{{ message.role==='user'?'你的问题':message.status==='answered'?'资料与图谱辅助回答':message.status==='insufficient_evidence'?'资料不足':'服务提示' }}</span><p>{{ message.text }}</p><p v-if="message.citationWarning" class="citation-warning">回答中有无法核对的引用编号，已标出；请结合原文核实。</p><div v-if="message.sources?.length" class="chat-sources"><span>检索依据 · 请结合原文核对</span><a v-for="source in message.sources" :key="source.url" :href="source.url" target="_blank" rel="noopener noreferrer">[{{ source.id }}] {{ source.title }} ↗</a><small v-if="message.graphCount">包含 {{ message.graphCount }} 条相关图谱关系</small></div></article><div id="chat-latest"></div></div>
      <form class="chat-form" @submit.prevent="send"><label class="chat-place"><span>指定景点 <small>可选</small></span><select v-model="selectedId"><option value="">根据问题自动识别</option><option v-for="place in places" :key="place.id" :value="String(place.id)">{{ place.name }}</option></select></label><label class="chat-input"><span>你的问题</span><textarea v-model="question" maxlength="500" rows="3" placeholder="例如：请完整介绍杨家岭的历史故事"></textarea></label><div class="chat-submit"><span>{{ question.length }} / 500</span><button type="submit" :disabled="busy">{{ busy?'正在查阅资料…':'发送问题' }} <span>↗</span></button></div><p v-if="error" class="error" role="alert">{{ error }}</p></form>
      <p class="chat-disclaimer">回答由大模型生成，可能有误，请查看所列资料核对。问题及检索内容会发送给第三方模型，请勿输入敏感个人信息。每次问题独立处理。</p>
    </div>
  </section>
</template>

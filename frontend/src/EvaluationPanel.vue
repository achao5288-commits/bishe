<script setup>
import { ref, watch } from 'vue'
const props=defineProps({ target:Object, userId:Function })
const emit=defineEmits(['close'])
const rating=ref(5), comment=ref(''), status=ref(''), busy=ref(false), summary=ref(null), summaryError=ref(''), demo=ref(null), demoError=ref('')
watch(()=>props.target,()=>{rating.value=5;comment.value='';status.value=''})
async function submit(){
 if(!props.target || busy.value)return
 busy.value=true;status.value=''
 try {const r=await fetch('/api/evaluation/feedback',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({userId:props.userId(),attractionId:props.target.id,exposureId:props.target.exposureId || null,rating:Number(rating.value),comment:comment.value})});if(!r.ok)throw Error();status.value='评分已保存，谢谢你的反馈。';if(summary.value)await loadSummary()}
 catch{status.value='保存失败，请检查服务端和浏览器存储后重试。'}finally{busy.value=false}
}
async function loadSummary(){summaryError.value='';try{const r=await fetch('/api/evaluation/summary');if(!r.ok)throw Error();summary.value=await r.json()}catch{summaryError.value='评估结果加载失败，请稍后重试。'}}
async function loadDemo(){demoError.value='';try{const r=await fetch('/data/synthetic-report.json');if(!r.ok)throw Error();demo.value=await r.json()}catch{demoError.value='模拟测试结果加载失败，请先运行测试脚本。'}}
async function removeMine(){
 if(!window.confirm('确定删除此浏览器提交的评分和展示记录吗？此操作不可撤销。'))return
 try{const r=await fetch('/api/evaluation/delete-own',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({userId:props.userId()})});if(!r.ok)throw Error();status.value='本浏览器的服务端记录已删除。';await loadSummary()}catch{status.value='删除失败，请稍后重试。'}
}
const percent = value => value == null ? '暂无数据' : (value*100).toFixed(1)+'%'
</script>
<template>
<section id="evaluation" class="evaluation-panel" aria-labelledby="evaluation-title">
 <div class="evaluation-title"><div><p class="overline">YOUR FEEDBACK / EVALUATION</p><h2 id="evaluation-title">评分与推荐评估</h2><p>提交评分会将景点、评分、选填文字和浏览器随机标识保存到服务端。请勿填写个人敏感信息。</p></div><button @click="loadSummary">查看评估结果 ↗</button></div>
 <form v-if="target" @submit.prevent="submit" class="feedback-form"><div class="feedback-form-head"><strong>为「{{ target.name }}」评分</strong><button type="button" @click="emit('close')">关闭</button></div><p>{{ target.exposureId ? '这条评分会关联到你同意记录的本次推荐。' : '这条评分不会关联推荐展示。' }}</p><label>评分<select v-model="rating"><option v-for="n in 5" :key="n" :value="n">{{ n }} 分</option></select></label><label>文字反馈（选填，最多 500 字）<textarea v-model="comment" maxlength="500" rows="3" placeholder="你觉得这个景点或推荐怎么样？"></textarea></label><button :disabled="busy">{{ busy ? '提交中…' : '提交评分' }}</button></form>
 <p v-else class="evaluation-hint">点击推荐结果或景点卡片上的“评分与反馈”开始。</p>
 <p v-if="status" role="status" class="feedback-status">{{ status }}</p>
 <div v-if="summary" class="evaluation-results" aria-live="polite"><strong>真实反馈汇总</strong><div class="evaluation-grid"><div><b>{{ summary.participants }}</b><span>参与浏览器数</span></div><div><b>{{ summary.ratings }}</b><span>评分数</span></div><div><b>{{ summary.exposures }}</b><span>已同意记录的推荐次数</span></div><div><b>{{ summary.judgedPositions }}</b><span>已评分推荐位置</span></div><div><b>{{ summary.averageRating == null ? '暂无数据' : summary.averageRating.toFixed(2) }}</b><span>平均评分（1–5）</span></div><div><b>{{ percent(summary.observedPositiveRate) }}</b><span>已评分推荐的好评率</span></div></div><p>好评为 4–5 分；好评率仅按已评分的推荐位置计算，未评分位置不计入。评分可包含非推荐来源，平均评分为全部评分。数据自选参与，不代表总体效果，也不能证明算法优劣。</p></div>
 <p v-if="summaryError" role="alert" class="error">{{ summaryError }}</p>
 <div class="simulation-panel"><div class="simulation-heading"><div><strong>模拟测试数据（非真实用户）</strong><p>独立于真实反馈，用于演示算法测试与对比。</p></div><button @click="loadDemo">查看模拟测试结果 ↗</button></div><p v-if="demoError" role="alert" class="error">{{ demoError }}</p><div v-if="demo"><p>模拟用户 {{ demo.users }} 人 · 景点 {{ demo.attractions }} 处 · 模拟评分 {{ demo.simulatedRatings }} 条 · 前 {{ demo.k }} 项</p><p>{{ demo.method }} {{ demo.ratingRule }}</p><div class="simulation-table-wrap"><table><thead><tr><th>指标</th><th>仅基础分</th><th>上版重排</th><th>现版重排</th></tr></thead><tbody><tr><th>Precision@3</th><td>{{ percent(demo.mean.before.precision) }}</td><td>{{ percent(demo.mean.previous.precision) }}</td><td>{{ percent(demo.mean.after.precision) }}</td></tr><tr><th>Recall@3</th><td>{{ percent(demo.mean.before.recall) }}</td><td>{{ percent(demo.mean.previous.recall) }}</td><td>{{ percent(demo.mean.after.recall) }}</td></tr><tr><th>NDCG@3</th><td>{{ demo.mean.before.ndcg.toFixed(4) }}</td><td>{{ demo.mean.previous.ndcg.toFixed(4) }}</td><td>{{ demo.mean.after.ndcg.toFixed(4) }}</td></tr><tr><th>历史景点重复比例</th><td>{{ percent(demo.mean.before.repeated) }}</td><td>{{ percent(demo.mean.previous.repeated) }}</td><td>{{ percent(demo.mean.after.repeated) }}</td></tr><tr><th>类型多样性</th><td>{{ percent(demo.mean.before.themeDiversity) }}</td><td>{{ percent(demo.mean.previous.themeDiversity) }}</td><td>{{ percent(demo.mean.after.themeDiversity) }}</td></tr><tr><th>景点覆盖率</th><td>{{ percent(demo.mean.before.coverage) }}</td><td>{{ percent(demo.mean.previous.coverage) }}</td><td>{{ percent(demo.mean.after.coverage) }}</td></tr></tbody></table></div><details><summary>查看模拟用户与推荐结果</summary><div v-for="item in demo.cases" :key="item.id" class="simulation-case"><strong>{{ item.id }} · {{ item.scenario }}</strong><p>仅基础分：{{ item.before.top.map(x => x.name + (x.seen ? '（已看过）' : '')).join('、') }}</p><p>上版重排：{{ item.previous.top.map(x => x.name + (x.seen ? '（已看过）' : '')).join('、') }}</p><p>现版重排：{{ item.after.top.map(x => x.name + (x.seen ? '（已看过）' : '')).join('、') }}</p></div></details><p>这些评分是人为设定的测试标签，不代表真实用户意见，不能作为真实用户实验结论。</p></div></div><button class="delete-feedback" @click="removeMine">删除此浏览器的服务端评分与展示记录</button>
</section>
</template>

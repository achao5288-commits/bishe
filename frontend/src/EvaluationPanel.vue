<script setup>
import { ref, watch } from 'vue'
const props=defineProps({ target:Object, userId:Function, loggedIn:Boolean, request:Function })
const emit=defineEmits(['close'])
const rating=ref(5), comment=ref(''), status=ref(''), busy=ref(false)
watch(()=>props.target,()=>{rating.value=5;comment.value='';status.value=''})
async function submit(){
 if(!props.target || busy.value)return
 if(!props.loggedIn){status.value='请先登录后提交评分。';return}
 busy.value=true;status.value=''
 try {const r=await props.request('/api/evaluation/feedback',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({userId:props.userId(),attractionId:props.target.id,exposureId:props.target.exposureId || null,rating:Number(rating.value),comment:comment.value})});if(!r.ok)throw Error();status.value='评分已保存，谢谢你的反馈。'}
 catch{status.value='保存失败，请检查服务端和浏览器存储后重试。'}finally{busy.value=false}
}
async function removeMine(){
 if(!props.loggedIn){status.value='请先登录后删除账号反馈。';return}
 if(!window.confirm('确定删除此浏览器提交的评分和展示记录吗？此操作不可撤销。'))return
 try{const r=await props.request('/api/evaluation/delete-own',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({userId:props.userId()})});if(!r.ok)throw Error();status.value='本浏览器的服务端记录已删除。'}catch{status.value='删除失败，请稍后重试。'}
}
</script>
<template>
<section id="evaluation" class="evaluation-panel" aria-labelledby="evaluation-title">
 <div class="evaluation-title"><div><p class="overline">YOUR FEEDBACK</p><h2 id="evaluation-title">评分与反馈</h2><p>提交评分会将景点、评分、选填文字和账号标识保存到服务端。请勿填写个人敏感信息。</p></div></div>
 <form v-if="target" @submit.prevent="submit" class="feedback-form"><div class="feedback-form-head"><strong>为「{{ target.name }}」评分</strong><button type="button" @click="emit('close')">关闭</button></div><p>{{ target.exposureId ? '这条评分会关联到你同意记录的本次推荐。' : '这条评分不会关联推荐展示。' }}</p><label>评分<select v-model="rating"><option v-for="n in 5" :key="n" :value="n">{{ n }} 分</option></select></label><label>文字反馈（选填，最多 500 字）<textarea v-model="comment" maxlength="500" rows="3" placeholder="你觉得这个景点或推荐怎么样？"></textarea></label><button :disabled="busy">{{ busy ? '提交中…' : '提交评分' }}</button></form>
 <p v-else class="evaluation-hint">点击推荐结果或景点卡片上的“评分与反馈”开始。</p>
 <p v-if="status" role="status" class="feedback-status">{{ status }}</p>
 <button class="delete-feedback" @click="removeMine">删除当前账号的评分与展示记录</button>
</section>
</template>

<script setup>
import { ref } from 'vue'
const props=defineProps({ session:Object })
const emit=defineEmits(['signed-in','sign-out'])
const mode=ref('login'), username=ref(''), password=ref(''), busy=ref(false), error=ref('')
async function submit(){
 if(busy.value)return;busy.value=true;error.value=''
 try{const r=await fetch(`/api/auth/${mode.value}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:username.value,password:password.value})});if(!r.ok){error.value=r.status===409?'用户名已存在。':r.status===401?'用户名或密码错误。':'登录或注册失败，请检查用户名和密码。';return}const data=await r.json();password.value='';emit('signed-in',data)}catch{error.value='无法连接服务，请检查后端和数据库。'}finally{busy.value=false}
}
</script>
<template>
<section class="account-panel" aria-label="用户账号">
 <div v-if="session" class="account-active"><span>已登录：{{ session.username }} <small v-if="session.role==='ADMIN'">管理员</small></span><button @click="emit('sign-out')">退出登录</button></div>
 <details v-else><summary>登录 / 注册账号</summary><form @submit.prevent="submit"><div class="account-mode"><button type="button" :aria-pressed="mode==='login'" @click="mode='login';error=''">登录</button><button type="button" :aria-pressed="mode==='register'" @click="mode='register';error=''">注册</button></div><label>用户名<input v-model.trim="username" autocomplete="username" required pattern="[A-Za-z0-9_]{3,32}" placeholder="3–32 位字母、数字或下划线"></label><label>密码<input v-model="password" :autocomplete="mode==='register'?'new-password':'current-password'" type="password" required minlength="8" maxlength="72" placeholder="至少 8 个字符"></label><button :disabled="busy">{{ busy ? '处理中…' : mode==='login' ? '登录' : '注册并登录' }}</button><p v-if="error" role="alert" class="error">{{ error }}</p></form></details>
 <p>登录后使用账号的收藏和浏览记录；本机原有记录仍保留，不会自动覆盖账号数据。</p>
</section>
</template>

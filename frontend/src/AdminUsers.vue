<script setup>
import {ref} from 'vue'
const props=defineProps({request:Function,session:Object})
const users=ref(null), error=ref(''), loading=ref(false), cacheStatus=ref('')
async function load(){loading.value=true;error.value='';try{const r=await props.request('/api/admin/users');if(!r.ok)throw Error();users.value=await r.json()}catch{error.value='用户列表加载失败，请确认管理员权限。'}finally{loading.value=false}}
async function invalidateCache(){cacheStatus.value='';try{const r=await props.request('/api/admin/cache/invalidate',{method:'POST'});if(!r.ok)throw Error();cacheStatus.value='本服务实例缓存已刷新。'}catch{cacheStatus.value='缓存刷新失败，请重试。'}}
async function change(user,role){if(user.id===props.session.userId && role==='USER')return;error.value='';try{const r=await props.request(`/api/admin/users/${user.id}/role`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({role})});if(!r.ok)throw Error();await load()}catch{error.value='权限修改失败，请重试。'}}
</script>
<template>
<details class="admin-users" @toggle="$event.target.open && !users && load()"><summary>管理用户权限</summary><p>管理员可以调整其他账号的角色；不能撤销自己的管理员权限。</p><p v-if="loading">加载中…</p><p v-if="error" role="alert" class="error">{{ error }}</p><ul v-if="users"><li v-for="user in users" :key="user.id"><span>{{ user.username }} · {{ user.role==='ADMIN'?'管理员':'普通用户' }}<small v-if="user.protectedUser">（初始管理员）</small></span><button v-if="user.id!==session.userId && !user.protectedUser" @click="change(user,user.role==='ADMIN'?'USER':'ADMIN')">{{ user.role==='ADMIN'?'设为普通用户':'设为管理员' }}</button></li></ul><button @click="invalidateCache">刷新景点、图谱和推荐缓存</button><p v-if="cacheStatus" role="status">{{ cacheStatus }}</p></details>
</template>

<template>
  <router-view v-slot="{ Component }">
    <transition name="page" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
  <Toast />
</template>
<script setup lang="ts">
import Toast from '@/components/Toast.vue'
</script>
<style>
.page-enter-active,.page-leave-active{transition:opacity .22s ease,transform .22s cubic-bezier(.2,.8,.2,1)}
.page-enter-from{opacity:0;transform:translateY(8px)}
.page-leave-to{opacity:0;transform:translateY(-4px)}
@media(prefers-reduced-motion:reduce){*,*::before,*::after{animation-duration:.01ms!important;animation-iteration-count:1!important;transition-duration:.01ms!important}}
</style>

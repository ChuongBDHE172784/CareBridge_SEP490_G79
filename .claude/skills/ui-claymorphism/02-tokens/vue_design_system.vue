<!-- 
  MOMCARE CLAY DESIGN SYSTEM - Vue 3 Implementation
  Depends on Tailwind CSS and Lucide Icons
-->

<template>
  <div 
    :class="[
      'bg-card rounded-[32px] p-8 border border-surface/50 transition-all duration-300',
      floating ? 'shadow-floating' : 'shadow-soft'
    ]"
  >
    <slot />
  </div>
</template>

<script setup>
import { defineProps } from 'vue'

defineProps({
  floating: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
/* Individual clay effects if needed outside Tailwind */
.bg-card {
  background-color: #FFFFFF;
}
.shadow-soft {
  box-shadow: 0 12px 32px rgba(90, 70, 63, 0.06);
}
.shadow-floating {
  box-shadow: 0 24px 64px rgba(90, 70, 63, 0.12);
}
</style>

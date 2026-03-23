<script setup>
defineProps({
  size: {
    type: Number,
    default: 36
  },
  title: {
    type: String,
    default: '校园二手交易系统'
  },
  subtitle: {
    type: String,
    default: 'CAMPUS MARKET'
  },
  showText: {
    type: Boolean,
    default: true
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<template>
  <div class="campus-logo" :class="{ 'campus-logo--compact': compact }" aria-label="校园二手交易系统 logo">
    <svg
      class="campus-logo__icon"
      :width="size"
      :height="size"
      viewBox="0 0 64 64"
      role="img"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="bookGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#ffe6a7" />
          <stop offset="100%" stop-color="#f4c96f" />
        </linearGradient>
      </defs>

      <rect x="5" y="8" width="54" height="48" rx="14" fill="#fff7e6" />
      <path
        d="M32 19c-2.8-2.4-7-3.6-11.8-3.6-4.7 0-8.3.9-10.7 1.9v24.5c2.4-1 6-1.9 10.7-1.9 4.8 0 9 1.2 11.8 3.6V19z"
        fill="url(#bookGradient)"
      />
      <path
        d="M32 19c2.8-2.4 7-3.6 11.8-3.6 4.7 0 8.3.9 10.7 1.9v24.5c-2.4-1-6-1.9-10.7-1.9-4.8 0-9 1.2-11.8 3.6V19z"
        fill="#ffecc0"
      />
      <path d="M32 19v24.5" stroke="#8a5b24" stroke-width="2.5" stroke-linecap="round" />

      <path
        d="M18.5 29.8h18.7"
        stroke="#8a5b24"
        stroke-width="3"
        stroke-linecap="round"
      />
      <path
        d="M34.6 25.7l4.2 4.1-4.2 4.1"
        fill="none"
        stroke="#8a5b24"
        stroke-width="3"
        stroke-linecap="round"
        stroke-linejoin="round"
      />

      <path
        d="M45.5 35.9H26.8"
        stroke="#7d4f1e"
        stroke-width="3"
        stroke-linecap="round"
      />
      <path
        d="M29.4 31.8l-4.2 4.1 4.2 4.1"
        fill="none"
        stroke="#7d4f1e"
        stroke-width="3"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>

    <div v-if="showText" class="campus-logo__text">
      <p class="campus-logo__title">{{ title }}</p>
      <p class="campus-logo__subtitle">{{ subtitle }}</p>
    </div>
  </div>
</template>

<style scoped>
.campus-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.campus-logo--compact {
  gap: 6px;
}

.campus-logo__icon {
  flex-shrink: 0;
  filter: drop-shadow(0 2px 3px rgba(128, 91, 28, 0.14));
}

.campus-logo__text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  line-height: 1.1;
}

.campus-logo__title {
  margin: 0;
  color: #7a5222;
  font-weight: 700;
  font-size: 16px;
}

.campus-logo__subtitle {
  margin: 0;
  color: #a67a3f;
  letter-spacing: 1px;
  font-size: 10px;
}
</style>

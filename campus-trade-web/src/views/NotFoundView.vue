<template>
  <div class="not-found-page">
    <h1>404</h1>
    <p>页面不存在或已被移动。</p>
    <el-button type="primary" @click="$router.push('/products')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found-page {
  min-height: 50vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
}

.not-found-page h1 {
  margin: 0;
  font-size: 56px;
  color: #8f6b16;
}

.not-found-page p {
  margin: 0;
  color: #6f4a11;
}
</style>

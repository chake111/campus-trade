<template>
  <div class="product-create-container" v-loading="loading">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>发布商品</span>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="商品名称" prop="title">
          <el-input v-model="formData.title" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品价格" prop="price">
          <el-input-number
            v-model="formData.price"
            :min="0.01"
            :precision="2"
            placeholder="请输入商品价格"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入商品详细描述"
          />
        </el-form-item>
        <el-form-item label="图片地址" prop="image">
          <el-input
            v-model="formData.image"
            placeholder="请输入图片链接（可选）"
          >
            <template #append>
              <el-button @click="previewImage">预览</el-button>
            </template>
          </el-input>
          <div v-if="formData.image" class="image-preview">
            <el-image
              :src="formData.image"
              fit="cover"
              :preview-src-list="[formData.image]"
              style="width: 200px; height: 200px; margin-top: 10px; border-radius: 8px;"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="loading">提交发布</el-button>
          <el-button @click="goBack">返回商品列表</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createProduct } from '@/api/product'
import { getToken } from '@/utils/request'
import { getUserId } from '@/utils/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const formRef = ref()

const formData = ref({
  title: '',
  price: undefined,
  description: '',
  image: ''
})

const rules = {
  title: [
    { required: true, message: '请输入商品名称', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'change' },
    { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入商品描述', trigger: 'blur' }
  ]
}

const submitForm = async () => {
  const token = getToken()
  const userId = getUserId()
  if (!token || !userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await formRef.value.validate()
    loading.value = true

    const payload = {
      ...formData.value,
      userId,
      sellerId: userId
    }

    await createProduct(payload)
    ElMessage.success('商品发布成功')
    router.push('/')
  } catch (error) {
    console.error('发布失败:', error)
    const backendMessage = error?.message || error?.data?.message || error?.response?.data?.message
    ElMessage.error(backendMessage || '商品发布失败，请重试')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/products')
}

const previewImage = () => {
  if (!formData.value.image) {
    ElMessage.warning('请先输入图片地址')
    return
  }
  ElMessageBox.alert(``, {
    title: '图片预览',
    dangerouslyUseHTMLString: true,
    message: `<img src="${formData.value.image}" style="max-width: 100%; max-height: 500px;" />`,
    showConfirmButton: false,
    cancelButtonText: '关闭'
  })
}
</script>

<style scoped>
.product-create-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
}

.box-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.image-preview {
  margin-top: 10px;
}
</style>

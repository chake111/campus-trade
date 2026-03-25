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
        <el-form-item label="校内交易地点" prop="tradeLocation">
          <el-select
            v-model="formData.tradeLocation"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="请选择或输入模糊位置（如：东区、图书馆附近）"
            style="width: 100%"
          >
            <el-option
              v-for="option in tradeLocationOptions"
              :key="option"
              :label="option"
              :value="option"
            />
          </el-select>
          <div class="location-tip">仅支持校内模糊面交地点，请勿填写宿舍号、门牌号等精确住址。</div>
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
  tradeLocation: '',
  image: ''
})

const tradeLocationOptions = ['东区', '西区', '图书馆附近', '一食堂附近', '宿舍园区附近', '校内面交']

const preciseAddressPattern = /(宿舍\s*\d+|\d+\s*栋|\d+\s*单元|\d+\s*室|门牌|详细地址|经纬度)/

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
  ],
  tradeLocation: [
    { required: true, message: '请选择或输入校内交易地点', trigger: 'change' },
    { min: 2, max: 30, message: '交易地点请控制在 2-30 个字', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const normalized = String(value || '').trim()
        if (!normalized) {
          callback(new Error('请选择或输入校内交易地点'))
          return
        }
        if (preciseAddressPattern.test(normalized)) {
          callback(new Error('请使用模糊位置，不要填写宿舍号或门牌号'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ]
}

const submitForm = async () => {
  const token = getToken()
  const userId = getUserId()
  if (!token || !userId) {
    ElMessage.warning('请先登录后再操作')
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
    ElMessage.success('发布成功')
    router.push('/')
  } catch (error) {
    console.error('商品发布失败详情:', error)
    ElMessage.error('操作失败，请稍后重试')
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
  max-width: 1280px;
  margin: 8px auto 24px;
  padding: 10px 8px 24px;
}

.page-hero {
  margin-bottom: 22px;
  padding: 24px 28px;
  border-radius: 16px;
  border: 1px solid #efdca8;
  background: linear-gradient(135deg, #fffef8 0%, #fff4cf 100%);
}

.page-hero h1 {
  margin: 0;
  font-size: 34px;
  color: #3c3c3c;
}

.page-hero p {
  margin: 10px 0 0;
  color: #7a6740;
  font-size: 16px;
}

.box-card {
  border-radius: 16px;
  border: 1px solid #f0e4c6;
  box-shadow: 0 10px 24px rgba(65, 49, 23, 0.08);
}

.box-card :deep(.el-card__header) {
  padding: 18px 24px;
}

.box-card :deep(.el-card__body) {
  padding: 24px;
}

.box-card :deep(.el-form) {
  max-width: 960px;
}

.card-header {
  font-size: 24px;
  font-weight: 600;
  color: #4c3b16;
}

.box-card :deep(.el-form-item) {
  margin-bottom: 22px;
}

.box-card :deep(.el-button--primary) {
  background: #ffd45a;
  border-color: #ffd45a;
  color: #3c3c3c;
}

.box-card :deep(.el-button--primary:hover) {
  background: #ffca33;
  border-color: #ffca33;
}

.image-preview {
  margin-top: 10px;
}

.location-tip {
  margin-top: 8px;
  color: #8c6d1f;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 992px) {
  .product-create-container {
    max-width: 100%;
    padding: 4px 0 14px;
  }

  .page-hero {
    padding: 18px 20px;
  }

  .page-hero h1 {
    font-size: 28px;
  }

  .box-card :deep(.el-card__header),
  .box-card :deep(.el-card__body) {
    padding: 16px;
  }
}
</style>

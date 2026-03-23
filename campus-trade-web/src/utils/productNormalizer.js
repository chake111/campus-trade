import bookFallback from '../assets/product-fallback/categories/cat-book-01.svg'
import digitalFallback from '../assets/product-fallback/categories/cat-digital-01.svg'
import dailyFallback from '../assets/product-fallback/categories/cat-daily-01.svg'
import sportFallback from '../assets/product-fallback/categories/cat-sport-01.svg'
import ticketFallback from '../assets/product-fallback/categories/cat-ticket-01.svg'
import defaultFallback from '../assets/product-fallback/default/default-01.svg'

const CATEGORY_FALLBACKS = {
  book: [bookFallback],
  digital: [digitalFallback],
  daily: [dailyFallback],
  sport: [sportFallback],
  ticket: [ticketFallback],
  default: [defaultFallback],
}

const CATEGORY_KEYWORDS = [
  { key: 'book', words: ['书', '教材', '考研', '笔记', '小说', 'book', 'textbook', 'kindle'] },
  { key: 'digital', words: ['手机', '电脑', '笔记本', 'ipad', '耳机', '键盘', '鼠标', '相机', '显示器', 'digital', 'laptop', 'iphone'] },
  { key: 'daily', words: ['日用', '宿舍', '收纳', '台灯', '杯', '水壶', '被子', '衣架', '洗漱', 'daily', 'home'] },
  { key: 'sport', words: ['运动', '球', '羽毛球', '篮球', '健身', '跑步', '自行车', 'sport', 'fitness', 'yoga'] },
  { key: 'ticket', words: ['票', '演出', '电影', '车票', '门票', '次卡', '券', 'ticket', 'coupon'] },
]

const normalizeText = (value) => String(value ?? '').trim()

const pickProductId = (raw) => {
  if (!raw || typeof raw !== 'object') return null
  return raw.id ?? raw.productId ?? raw.goodsId ?? raw.product_id ?? null
}

const pickRawImage = (raw) => {
  const image = normalizeText(raw?.image)
  if (image) return { image, source: 'image' }

  const imageUrl = normalizeText(raw?.imageUrl)
  if (imageUrl) return { image: imageUrl, source: 'imageUrl' }

  const cover = normalizeText(raw?.cover)
  if (cover) return { image: cover, source: 'cover' }

  return { image: '', source: '' }
}

const detectFallbackCategory = (title, description) => {
  const haystack = `${normalizeText(title)} ${normalizeText(description)}`.toLowerCase()
  if (!haystack) return 'default'

  for (const category of CATEGORY_KEYWORDS) {
    if (category.words.some((word) => haystack.includes(word.toLowerCase()))) {
      return category.key
    }
  }

  return 'default'
}

const stableHash = (seed) => {
  const text = normalizeText(seed)
  if (!text) return 0

  let hash = 0
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 31 + text.charCodeAt(i)) >>> 0
  }
  return hash
}

const pickStableFallbackImage = (fallbackKey, seed) => {
  const pool = CATEGORY_FALLBACKS[fallbackKey] ?? CATEGORY_FALLBACKS.default
  if (!pool.length) return defaultFallback
  const idx = stableHash(seed) % pool.length
  return pool[idx]
}


export const isProductOrderableStatus = (status) => {
  if (status === null || status === undefined || status === '') return true
  return Number(status) === 1
}

const normalizeStatusToken = (status) => {
  if (status === null || status === undefined) return ''
  return String(status).trim().toUpperCase()
}

export const getProductStatusMeta = (status) => {
  const token = normalizeStatusToken(status)

  if (!token || token === '1' || token === 'ON_SALE' || token === 'AVAILABLE' || token === '在售') {
    return { label: '在售', type: 'on-sale' }
  }

  if (token === '2' || token === 'SOLD' || token === 'FINISHED' || token === '已售出') {
    return { label: '已售出', type: 'sold' }
  }

  if (
    token === '0' ||
    token === '3' ||
    token === 'OFF_SHELF' ||
    token === 'DISABLED' ||
    token === 'REMOVED' ||
    token === '已下架'
  ) {
    return { label: '已下架', type: 'off-shelf' }
  }

  if (isProductOrderableStatus(status)) {
    return { label: '在售', type: 'on-sale' }
  }
  return { label: '已下架', type: 'off-shelf' }
}

export const normalizeProduct = (raw) => {
  const id = pickProductId(raw)
  const title = normalizeText(raw?.title ?? raw?.name) || '未命名商品'
  const description = normalizeText(raw?.description ?? raw?.desc) || '暂无描述'
  const price = raw?.price ?? 0

  const { image, source } = pickRawImage(raw)
  const fallbackKey = detectFallbackCategory(title, description)
  const stableSeed = id ?? title
  const fallbackImage = pickStableFallbackImage(fallbackKey, stableSeed)

  const imageSource = image ? source : `fallback:${fallbackKey}`
  const displayImage = image || fallbackImage

  return {
    ...(raw && typeof raw === 'object' ? raw : {}),
    id,
    title,
    description,
    price,
    image,
    imageSource,
    fallbackKey,
    displayImage,
  }
}

export const normalizeProductList = (rawList) => {
  if (!Array.isArray(rawList)) return []
  return rawList.map((item) => normalizeProduct(item))
}

export const normalizeProductResponseList = (res) => {
  if (Array.isArray(res?.data)) return normalizeProductList(res.data)
  if (Array.isArray(res)) return normalizeProductList(res)
  return []
}

export const normalizeProductResponseDetail = (res) => {
  const payload = res?.data !== undefined ? res.data : res
  if (!payload || typeof payload !== 'object') return null
  return normalizeProduct(payload)
}

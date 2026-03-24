const ORDER_STATUS_META = {
  PENDING: { text: '待支付', tagType: 'warning' },
  PAID: { text: '已支付', tagType: 'primary' },
  CONFIRMED: { text: '已确认', tagType: 'success' },
  FINISHED: { text: '已完成', tagType: 'success' },
  CANCELLED: { text: '已取消', tagType: 'info' },
}

const ORDER_TAB_OPTIONS = [
  { label: '全部', value: 'ALL' },
  { label: '待支付', value: 'PENDING' },
  { label: '已支付/待确认', value: 'PAID' },
  { label: '已完成', value: 'FINISHED' },
  { label: '已取消', value: 'CANCELLED' },
]

function getOrderStatus(orderOrStatus) {
  if (typeof orderOrStatus === 'string') return orderOrStatus
  return orderOrStatus?.status
}

export function getOrderStatusText(orderOrStatus) {
  const status = getOrderStatus(orderOrStatus)
  return ORDER_STATUS_META[status]?.text || status || '-'
}

export function getOrderStatusTagType(orderOrStatus) {
  const status = getOrderStatus(orderOrStatus)
  return ORDER_STATUS_META[status]?.tagType || 'info'
}

export function getOrderTabOptions() {
  return ORDER_TAB_OPTIONS
}

export function matchesOrderStatusTab(order, tabValue) {
  if (tabValue === 'ALL') return true
  if (tabValue === 'PAID') return ['PAID', 'CONFIRMED'].includes(order?.status)
  return order?.status === tabValue
}

function getStatusActions(order, canBuyerOperate) {
  if (!canBuyerOperate) return []

  if (order?.status === 'PENDING') {
    return [
      {
        key: 'pay',
        kind: 'status',
        label: '去支付',
        level: 'primary',
        buttonType: 'primary',
        targetStatus: 'PAID',
        actionText: '支付',
      },
      {
        key: 'cancel',
        kind: 'status',
        label: '取消',
        level: 'secondary',
        isText: true,
        targetStatus: 'CANCELLED',
        actionText: '取消',
      },
    ]
  }

  if (order?.status === 'PAID') {
    return [
      {
        key: 'confirm',
        kind: 'status',
        label: '确认收货',
        level: 'primary',
        buttonType: 'success',
        targetStatus: 'CONFIRMED',
        actionText: '确认',
      },
    ]
  }

  if (order?.status === 'CONFIRMED') {
    return [
      {
        key: 'finish',
        kind: 'status',
        label: '完成订单',
        level: 'primary',
        buttonType: 'warning',
        targetStatus: 'FINISHED',
        actionText: '完成',
      },
    ]
  }

  return []
}

export function getOrderActions(order, perspective, options = {}) {
  const canBuyerOperate = options.canBuyerOperate === true
  const statusActions = getStatusActions(order, canBuyerOperate)
  const hasStatusActions = statusActions.length > 0
  const contactLabel = perspective === 'buyer' ? '联系卖家' : '联系买家'

  const baseActions = hasStatusActions
    ? [
        {
          key: 'contact',
          kind: 'contact',
          label: contactLabel,
          level: 'secondary',
          isText: true,
        },
        {
          key: 'view',
          kind: 'view',
          label: '查看详情',
          level: 'secondary',
          isText: true,
        },
      ]
    : [
        {
          key: 'view',
          kind: 'view',
          label: '查看详情',
          level: 'primary',
          buttonType: 'primary',
        },
        {
          key: 'contact',
          kind: 'contact',
          label: contactLabel,
          level: 'secondary',
          isText: true,
        },
      ]

  return [...statusActions, ...baseActions]
}

export function getPrimaryOrderAction(actions = []) {
  return actions.find((action) => action?.level === 'primary') || null
}

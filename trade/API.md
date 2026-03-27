# Campus Trade 后端接口文档

> Base URL: `http://localhost:8080`

## 1. 统一说明

- 大部分接口返回统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- 认证方式：JWT（登录后获取 token）
  - 请求头：`Authorization: Bearer <token>`
- 公开接口：登录、注册、商品列表/详情、推荐相关、`/test`

---

## 2. 用户模块

### 2.1 用户注册
- **POST** `/user/register`
- Body:

```json
{
  "username": "alice",
  "password": "123456"
}
```

### 2.2 用户登录
- **POST** `/user/login`
- Body:

```json
{
  "username": "alice",
  "password": "123456"
}
```

- 成功后 `data` 中包含 `token` 与 `userInfo`

### 2.3 按 ID 查询用户
- **GET** `/api/users/{id}`
- 需登录

### 2.4 创建用户
- **POST** `/api/users`
- 需登录

### 2.5 更新当前用户资料
- **PUT** `/api/users/me/profile`
- 需登录
- Body:

```json
{
  "username": "alice_new",
  "avatar": "https://example.com/a.png"
}
```

---

## 3. 商品模块

### 3.1 发布商品
- **POST** `/product/add`
- 需登录
- Body 示例：

```json
{
  "title": "二手显示器",
  "description": "95 新",
  "price": 199.0,
  "category": "数码",
  "userId": 1
}
```

### 3.2 商品列表
- **GET** `/product/list`
- Query（可选）：`keyword`

### 3.3 我的商品
- **GET** `/product/mine`
- 需登录
- Query（可选）：`keyword`

### 3.4 商品详情
- **GET** `/product/{id}`

---

## 4. 订单模块

### 4.1 创建订单
- **POST** `/order/create`
- 需登录
- Body:

```json
{
  "productId": 1001,
  "userId": 1
}
```

> 普通用户会以当前登录用户作为买家，管理员可传 `userId` 指定买家。

### 4.2 订单列表
- **GET** `/order/list`
- 需登录
- Query：
  - `userId`（可选，管理员可用）
  - `role`（可选，默认 `buyer`，可选 `seller`）

### 4.3 更新订单状态
- **POST** `/order/update`
- 需登录
- Body:

```json
{
  "orderId": 2001,
  "status": "PAID"
}
```

- `status` 可选值：`PENDING`、`PAID`、`CONFIRMED`、`FINISHED`、`CANCELLED`

---

## 5. 咨询/消息模块

接口前缀：`/api/consult`（均需登录）

### 5.1 创建/获取会话
- **POST** `/api/consult/sessions/ensure`
- Body:

```json
{
  "productId": 1001,
  "counterpartId": 2
}
```

### 5.2 会话列表
- **GET** `/api/consult/sessions`

### 5.3 未读会话数
- **GET** `/api/consult/sessions/unread-count`

### 5.4 会话消息列表
- **GET** `/api/consult/sessions/{sessionId}/messages`

### 5.5 发送消息
- **POST** `/api/consult/sessions/{sessionId}/messages`
- Body:

```json
{
  "content": "这个商品还在吗？"
}
```

---

## 6. 推荐模块

接口前缀：`/api/recommend`（当前为公开）

- **GET** `/api/recommend/{userId}` 基础推荐
- **GET** `/api/recommend/smart/{userId}` 智能推荐
- **GET** `/api/recommend/personal/{userId}` 个性化推荐
- **GET** `/api/recommend/popular?limit=10` 热门商品
- **GET** `/api/recommend/explain/{userId}?productIds=1&productIds=2` 推荐解释
- **GET** `/api/recommend/details/{userId}` 推荐打分明细（调试）

---

## 7. 信用模块

接口前缀：`/credit`（需登录）

> 这两个接口返回 `ResponseEntity`（非统一 Result 结构）。

### 7.1 信用记录
- **GET** `/credit/log?userId=1&page=0&size=10`

### 7.2 当前信用分
- **GET** `/credit/score?userId=1`

---

## 8. 管理员模块

### 8.1 商品管理（管理员）
接口前缀：`/api/admin/products`（需管理员权限）

- **GET** `/api/admin/products?keyword=xx&status=1`
- **PUT** `/api/admin/products/{id}/off-shelf`
- **PUT** `/api/admin/products/{id}/restore`
- **DELETE** `/api/admin/products/{id}`

### 8.2 仪表盘汇总（管理员）
- **GET** `/api/admin/dashboard/summary`

---

## 9. 测试接口

- **GET** `/test`
- 返回：`hello campus trade`

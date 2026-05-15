# Skill: 用户认证与权限

## 路由判定

### 应该读这个 Skill 的场景
- 实现/修改用户注册、登录功能
- 修改 JWT Token 的生成、校验逻辑
- 修改 Spring Security 配置（过滤器链、端点权限）
- 修改 User 实体或 UserRepository
- 添加新的认证方式（如 OAuth2）
- 修改密码加密策略

### 不应该读这个 Skill 的场景
- 修改笔记 CRUD（应读 Skill: 笔记管理）
- 修改 AI 问答逻辑（应读 Skill: 意图路由）
- 修改前端页面布局（无关）
- 修改数据库连接配置（见 `config/` 目录）

## 业务领域知识

### 背景
MindFlow 使用 JWT（JSON Web Token）进行无状态认证。用户注册后凭用户名密码登录，服务端签发 JWT，客户端在所有后续请求的 Authorization Header 中携带此 Token。

### 核心概念
- **无状态认证**：服务端不维护 Session，Token 自包含用户身份
- **JWT**：HS512 签名，包含 subject(username)、签发时间(iat)、过期时间(exp)
- **PasswordEncoder**：BCrypt 密码加密
- **AuthenticationManager**：Spring Security 认证管理器

### 关键业务规则与不变量

每条规则按 `规则 → 实现位置 → 代码行号` 格式组织。

1. **用户名全局唯一**
   → 注册时调用 `userRepository.existsByUsername()` 前置检查，重复则抛异常
   → 实现: `UserServiceImpl.registerUser()` 第 27 行
   → 位置: `service/UserServiceImpl.java:27-29`

2. **密码永不明文存储**
   → 存入 DB 前必须用 BCrypt 编码，永不可逆
   → 实现: `PasswordEncoder` Bean（`BCryptPasswordEncoder`）
   → 位置: `config/SecurityConfig.java:28-31` | 调用处 `service/UserServiceImpl.java:33`

3. **Token 有效期 24 小时**
   → JWT 签发时写入 exp 声明，过期后服务端拒收
   → 实现: `JwtTokenProvider.generateToken()` 读取 `jwt.expiration`
   → 位置: `application.yml:63-64` | `security/JwtTokenProvider.java:31`

4. **认证端点开放，其余全部保护**
   → `/api/auth/**` permitAll，其余所有 `/api/**` 需携带有效 JWT
   → 实现: `SecurityConfig.filterChain()` 的 `authorizeHttpRequests` 链
   → 位置: `config/SecurityConfig.java:43-46`

5. **CSRF 禁用**
   → JWT 无状态模式下 CSRF 攻击面不存在，故关闭
   → 实现: `http.csrf(AbstractHttpConfigurer::disable)`
   → 位置: `config/SecurityConfig.java:41`

6. **无状态会话**
   → 服务端不创建 HttpSession，每次请求独立认证
   → 实现: `session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)`
   → 位置: `config/SecurityConfig.java:42`

7. **User 实体无 role/权限体系**
   → 当前仅做身份认证，所有已登录用户权限平等
   → 约束: `User.java` 无 role 字段，`UserDetailsServiceImpl` 返回 empty authorities
   → 位置: `domain/User.java:10-32` | `security/UserDetailsServiceImpl.java:27`

## 核心代码流程

### 注册流程

```
用户 POST /api/auth/register
  → AuthController.registerUser(RegisterRequest)
    → UserService.registerUser(request)
      → 检查 username 是否已存在
      → 创建 User，password 用 BCrypt 编码
      → userRepository.save(user)
  ← 返回 { "message": "User registered successfully" }
```

关键代码位置：
- Controller: `controller/AuthController.java:18-26`
- Service: `service/UserServiceImpl.java:26-37`
- Entity: `domain/User.java:10-32`

### 登录流程

```
用户 POST /api/auth/login
  → AuthController.authenticateUser(LoginRequest)
    → UserService.loginUser(request)
      → authenticationManager.authenticate(UsernamePasswordAuthenticationToken)
        → UserDetailsServiceImpl.loadUserByUsername(username)
          → userRepository.findByUsername(username)
      → JwtTokenProvider.generateToken(authentication)
        → 构建 JWT: subject=username, HS512 签名
  ← 返回 { "token": "eyJhbGci..." }
```

关键代码位置：
- Controller: `controller/AuthController.java:28-33`
- Service: `service/UserServiceImpl.java:39-47`
- Auth: `security/UserDetailsServiceImpl.java:20-30`
- Token: `security/JwtTokenProvider.java:29-39`

### JWT 验证过滤流程

```
每次请求（除 /api/auth/**）
  → JwtAuthenticationFilter.doFilterInternal(request, response, filterChain)
    → 从 Authorization Header 提取 "Bearer <token>"
    → JwtTokenProvider.validateToken(token)
      → 解析 JWS，校验签名和过期时间
    → 提取 username
    → UserDetailsServiceImpl 加载用户
    → 设置 SecurityContextHolder Authentication
  → filterChain.doFilter() 继续后续处理
```

关键代码位置：
- Filter: `security/JwtAuthenticationFilter.java:20-54`
- Token 验证: `security/JwtTokenProvider.java:52-61`
- SecurityConfig: `config/SecurityConfig.java:19-51`

## 变更指南

### 修改时机
- 需要增加用户字段（如 avatar, phone）→ 修改 `User.java` + `RegisterRequest.java`
- 需要修改 Token 过期时间 → 修改 `application.yml` 中 `jwt.expiration` 或环境变量
- 需要开放新 API 端点 → 修改 `SecurityConfig.filterChain()` 中 `requestMatchers`
- 需要升级 JWT 库版本 → 修改 `pom.xml` jjwt 版本号

### 影响检查清单
- [ ] 修改 User 实体 → 是否需要更新 DB schema（JPA ddl-auto=update 自动处理）
- [ ] 修改 JWT 逻辑 → 现有已签发的 Token 是否会失效
- [ ] 修改 SecurityConfig → 所有受保护端点是否仍正确受控
- [ ] 修改 UserService → 登录流程是否完整（authenticationManager.authenticate → tokenProvider.generateToken）
- [ ] 添加新字段 → 是否需要前端同步更新

### 常见变更模式

**新增用户字段**：
1. 在 `domain/User.java` 添加字段
2. 在 `dto/request/RegisterRequest.java` 添加校验
3. 在 `service/UserServiceImpl.registerUser()` 中赋值

**修改 Token 过期策略**：
```bash
# 设置环境变量，覆盖 application.yml 中的默认值
export JWT_EXPIRATION=604800000  # 7天
```

**开放新 API 端点**：
```java
// 在 SecurityConfig.filterChain() 中添加
.requestMatchers("/api/public/**").permitAll()
```

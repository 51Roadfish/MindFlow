# 用户认证 — API Contracts

## POST /api/auth/register

注册新用户。

**Request Body:**
```json
{
  "username": "string (required, max 50)",
  "password": "string (required)",
  "email": "string (optional, email format)"
}
```

**Response 200:**
```json
{
  "message": "User registered successfully"
}
```

**Response 400:**
```json
{
  "error": "Username is already taken!"
}
```

## POST /api/auth/login

登录并获取 JWT Token。

**Request Body:**
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response 400:**
```json
{
  "error": "Bad credentials"
}
```

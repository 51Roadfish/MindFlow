import requests, json
base = 'http://localhost:8081/api'

# Login
r = requests.post(f'{base}/auth/login', json={'username':'test123','password':'123456'})
print('Login status:', r.status_code)
if r.status_code != 200:
    r = requests.post(f'{base}/auth/register', json={'username':'test123','email':'t@t.com','password':'123456'})
    print('Register:', r.status_code, r.text)
    r = requests.post(f'{base}/auth/login', json={'username':'test123','password':'123456'})
    print('Login2:', r.status_code)

token = r.json().get('token', '')
print('Token obtained:', bool(token))

# Chat
r = requests.post(f'{base}/ai/chat', json={'question':'你好'},
                  headers={'Authorization':f'Bearer {token}'})
print('Status:', r.status_code)
print('Response:', r.text[:500])

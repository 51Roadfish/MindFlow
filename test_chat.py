import requests
r = requests.post('http://localhost:8081/api/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']
print('Token:', token[:30])
r = requests.post('http://localhost:8081/api/ai/chat', json={'question':'你好'}, headers={'Authorization': f'Bearer {token}'})
print('Status:', r.status_code)
print('Response:', r.text[:500])

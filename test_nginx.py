import requests

# Test through nginx proxy (same as frontend)
r = requests.post('http://localhost:8082/api/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']
print('Login via nginx: OK')

r = requests.post('http://localhost:8082/api/ai/chat', json={'question':'你好'}, headers={'Authorization': f'Bearer {token}'})
print(f'Chat via nginx: {r.status_code}')
print(f'Response keys: {list(r.json().keys())}')
print(f'intent: {r.json().get("intent")}')

import requests, time

r = requests.post('http://localhost:8082/api/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']
print('Login via nginx OK')

# Chat
start = time.time()
r = requests.post('http://localhost:8082/api/ai/chat', json={'question':'你好'}, headers={'Authorization': f'Bearer {token}'}, timeout=30)
elapsed = time.time() - start
print(f'Chat: {r.status_code} ({elapsed:.1f}s), intent={r.json().get("intent")}')

# Search
r = requests.post('http://localhost:8082/api/ai/search', json={'query':'Java'}, headers={'Authorization': f'Bearer {token}'}, timeout=30)
results = r.json()
print(f'Search: {r.status_code}, results={len(results)}')

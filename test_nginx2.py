import requests, time

# Login through nginx
r = requests.post('http://localhost:8082/api/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']
print(f'Login via nginx: OK')

# Chat through nginx
start = time.time()
r = requests.post('http://localhost:8082/api/ai/chat', json={'question':'你好'}, headers={'Authorization': f'Bearer {token}'}, timeout=120)
elapsed = time.time() - start
print(f'Chat via nginx: {r.status_code} ({elapsed:.1f}s)')

# Search through nginx
r = requests.post('http://localhost:8082/api/ai/search', json={'query':'测试'}, headers={'Authorization': f'Bearer {token}'}, timeout=30)
print(f'Search via nginx: {r.status_code}')
if r.status_code == 200:
    print(f'  Results: {len(r.json())}')

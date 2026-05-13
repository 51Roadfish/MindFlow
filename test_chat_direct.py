import requests, time

# Test backend directly
start = time.time()
r = requests.post('http://localhost:8081/api/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']
print(f'Login OK ({time.time()-start:.1f}s)')

start = time.time()
r = requests.post('http://localhost:8081/api/ai/chat', json={'question':'你好'}, headers={'Authorization': f'Bearer {token}'}, timeout=60)
elapsed = time.time() - start
print(f'Chat direct: {r.status_code} ({elapsed:.1f}s)')
if r.status_code == 200:
    print(f'intent={r.json().get("intent")}')

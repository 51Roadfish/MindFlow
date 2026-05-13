import requests, time

base = 'http://localhost:8081/api'

r = requests.post(f'{base}/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']
print('Login OK')

# Chat
start = time.time()
r = requests.post(f'{base}/ai/chat', json={'question':'你好'}, headers={'Authorization': f'Bearer {token}'}, timeout=30)
elapsed = time.time() - start
print(f'Chat: {r.status_code} ({elapsed:.1f}s)')
if r.status_code == 200:
    d = r.json()
    print(f'  intent={d["intent"]}')
    print(f'  answer={d["answer"][:80]}')

# Search
r = requests.post(f'{base}/ai/search', json={'query':'测试'}, headers={'Authorization': f'Bearer {token}'}, timeout=30)
print(f'\nSearch: {r.status_code}')
if r.status_code == 200:
    results = r.json()
    print(f'  Results: {len(results)}')
    for res in results[:3]:
        md = res.get('metadata', {})
        print(f'  - Note {md.get("noteId")}: {md.get("noteTitle","")} -> {res.get("content","")[:40]}')

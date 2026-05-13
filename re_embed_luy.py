import requests, time

base = 'http://localhost:8081/api'

# Login as luy
r = requests.post(f'{base}/auth/login', json={'username':'luy','password':'123456'})
if r.status_code != 200:
    print(f'Login failed: {r.text}')
    exit(1)
token = r.json()['token']
print('Login as luy OK')

# Get luy's notes
r = requests.get(f'{base}/notes', headers={'Authorization': f'Bearer {token}'})
notes = r.json()
print(f"luy's notes: {len(notes)}")

for note in notes:
    nid = note['id']
    r = requests.put(f'{base}/notes/{nid}', json={
        'title': note['title'],
        'content': note['content']
    }, headers={'Authorization': f'Bearer {token}'})
    print(f'  Note {nid} ({note["title"]}): triggered OK')
    time.sleep(0.3)

print('\nWaiting 8s for embeddings...')
time.sleep(8)

# Test search
r = requests.post(f'{base}/ai/search', json={'query': 'redis'},
                  headers={'Authorization': f'Bearer {token}'})
results = r.json()
print(f'Search "redis": {r.status_code}, results={len(results)}')
for res in results[:3]:
    md = res.get('metadata', {})
    print(f'  Note {md.get("noteId")}: {res.get("content","")[:50]}')

# Test chat
r = requests.post(f'{base}/ai/chat', json={'question': 'redis是什么'},
                  headers={'Authorization': f'Bearer {token}'}, timeout=30)
print(f'\nChat: {r.status_code}')
if r.status_code == 200:
    d = r.json()
    print(f'  intent={d["intent"]}')
    print(f'  answer={d["answer"][:100]}')

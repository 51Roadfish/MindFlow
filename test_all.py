import requests, json, time

base = 'http://localhost:8081/api'

# Login
r = requests.post(f'{base}/auth/login', json={'username':'test123','password':'123456'})
if r.status_code != 200:
    requests.post(f'{base}/auth/register', json={'username':'test123','email':'t@t.com','password':'123456'})
    r = requests.post(f'{base}/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']
print('Login OK')

# Create a note and check the response time
r = requests.post(f'{base}/notes', json={
    'title': '测试搜索和时间的笔记',
    'content': '这是一条用于测试语义搜索能否正常工作的笔记内容，包含一些关键信息。',
    'notebookId': 1
}, headers={'Authorization': f'Bearer {token}'})
print(f'Create note: {r.status_code}')
note = r.json()
print(f'  id={note.get("id")}, createdAt={note.get("createdAt")}, updatedAt={note.get("updatedAt")}')

# Wait for async embedding
print('Waiting for embedding...')
time.sleep(5)

# Search for it
r = requests.post(f'{base}/ai/search', json={'query': '测试语义搜索'},
                  headers={'Authorization': f'Bearer {token}'})
print(f'Search: {r.status_code}')
if r.status_code == 200:
    results = r.json()
    print(f'  Results count: {len(results)}')
    for res in results:
        print(f'  - {res.get("metadata",{}).get("noteTitle","")}: {res.get("content","")[:40]}')
else:
    print(f'  Error: {r.text[:300]}')

# Test chat
r = requests.post(f'{base}/ai/chat', json={'question': '你好'},
                  headers={'Authorization': f'Bearer {token}'})
print(f'\nChat: {r.status_code}')
if r.status_code == 200:
    data = r.json()
    print(f'  intent={data.get("intent")}')
    print(f'  answer={data.get("answer","")[:80]}')
else:
    print(f'  Error: {r.text[:300]}')

import requests, json, time
base = 'http://localhost:8081/api'

# Login
r = requests.post(f'{base}/auth/login', json={'username':'test123','password':'123456'})
if r.status_code != 200:
    requests.post(f'{base}/auth/register', json={'username':'test123','email':'t@t.com','password':'123456'})
    r = requests.post(f'{base}/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']

# Create a note with meaningful content
r = requests.post(f'{base}/notes', json={
    'title': 'Java编程入门',
    'content': 'Java是一种面向对象的编程语言，广泛用于企业级应用开发。Spring框架是Java生态中最流行的框架之一。',
    'notebookId': 1
}, headers={'Authorization': f'Bearer {token}'})
print('Create note:', r.status_code, r.json().get('id'))

# Wait for async embedding
time.sleep(3)

# Search for it
r = requests.post(f'{base}/ai/search', json={'query': 'Java编程'},
                  headers={'Authorization': f'Bearer {token}'})
print('Search results:', r.status_code)
print('Response:', r.text[:500])

# Also test chat
r = requests.post(f'{base}/ai/chat', json={'question': '你好'},
                  headers={'Authorization': f'Bearer {token}'})
print('\nChat:', r.status_code)
print('Response:', r.text[:300])

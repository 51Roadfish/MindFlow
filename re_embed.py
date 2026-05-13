import requests, time, json

base = 'http://localhost:8081/api'

r = requests.post(f'{base}/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']
print('Login OK')

# Get all notes
r = requests.get(f'{base}/notes', headers={'Authorization': f'Bearer {token}'})
notes = r.json()
print(f'Total notes: {len(notes)}')

# Re-embed each note by triggering update
for note in notes:
    nid = note['id']
    # Update note to trigger embedAndStore
    r = requests.put(f'{base}/notes/{nid}', json={
        'title': note['title'],
        'content': note['content']
    }, headers={'Authorization': f'Bearer {token}'})
    print(f'  Note {nid} ({note["title"][:15]}): trigger OK')
    time.sleep(0.3)

print('\nWaiting 8s for async embeddings...')
time.sleep(8)

r = requests.post(f'{base}/ai/search', json={'query': 'Java'},
                  headers={'Authorization': f'Bearer {token}'})
results = r.json()
print(f'Search results: {len(results)}')
for res in results[:5]:
    md = res.get('metadata', {})
    pid = md.get('noteId', '?')
    print(f'  Note {pid}: {md.get("noteTitle","")} -> {res.get("content","")[:50]}')

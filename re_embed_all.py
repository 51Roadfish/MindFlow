import requests, time

base = 'http://localhost:8081/api'

for user in ['luy', 'test123']:
    r = requests.post(f'{base}/auth/login', json={'username': user, 'password': '123456'})
    token = r.json()['token']

    r = requests.get(f'{base}/notes', headers={'Authorization': f'Bearer {token}'})
    notes = r.json()
    print(f'{user}: {len(notes)} notes')

    for note in notes:
        requests.put(f'{base}/notes/{note["id"]}', json={
            'title': note['title'],
            'content': note['content']
        }, headers={'Authorization': f'Bearer {token}'})
        print(f'  Note {note["id"]} done')
        time.sleep(0.3)

print('\nWaiting 12s for async embeddings...')
time.sleep(12)

# Test
for user in ['luy', 'test123']:
    r = requests.post(f'{base}/auth/login', json={'username': user, 'password': '123456'})
    token = r.json()['token']

    r = requests.post(f'{base}/ai/search', json={'query': 'redis'},
                      headers={'Authorization': f'Bearer {token}'})
    results = r.json() if r.status_code == 200 else []
    print(f'{user} search "redis": {len(results)} results')

docker_result = __import__('subprocess').run(
    'docker exec mindflow-postgres psql -U mindflow -d mindflow -c "SELECT metadata->>\\\'userId\\\' AS uid, COUNT(*) FROM vector_store GROUP BY uid;"',
    capture_output=True, text=True, shell=True
)
print(f'Vector store:\n{docker_result.stdout}')

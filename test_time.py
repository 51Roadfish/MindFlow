import requests, json, time

base = 'http://localhost:8081/api'

# Login
r = requests.post(f'{base}/auth/login', json={'username':'test123','password':'123456'})
token = r.json()['token']

# Create a note
r = requests.post(f'{base}/notes', json={
    'title': '时间测试',
    'content': '测试时间更新功能',
    'notebookId': 1
}, headers={'Authorization': f'Bearer {token}'})
note = r.json()
print(f'Created: id={note.get("id")}, updatedAt={note.get("updatedAt")}')

# Update the note
time.sleep(1)
r = requests.put(f'{base}/notes/{note.get("id")}', json={
    'content': '更新后的内容'
}, headers={'Authorization': f'Bearer {token}'})
updated = r.json()
print(f'Updated: updatedAt={updated.get("updatedAt")}')

# Check if times differ
if note.get('updatedAt') != updated.get('updatedAt'):
    print('OK: updatedAt changed after update')
else:
    print('ISSUE: updatedAt did NOT change after update')

# Check the list
r = requests.get(f'{base}/notes', headers={'Authorization': f'Bearer {token}'})
notes = r.json()
for n in notes:
    if n.get('id') == note.get('id'):
        print(f'\nIn list: id={n.get("id")}, updatedAt={n.get("updatedAt")}')

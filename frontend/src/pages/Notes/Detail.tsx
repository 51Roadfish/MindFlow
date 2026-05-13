import React, { useEffect, useState } from 'react';
import { Input, Button, Space, message } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import SimpleMdeReact from 'react-simplemde-editor';
import 'easymde/dist/easymde.min.css';
import request from '../../utils/request';

export default function NoteDetail({ isNew = false }: { isNew?: boolean }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  useEffect(() => {
    if (!isNew && id) {
      request.get(`/notes/${id}`).then((res: any) => {
        setTitle(res.title || '');
        setContent(res.content || '');
      }).catch(console.error);
    }
  }, [id, isNew]);

  const handleSave = async () => {
    if (!title) return message.warning('标题不能为空');
    try {
      if (isNew) {
        await request.post('/notes', { title, content, notebookId: 1 });
        message.success('创建成功');
        navigate('/notes');
      } else {
        await request.put(`/notes/${id}`, { title, content });
        message.success('更新成功');
      }
    } catch (e: any) {
      message.error(e.response?.data?.error || '操作失败');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Space style={{ marginBottom: 16 }}>
        <Input 
          size="large" 
          placeholder="笔记标题" 
          value={title} 
          onChange={e => setTitle(e.target.value)} 
          style={{ width: 400 }}
        />
        <Button size="large" type="primary" onClick={handleSave}>保存</Button>
      </Space>
      <div style={{ flex: 1 }}>
        <SimpleMdeReact value={content} onChange={setContent} options={{ autofocus: false, spellChecker: false }} />
      </div>
    </div>
  );
}

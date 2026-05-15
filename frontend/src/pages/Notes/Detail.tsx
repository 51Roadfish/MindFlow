import React, { useEffect, useState } from 'react';
import { Input, Button, Space, message, Select } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import SimpleMdeReact from 'react-simplemde-editor';
import 'easymde/dist/easymde.min.css';
import request from '../../utils/request';

export default function NoteDetail({ isNew = false }: { isNew?: boolean }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [allTags, setAllTags] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isNew && id) {
      request.get(`/notes/${id}`).then((res: any) => {
        setTitle(res.title || '');
        setContent(res.content || '');
        setTags(res.tags || []);
      }).catch(console.error);
    }
    // 收集已有标签供自动补全
    request.get('/notes').then((res: any) => {
      const items = Array.isArray(res) ? res : [];
      const tagSet = new Set<string>();
      items.forEach((n: any) => n.tags?.forEach((t: string) => tagSet.add(t)));
      setAllTags(Array.from(tagSet).sort());
    }).catch(() => {});
  }, [id, isNew]);

  const handleSave = async () => {
    if (!title) return message.warning('标题不能为空');
    setSaving(true);
    try {
      if (isNew) {
        await request.post('/notes', { title, content, notebookId: 1, tags });
        message.success('创建成功');
        navigate('/notes');
      } else {
        await request.put(`/notes/${id}`, { title, content, tags });
        message.success('更新成功');
      }
    } catch (e: any) {
      message.error(e.response?.data?.error || '操作失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          size="large"
          placeholder="笔记标题"
          value={title}
          onChange={e => setTitle(e.target.value)}
          style={{ width: 360 }}
        />
        <Select
          mode="tags"
          style={{ minWidth: 240 }}
          placeholder="添加标签，回车确认"
          value={tags}
          onChange={(vals) => setTags(vals)}
          options={allTags.map(t => ({ label: t, value: t }))}
          tokenSeparators={[',', '，']}
        />
        <Button size="large" type="primary" onClick={handleSave} loading={saving}>
          保存
        </Button>
      </Space>
      <div style={{ flex: 1 }}>
        <SimpleMdeReact value={content} onChange={setContent} options={{ autofocus: false, spellChecker: false }} />
      </div>
    </div>
  );
}

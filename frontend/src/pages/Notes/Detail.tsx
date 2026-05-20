import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { Input, Button, Space, message, Select } from 'antd';
import { PlusOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import SimpleMdeReact from 'react-simplemde-editor';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
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
  const [previewMode, setPreviewMode] = useState(!isNew);

  const editorOptions = useMemo(() => ({
    autofocus: false,
    spellChecker: false,
  }), []);

  const handleContentChange = useCallback((value: string) => {
    setContent(value);
  }, []);

  useEffect(() => {
    if (!isNew && id) {
      request.get(`/notes/${id}`).then((res: any) => {
        setTitle(res.title || '');
        setContent(res.content || '');
        setTags(res.tags || []);
      }).catch(console.error);
    }
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
        <Button
          size="large"
          icon={previewMode ? <EyeOutlined /> : <EyeInvisibleOutlined />}
          onClick={() => setPreviewMode(v => !v)}
        >
          {previewMode ? '编辑' : '预览'}
        </Button>
        <Button size="large" type="primary" onClick={handleSave} loading={saving}>
          保存
        </Button>
      </Space>
      <div style={{ flex: 1, overflow: 'auto' }}>
        {previewMode ? (
          <div className="markdown-preview">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {content || '*暂无内容*'}
            </ReactMarkdown>
          </div>
        ) : (
          <SimpleMdeReact value={content} onChange={handleContentChange} options={editorOptions} />
        )}
      </div>
      <style>{`
        .markdown-preview {
          padding: 16px 24px;
          line-height: 1.8;
          color: #1a1a1a;
        }
        .markdown-preview h1 { font-size: 26px; border-bottom: 1px solid #eee; padding-bottom: 8px; margin: 24px 0 16px; }
        .markdown-preview h2 { font-size: 22px; border-bottom: 1px solid #eee; padding-bottom: 6px; margin: 20px 0 14px; }
        .markdown-preview h3 { font-size: 18px; margin: 18px 0 12px; }
        .markdown-preview h4 { font-size: 16px; margin: 16px 0 10px; }
        .markdown-preview p { margin: 0 0 12px; }
        .markdown-preview ul, .markdown-preview ol { padding-left: 24px; margin: 0 0 12px; }
        .markdown-preview li { margin-bottom: 4px; }
        .markdown-preview code {
          background: #f5f5f5;
          padding: 2px 6px;
          border-radius: 3px;
          font-size: 0.9em;
          color: #d63384;
        }
        .markdown-preview pre {
          background: #1e1e1e;
          color: #d4d4d4;
          padding: 16px;
          border-radius: 6px;
          overflow-x: auto;
          margin: 0 0 16px;
        }
        .markdown-preview pre code {
          background: none;
          color: inherit;
          padding: 0;
          font-size: 14px;
        }
        .markdown-preview blockquote {
          border-left: 4px solid #1677ff;
          padding: 8px 16px;
          margin: 0 0 16px;
          background: #f6f9ff;
          color: #555;
        }
        .markdown-preview table {
          border-collapse: collapse;
          width: 100%;
          margin: 0 0 16px;
        }
        .markdown-preview th, .markdown-preview td {
          border: 1px solid #ddd;
          padding: 8px 12px;
          text-align: left;
        }
        .markdown-preview th {
          background: #fafafa;
          font-weight: 600;
        }
        .markdown-preview img {
          max-width: 100%;
          border-radius: 4px;
          margin: 8px 0;
        }
        .markdown-preview hr {
          border: none;
          border-top: 1px solid #eee;
          margin: 24px 0;
        }
      `}</style>
    </div>
  );
}

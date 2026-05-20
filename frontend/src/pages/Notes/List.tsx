import React, { useEffect, useState } from 'react';
import { Card, Button, List, Typography, Space, Tag, Popconfirm, message, Select } from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import request from '../../utils/request';
import { stripMarkdown } from '../../utils/markdown';

export default function NotesList() {
  const navigate = useNavigate();
  const [data, setData] = useState<any[]>([]);
  const [allTags, setAllTags] = useState<string[]>([]);
  const [selectedTag, setSelectedTag] = useState<string | undefined>(undefined);
  const [deleting, setDeleting] = useState<number | null>(null);

  useEffect(() => {
    loadNotes();
  }, []);

  const loadNotes = async (tag?: string) => {
    try {
      const params = tag ? { tags: [tag] } : undefined;
      const res: any = await request.get('/notes', { params });
      setData(Array.isArray(res) ? res : []);
    } catch (e) {
      console.error(e);
    }
  };

  // 从全量加载中收集所有标签做筛选
  const collectTags = async () => {
    try {
      const res: any = await request.get('/notes');
      const items = Array.isArray(res) ? res : [];
      const tags = new Set<string>();
      items.forEach((n: any) => n.tags?.forEach((t: string) => tags.add(t)));
      setAllTags(Array.from(tags).sort());
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    collectTags();
  }, []);

  useEffect(() => {
    loadNotes(selectedTag);
  }, [selectedTag]);

  const handleDelete = async (id: number) => {
    setDeleting(id);
    try {
      await request.delete(`/notes/${id}`);
      message.success('笔记已删除');
      setData(prev => prev.filter(n => n.id !== id));
    } catch (e: any) {
      message.error(e?.response?.data?.error || '删除失败');
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>全部笔记</Typography.Title>
        <Space>
          {allTags.length > 0 && (
            <Select
              allowClear
              placeholder="按标签筛选"
              style={{ width: 160 }}
              value={selectedTag}
              onChange={(val) => setSelectedTag(val)}
              options={allTags.map(t => ({ label: t, value: t }))}
            />
          )}
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/notes/new')}>
            新建笔记
          </Button>
        </Space>
      </div>
      <List
        grid={{ gutter: 16, xs: 1, sm: 2, md: 3 }}
        dataSource={data}
        locale={{ emptyText: '暂无笔记，点击"新建笔记"开始记录' }}
        renderItem={(item: any) => (
          <List.Item>
            <Card
              hoverable
              actions={[
                <EditOutlined key="edit" onClick={() => navigate(`/notes/${item.id}`)} />,
                <Popconfirm
                  key="delete"
                  title="确认删除"
                  description="删除后不可恢复，笔记的向量数据也将失效"
                  onConfirm={() => handleDelete(item.id)}
                  okText="删除"
                  cancelText="取消"
                  okButtonProps={{ danger: true }}
                >
                  <DeleteOutlined style={{ color: deleting === item.id ? undefined : '#ff4d4f' }} />
                </Popconfirm>,
              ]}
            >
              <div onClick={() => navigate(`/notes/${item.id}`)}>
                <Card.Meta
                  title={item.title}
                  description={
                    item.summary || (stripMarkdown(item.content)?.substring(0, 80) + '...') || '无内容'
                  }
                />
                <div style={{ marginTop: 12, minHeight: 24 }}>
                  {item.tags?.map((t: string) => (
                    <Tag key={t} color="blue" style={{ marginBottom: 4 }}
                      onClick={(e) => { e.stopPropagation(); setSelectedTag(t); }}>
                      {t}
                    </Tag>
                  ))}
                </div>
                <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 8 }}>
                  更新: {item.updatedAt ? new Date(item.updatedAt).toLocaleString('zh-CN') : '暂无'}
                </Typography.Text>
              </div>
            </Card>
          </List.Item>
        )}
      />
    </div>
  );
}

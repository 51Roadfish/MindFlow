import React, { useEffect, useState } from 'react';
import { Card, Button, List, Typography, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import request from '../../utils/request';

export default function NotesList() {
  const navigate = useNavigate();
  const [data, setData] = useState<any[]>([]);

  useEffect(() => {
    loadNotes();
  }, []);

  const loadNotes = async () => {
    try {
      const res: any = await request.get('/notes');
      setData(Array.isArray(res) ? res : []);
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
        <Typography.Title level={3}>全部笔记</Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/notes/new')}>新建笔记</Button>
      </div>
      <List
        grid={{ gutter: 16, xs: 1, sm: 2, md: 3 }}
        dataSource={data}
        renderItem={(item: any) => (
          <List.Item>
            <Card hoverable title={item.title} onClick={() => navigate(`/notes/${item.id}`)}>
              {item.summary && <p style={{ color: 'gray' }}>{item.summary}</p>}
              <p>更新时间：{item.updatedAt ? new Date(item.updatedAt).toLocaleString('zh-CN') : '暂无'}</p>
            </Card>
          </List.Item>
        )}
      />
    </div>
  );
}

import React, { useState, useEffect } from 'react';
import { Input, List, Typography } from 'antd';
import { useSearchParams } from 'react-router-dom';
import request from '../../utils/request';

export default function AISearch() {
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get('q') || '';
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (q) handleSearch(q);
  }, [q]);

  const handleSearch = async (val: string) => {
    if (!val) return;
    setSearchParams({ q: val });
    setLoading(true);
    try {
      const res: any = await request.post('/ai/search', { query: val });
      setResults(Array.isArray(res) ? res : res.content || []);
    } catch (e) {}
    setLoading(false);
  };

  return (
    <div>
      <Typography.Title level={3}>AI 语义搜索</Typography.Title>
      <Input.Search 
        size="large" 
        defaultValue={q} 
        onSearch={handleSearch} 
        placeholder="输入问题或关键词搜索笔记..." 
        loading={loading}
        style={{ marginBottom: 24 }}
      />
      <List
        loading={loading}
        dataSource={results}
        renderItem={(item: any) => (
          <List.Item>
            <List.Item.Meta
              title={item.title || item.metadata?.noteTitle || '无标题文档'}
              description={item.content || item.fragment}
            />
          </List.Item>
        )}
      />
    </div>
  );
}

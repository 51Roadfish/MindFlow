import React, { useState } from 'react';
import { Select, Button, Typography, Input, message, Card } from 'antd';
import request from '../../utils/request';

export default function AIWrite() {
  const [action, setAction] = useState('continue');
  const [content, setContent] = useState('');
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);

  const handleWrite = async () => {
    if (!content) return message.warning('请输入基础文本');
    setLoading(true);
    try {
      const res: any = await request.post('/ai/write', { action, content });
      setResult(res.result || res.answer || '');
    } catch (e) {}
    setLoading(false);
  };

  return (
    <div>
      <Typography.Title level={3}>AI 写作助手</Typography.Title>
      <div style={{ marginBottom: 16 }}>
        <Select value={action} onChange={setAction} style={{ width: 200, marginRight: 16 }}>
          <Select.Option value="continue">续写</Select.Option>
          <Select.Option value="polish">润色与修饰</Select.Option>
          <Select.Option value="summarize">提取摘要 / 总结</Select.Option>
        </Select>
        <Button type="primary" onClick={handleWrite} loading={loading}>开始处理</Button>
      </div>
      <Input.TextArea 
        rows={8} 
        value={content} 
        onChange={e => setContent(e.target.value)} 
        placeholder="请输入需要处理的原始内容..." 
        style={{ marginBottom: 24 }}
      />
      {result && (
        <Card title="生成结果">
          <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
            {result}
          </Typography.Paragraph>
        </Card>
      )}
    </div>
  );
}

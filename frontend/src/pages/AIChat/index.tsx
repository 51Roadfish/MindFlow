import React, { useState } from 'react';
import { Input, Button, List, Typography, Space, Spin } from 'antd';
import { RobotOutlined, UserOutlined, SendOutlined } from '@ant-design/icons';
import { useAuthStore } from '../../store';

const { Text } = Typography;

interface Message {
  role: 'user' | 'ai';
  content: string;
  sources?: string[];
}

const AIChat: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([
    { role: 'ai', content: '你好！我是 MindFlow AI 助手。我可以解答问题或帮您检索相关笔记内容。' }
  ]);
  const [inputVal, setInputVal] = useState('');
  const [loading, setLoading] = useState(false);

  const sendMessage = async () => {
    if (!inputVal.trim()) return;
    const userMsg = inputVal;
    setInputVal('');
    setMessages(prev => [...prev, { role: 'user', content: userMsg }]);

    // 先插入一条空的 AI 回复占位
    setMessages(prev => [...prev, { role: 'ai', content: '' }]);
    setLoading(true);

    let doneStreaming = false;
    let hasContent = false;

    try {
      const token = useAuthStore.getState().token;
      const response = await fetch('/api/ai/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ question: userMsg }),
      });

      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          useAuthStore.getState().clearAuth();
          window.location.href = '/login';
          return;
        }
        throw new Error(`请求失败: ${response.status}`);
      }

      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let answer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done || doneStreaming) break;

        buffer += decoder.decode(value, { stream: true });
        const frames = buffer.split('\n\n');
        buffer = frames.pop() || '';

        for (const frame of frames) {
          if (doneStreaming) break;
          const lines = frame.split('\n');
          for (const line of lines) {
            if (!line.startsWith('data:')) continue;
            const data = line.slice(5).trim();
            if (data === '[DONE]') { doneStreaming = true; break; }
            if (data.startsWith('[ERROR]')) throw new Error(data.slice(7));
            answer += data;
            hasContent = true;
            // 逐 token 更新 UI
            setMessages(prev => {
              const msgs = [...prev];
              if (msgs.length > 0) msgs[msgs.length - 1].content = answer;
              return msgs;
            });
          }
        }
      }
    } catch (e: any) {
      // 流结束后连接关闭可能触发无关错误，忽略
      if (doneStreaming || hasContent) {
        console.warn('SSE stream warning after content received:', e);
        return;
      }
      setMessages(prev => {
        const msgs = [...prev];
        if (msgs.length > 0) {
          msgs[msgs.length - 1].content = e?.message || '抱歉，当前模型请求出错。';
        }
        return msgs;
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ flex: 1, overflowY: 'auto', marginBottom: 20 }}>
        <List
          dataSource={messages}
          renderItem={(msg) => (
            <List.Item style={{ borderBottom: 'none', display: 'flex', justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start' }}>
              <Space align="start">
                {msg.role === 'ai' && <RobotOutlined style={{ fontSize: 24, marginTop: 4, color: '#1677ff' }} />}
                <div style={{ maxWidth: 600, background: msg.role === 'user' ? '#1677ff' : '#f5f5f5', color: msg.role === 'user' ? '#fff' : '#000', padding: '10px 16px', borderRadius: 8 }}>
                  <Text style={{ color: 'inherit', whiteSpace: 'pre-wrap' }}>{msg.content || (loading ? '思考中...' : '')}</Text>
                  {msg.sources && msg.sources.length > 0 && (
                     <div style={{ marginTop: 8, fontSize: 12, borderTop: '1px solid #d9d9d9', paddingTop: 4 }}>
                       来源引用: {msg.sources.join(', ')}
                     </div>
                  )}
                </div>
                {msg.role === 'user' && <UserOutlined style={{ fontSize: 24, marginTop: 4 }} />}
              </Space>
            </List.Item>
          )}
        />
        {loading && <div style={{ padding: 20 }}><Spin tip="AI正在思考中..." /></div>}
      </div>
      <Space.Compact style={{ width: '100%' }}>
        <Input
          value={inputVal}
          onChange={e => setInputVal(e.target.value)}
          onPressEnter={sendMessage}
          placeholder="有什么我可以帮您的？"
          size="large"
        />
        <Button size="large" type="primary" icon={<SendOutlined />} onClick={sendMessage} loading={loading}>发送</Button>
      </Space.Compact>
    </div>
  );
};

export default AIChat;

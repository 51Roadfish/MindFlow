import React, { useState } from 'react';
import { Form, Input, Button, Card, Alert, message } from 'antd';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../../store';
import request from '../../utils/request';

const ERROR_MAP: Record<string, string> = {
  'Bad credentials': '用户名或密码错误',
  'User is disabled': '该用户已被禁用',
  'User account is locked': '账户已锁定',
};

export default function Login() {
  const navigate = useNavigate();
  const setAuth = useAuthStore(s => s.setAuth);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: any) => {
    setError('');
    setLoading(true);
    try {
      const res: any = await request.post('/auth/login', values);
      setAuth(res.token || 'mock_token', values.username);
      message.success('登录成功');
      navigate('/');
    } catch (e: any) {
      const raw = e?.response?.data?.error || '';
      const msg = ERROR_MAP[raw] || raw || '登录失败，请检查网络或后端服务';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#f0f2f5' }}>
      <Card title="登录 MindFlow" style={{ width: 400 }}>
        <Form onFinish={onFinish} layout="vertical">
          {error && (
            <Alert
              message={error}
              type="error"
              showIcon
              closable
              onClose={() => setError('')}
              style={{ marginBottom: 16 }}
            />
          )}
          <Form.Item label="用户名" name="username" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true }]}>
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>登录</Button>
          <div style={{ marginTop: 16, textAlign: 'center' }}>
            <Link to="/register">没有账号？去注册</Link>
          </div>
        </Form>
      </Card>
    </div>
  );
}

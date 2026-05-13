import React from 'react';
import { Form, Input, Button, Card, message } from 'antd';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../../store';
import request from '../../utils/request';

export default function Login() {
  const navigate = useNavigate();
  const setAuth = useAuthStore(s => s.setAuth);
  
  const onFinish = async (values: any) => {
    try {
      const res: any = await request.post('/auth/login', values);
      setAuth(res.token || 'mock_token', values.username);
      message.success('登录成功');
      navigate('/');
    } catch (e) {
      // request.ts 已处理错误
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#f0f2f5' }}>
      <Card title="登录 MindFlow" style={{ width: 400 }}>
        <Form onFinish={onFinish} layout="vertical">
          <Form.Item label="用户名" name="username" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true }]}>
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>登录</Button>
          <div style={{ marginTop: 16, textAlign: 'center' }}>
            <Link to="/register">没有账号？去注册</Link>
          </div>
        </Form>
      </Card>
    </div>
  );
}

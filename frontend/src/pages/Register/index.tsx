import React from 'react';
import { Form, Input, Button, Card, message } from 'antd';
import { useNavigate, Link } from 'react-router-dom';
import request from '../../utils/request';

export default function Register() {
  const navigate = useNavigate();
  
  const onFinish = async (values: any) => {
    try {
      await request.post('/auth/register', values);
      message.success('注册成功，请登录');
      navigate('/login');
    } catch (e: any) {
      message.error(e.response?.data?.error || '注册失败，请重试');
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#f0f2f5' }}>
      <Card title="注册 MindFlow" style={{ width: 400 }}>
        <Form onFinish={onFinish} layout="vertical">
          <Form.Item label="用户名" name="username" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="邮箱" name="email" rules={[{ required: true, type: 'email' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true }]}>
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>注册</Button>
          <div style={{ marginTop: 16, textAlign: 'center' }}>
            <Link to="/login">已有账号？去登录</Link>
          </div>
        </Form>
      </Card>
    </div>
  );
}

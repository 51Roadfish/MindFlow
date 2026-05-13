import React from 'react';
import { Layout, Menu, Input, Dropdown, Space, Avatar } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { 
  FileTextOutlined, RobotOutlined, SearchOutlined, 
  EditOutlined, LogoutOutlined, UserOutlined 
} from '@ant-design/icons';
import { useAuthStore } from '../store';

const { Header, Sider, Content } = Layout;

const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { username, clearAuth } = useAuthStore();
  
  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key);
  };

  const menuItems = [
    { key: '/notes', icon: <FileTextOutlined />, label: '我的笔记' },
    { key: '/ai/chat', icon: <RobotOutlined />, label: 'AI 问答助手' },
    { key: '/ai/write', icon: <EditOutlined />, label: 'AI 写作助手' },
    { key: '/ai/search', icon: <SearchOutlined />, label: 'AI 语义搜索' },
  ];

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="light" style={{ borderRight: '1px solid #f0f0f0' }}>
        <div style={{ height: 60, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: 18, color: '#1677ff' }}>
          MindFlow
        </div>
        <Menu mode="inline" selectedKeys={[location.pathname]} items={menuItems} onClick={handleMenuClick} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ width: 400 }}>
             <Input.Search 
               placeholder="使用自然语言在全局搜索知识库" 
               onSearch={(val) => navigate(`/ai/search?q=${val}`)} 
             />
          </div>
          <Dropdown menu={{ items: [{ key: 'logout', label: '退出登录', icon: <LogoutOutlined />, onClick: handleLogout }] }}>
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} />
              {username || 'User'}
            </Space>
          </Dropdown>
        </Header>
        <Content style={{ margin: '24px', background: '#fff', padding: 24, borderRadius: 8, overflowY: 'auto' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;

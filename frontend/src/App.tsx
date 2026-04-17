import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Layout } from 'antd';

const { Header, Content, Footer } = Layout;

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Layout style={{ minHeight: '100vh' }}>
        <Header style={{ color: 'white' }}>MindFlow Knowledge Base</Header>
        <Content style={{ padding: '0 50px', marginTop: 16 }}>
          <div style={{ background: '#fff', padding: 24, minHeight: 600 }}>
            <h1>Welcome to MindFlow Platform</h1>
            <p>Your AI-Assisted Notebook.</p>
          </div>
        </Content>
        <Footer style={{ textAlign: 'center' }}>MindFlow ©2026 Created by User</Footer>
      </Layout>
    </BrowserRouter>
  );
};

export default App;

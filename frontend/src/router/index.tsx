import { createBrowserRouter, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import Login from '../pages/Login';
import Register from '../pages/Register';
import NotesList from '../pages/Notes/List';
import NoteDetail from '../pages/Notes/Detail';
import AIChat from '../pages/AIChat';
import AIWrite from '../pages/AIWrite';
import AISearch from '../pages/AISearch';
import { useAuthStore } from '../store';

const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const token = useAuthStore((state) => state.token);
  // 测试开发阶段如果没有接入后端登录，可以暂时取消拦截：return <>{children}</>;
  return token ? <>{children}</> : <Navigate to="/login" replace />;
};

export const router = createBrowserRouter([
  { path: '/login', element: <Login /> },
  { path: '/register', element: <Register /> },
  {
    path: '/',
    element: (
      <PrivateRoute>
        <MainLayout />
      </PrivateRoute>
    ),
    children: [
      { index: true, element: <Navigate to="/notes" replace /> },
      { path: 'notes', element: <NotesList /> },
      { path: 'notes/new', element: <NoteDetail isNew /> },
      { path: 'notes/:id', element: <NoteDetail /> },
      { path: 'ai/chat', element: <AIChat /> },
      { path: 'ai/write', element: <AIWrite /> },
      { path: 'ai/search', element: <AISearch /> },
    ],
  },
]);

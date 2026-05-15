import React, { useState, useEffect } from 'react';
import { Button, List, Typography, Space, Spin, Input, Tag, Card, Checkbox, message } from 'antd';
import { RobotOutlined, UserOutlined, SendOutlined, CheckCircleOutlined, ReloadOutlined, FileDoneOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import request from '../../utils/request';

const { Text } = Typography;

type StepStatus = 'selecting' | 'loading' | 'chat' | 'answering' | 'complete';

interface Question {
  questionId: string;
  question: string;
  expectedAnswer: string;
}

const Review: React.FC = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState<StepStatus>('selecting');
  const [notes, setNotes] = useState<any[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState<Question | null>(null);
  const [answer, setAnswer] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [totalQ, setTotalQ] = useState(0);
  const [answeredQ, setAnsweredQ] = useState(0);
  const [totalScore, setTotalScore] = useState(0);
  const [maxQ, setMaxQ] = useState(20);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [lastScore, setLastScore] = useState<number | null>(null);
  const [summary, setSummary] = useState<string | null>(null);
  const [history, setHistory] = useState<{ question: string; answer: string; score: number; feedback: string }[]>([]);

  useEffect(() => {
    loadNotes();
  }, []);

  const loadNotes = async () => {
    try {
      const res: any = await request.get('/notes');
      setNotes(Array.isArray(res) ? res : []);
    } catch (e) {
      console.error(e);
    }
  };

  const startReview = async () => {
    if (selectedIds.length === 0) return;
    setStep('loading');
    setLoading(true);
    try {
      const res: any = await request.post('/review/start', { noteIds: selectedIds });
      setSessionId(res.id);
      setCurrentQuestion(res.currentQuestion);
      setTotalQ(res.totalQuestions);
      setAnsweredQ(res.answeredQuestions);
      setTotalScore(res.totalScore);
      setMaxQ(res.maxQuestions || 20);
      setStep('chat');
    } catch (e: any) {
      const msg = e?.message || e?.response?.data?.message || '启动复习失败';
      message.error(msg);
      setStep('selecting');
    } finally {
      setLoading(false);
    }
  };

  const submitAnswer = async () => {
    if (!answer.trim() || !sessionId || !currentQuestion) return;
    setSubmitting(true);
    try {
      const res: any = await request.post(`/review/${sessionId}/answer`, {
        questionId: currentQuestion.questionId,
        answer: answer.trim(),
      });

      setLastScore(res.score);
      setFeedback(res.feedback);
      setAnsweredQ(prev => prev + 1);
      setTotalScore(prev => prev + (res.score || 0));

      setHistory(prev => [...prev, {
        question: currentQuestion.question,
        answer: answer.trim(),
        score: res.score,
        feedback: res.feedback,
      }]);

      if (res.nextAction === 'complete') {
        setSummary(res.summary || '复习结束！');
        setStep('complete');
      } else if (res.nextQuestion) {
        setCurrentQuestion(res.nextQuestion);
        setTotalQ(res.nextQuestion ? totalQ + 1 : totalQ);
      }

      setAnswer('');
    } catch (e: any) {
      const msg = e?.message || e?.response?.data?.message || '提交答案失败';
      message.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const generateWeakExam = async () => {
    if (!sessionId) return;
    try {
      const res: any = await request.post(`/review/${sessionId}/exam`);
      message.success('针对性试卷生成成功！');
      navigate('/ai/exam');
    } catch (e: any) {
      message.error(e?.response?.data?.message || '生成试卷失败');
    }
  };

  const endSession = async () => {
    if (!sessionId) return;
    try {
      await request.post(`/review/${sessionId}/end`);
    } catch (e) { /* ignore */ }
    resetAll();
  };

  const resetAll = () => {
    setStep('selecting');
    setSessionId(null);
    setCurrentQuestion(null);
    setAnswer('');
    setFeedback(null);
    setLastScore(null);
    setSummary(null);
    setTotalQ(0);
    setAnsweredQ(0);
    setTotalScore(0);
    setHistory([]);
  };

  const renderNoteSelector = () => (
    <div>
      <Typography.Title level={4}>选择要复习的笔记</Typography.Title>
      <Checkbox.Group style={{ width: '100%' }} onChange={(vals) => setSelectedIds(vals as number[])}>
        <List
          dataSource={notes}
          renderItem={(item: any) => (
            <List.Item>
              <Checkbox value={item.id}>
                <div>
                  <Text strong>{item.title || '无标题'}</Text>
                  {item.summary && <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>{item.summary}</Text>}
                </div>
              </Checkbox>
            </List.Item>
          )}
        />
      </Checkbox.Group>
      <Space style={{ marginTop: 20 }}>
        <Button type="primary" size="large" onClick={startReview} disabled={selectedIds.length === 0}>
          开始复习 ({selectedIds.length} 篇笔记)
        </Button>
        <Button onClick={loadNotes} icon={<ReloadOutlined />}>刷新笔记列表</Button>
      </Space>
    </div>
  );

  const renderChat = () => (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* 进度信息 */}
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Tag color="blue">进度 {answeredQ}/{totalQ}</Tag>
          <Tag color="green">得分 {totalScore}</Tag>
          <Tag color="orange">上限 {maxQ} 题</Tag>
          <Button size="small" danger onClick={endSession}>结束复习</Button>
        </Space>
      </div>

      {/* 历史问答记录 */}
      <div style={{ flex: 1, overflowY: 'auto', marginBottom: 16 }}>
        {history.map((h, i) => (
          <Card key={i} size="small" style={{ marginBottom: 12 }}>
            <div style={{ marginBottom: 4 }}><Text type="secondary">问:</Text> {h.question}</div>
            <div style={{ marginBottom: 4 }}><Text type="secondary">答:</Text> {h.answer}</div>
            <div style={{ marginBottom: 4 }}>
              <Text type="secondary">评分: </Text>
              <Text style={{ color: h.score >= 60 ? '#52c41a' : '#ff4d4f' }}>{h.score} 分</Text>
            </div>
            <div><Text type="secondary">反馈:</Text> {h.feedback}</div>
          </Card>
        ))}

        {/* 当前问题 */}
        {currentQuestion && (
          <div style={{
            background: '#f6ffed',
            border: '1px solid #b7eb8f',
            borderRadius: 8,
            padding: '16px 20px',
            marginBottom: 12,
          }}>
            <Space align="start">
              <RobotOutlined style={{ fontSize: 24, color: '#52c41a', marginTop: 4 }} />
              <div>
                <Text style={{ fontSize: 16, fontWeight: 500 }}>{currentQuestion.question}</Text>
              </div>
            </Space>
          </div>
        )}
      </div>

      {/* 输入区域 */}
      <div>
        {feedback && (
          <div style={{
            background: '#fff7e6',
            border: '1px solid #ffd591',
            borderRadius: 8,
            padding: '12px 16px',
            marginBottom: 12,
          }}>
            <Text strong style={{ color: lastScore !== null && lastScore >= 60 ? '#52c41a' : '#fa8c16' }}>
              得分: {lastScore}
            </Text>
            <Text style={{ marginLeft: 12 }}>{feedback}</Text>
          </div>
        )}
        <Space.Compact style={{ width: '100%' }}>
          <Input.TextArea
            value={answer}
            onChange={e => setAnswer(e.target.value)}
            placeholder="输入你的答案..."
            rows={2}
            disabled={submitting}
            onPressEnter={(e) => { if (!e.shiftKey) { e.preventDefault(); submitAnswer(); } }}
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={submitAnswer}
            loading={submitting}
            disabled={!answer.trim()}
            style={{ height: 'auto' }}
          >
            提交
          </Button>
        </Space.Compact>
      </div>
    </div>
  );

  const renderComplete = () => (
    <div style={{ textAlign: 'center', padding: '40px 0' }}>
      <CheckCircleOutlined style={{ fontSize: 64, color: '#52c41a', marginBottom: 20 }} />
      <Typography.Title level={3}>复习完成！</Typography.Title>
      <Card style={{ maxWidth: 500, margin: '0 auto 24px' }}>
        <Space direction="vertical" size="middle">
          <div><Text type="secondary">总计答题：</Text><Text strong>{answeredQ} 题</Text></div>
          <div><Text type="secondary">总分：</Text><Text strong>{totalScore} 分</Text></div>
          <div><Text type="secondary">平均分：</Text>
            <Text strong style={{ color: answeredQ > 0 && totalScore / answeredQ >= 60 ? '#52c41a' : '#ff4d4f' }}>
              {answeredQ > 0 ? Math.round(totalScore / answeredQ) : 0} 分
            </Text>
          </div>
          {summary && (
            <div style={{ background: '#f0f5ff', padding: '12px 16px', borderRadius: 8 }}>
              <Text>{summary}</Text>
            </div>
          )}
        </Space>
      </Card>
      <Space>
        <Button type="primary" size="large" onClick={generateWeakExam} icon={<FileDoneOutlined />}>
          生成针对性试卷
        </Button>
        <Button size="large" onClick={resetAll}>再来一次</Button>
        <Button size="large" onClick={() => window.history.back()}>返回</Button>
      </Space>
    </div>
  );

  if (step === 'loading') {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin size="large" tip="AI 正在生成题目..." />
      </div>
    );
  }

  if (step === 'chat') {
    return renderChat();
  }

  if (step === 'complete') {
    return renderComplete();
  }

  return renderNoteSelector();
};

export default Review;

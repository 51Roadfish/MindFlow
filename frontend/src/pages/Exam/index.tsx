import React, { useState, useEffect } from 'react';
import { Button, List, Typography, Space, Spin, Card, Tag, Checkbox, Divider, message } from 'antd';
import { FileTextOutlined, ReloadOutlined, CheckCircleOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import request from '../../utils/request';

const { Text, Title } = Typography;

type StepStatus = 'selecting' | 'generating' | 'display';

interface ExamQuestion {
  id: string;
  type: string;
  question: string;
  options?: string[];
  answer: string;
  points: number;
}

interface ExamPaper {
  id: number;
  title: string;
  questionCount: number;
  questions: ExamQuestion[];
  createdAt: string;
}

const Exam: React.FC = () => {
  const [step, setStep] = useState<StepStatus>('selecting');
  const [notes, setNotes] = useState<any[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [examPapers, setExamPapers] = useState<ExamPaper[]>([]);
  const [currentExam, setCurrentExam] = useState<ExamPaper | null>(null);
  const [loading, setLoading] = useState(false);
  const [showAnswers, setShowAnswers] = useState<Record<string, boolean>>({});
  const [generating, setGenerating] = useState(false);

  useEffect(() => {
    loadNotes();
    loadExamList();
  }, []);

  const loadNotes = async () => {
    try {
      const res: any = await request.get('/notes');
      setNotes(Array.isArray(res) ? res : []);
    } catch (e) {
      console.error(e);
    }
  };

  const loadExamList = async () => {
    try {
      const res: any = await request.get('/review/exam/list');
      setExamPapers(Array.isArray(res) ? res : []);
    } catch (e) {
      console.error(e);
    }
  };

  const generateExam = async () => {
    if (selectedIds.length === 0) return;
    setStep('generating');
    setGenerating(true);
    try {
      const res: any = await request.post('/review/exam/generate', {
        noteIds: selectedIds,
        questionCount: 10,
      });
      setCurrentExam(res);
      setStep('display');
    } catch (e: any) {
      const msg = e?.message || e?.response?.data?.message || '生成试卷失败';
      message.error(msg);
      setStep('selecting');
    } finally {
      setGenerating(false);
    }
  };

  const viewExam = async (id: number) => {
    setLoading(true);
    try {
      const res: any = await request.get(`/review/exam/${id}`);
      setCurrentExam(res);
      setStep('display');
    } catch (e: any) {
      message.error('加载试卷失败');
    } finally {
      setLoading(false);
    }
  };

  const toggleAnswer = (qId: string) => {
    setShowAnswers(prev => ({ ...prev, [qId]: !prev[qId] }));
  };

  const resetSelection = () => {
    setStep('selecting');
    setCurrentExam(null);
    setShowAnswers({});
  };

  const renderNoteSelector = () => (
    <div>
      <Title level={4}>选择笔记生成试卷</Title>

      <Divider orientation="left">已有试卷</Divider>
      {examPapers.length > 0 && (
        <List
          size="small"
          style={{ marginBottom: 20 }}
          dataSource={examPapers.slice(0, 5)}
          renderItem={(paper: ExamPaper) => (
            <List.Item
              actions={[
                <Button type="link" onClick={() => viewExam(paper.id)}>查看</Button>
              ]}
            >
              <List.Item.Meta
                title={paper.title || `试卷 #${paper.id}`}
                description={`${paper.questionCount} 题 · ${paper.createdAt ? new Date(paper.createdAt).toLocaleString('zh-CN') : ''}`}
              />
            </List.Item>
          )}
        />
      )}

      <Divider orientation="left">选择笔记</Divider>
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
        <Button type="primary" size="large" onClick={generateExam} disabled={selectedIds.length === 0} loading={generating}>
          生成试卷 ({selectedIds.length} 篇笔记)
        </Button>
        <Button onClick={() => { loadNotes(); loadExamList(); }} icon={<ReloadOutlined />}>刷新</Button>
      </Space>
    </div>
  );

  const renderExamPaper = () => {
    if (!currentExam) return null;
    const { title, questions, questionCount, createdAt } = currentExam;

    return (
      <div>
        <div style={{ marginBottom: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={4} style={{ margin: 0 }}>{title || 'AI 考试'}</Title>
            <Text type="secondary">{questionCount} 题 · {createdAt ? new Date(createdAt).toLocaleString('zh-CN') : ''}</Text>
          </div>
          <Space>
            <Button onClick={() => setShowAnswers(
              Object.fromEntries(questions?.map(q => [q.id, true]) || [])
            )} icon={<EyeOutlined />}>全部显示答案</Button>
            <Button onClick={() => setShowAnswers({})} icon={<EyeInvisibleOutlined />}>全部隐藏答案</Button>
            <Button onClick={resetSelection}>返回</Button>
          </Space>
        </div>

        {questions && questions.map((q, idx) => (
          <Card
            key={q.id}
            size="small"
            style={{ marginBottom: 12 }}
            title={
              <Space>
                <Tag color="blue">第 {idx + 1} 题</Tag>
                <Tag color={q.type === 'multiple_choice' ? 'purple' : 'orange'}>
                  {q.type === 'multiple_choice' ? '选择题' : '简答题'}
                </Tag>
                <Text type="secondary">{q.points} 分</Text>
              </Space>
            }
          >
            <div style={{ marginBottom: 12, fontSize: 15 }}>{q.question}</div>

            {q.options && q.options.length > 0 && (
              <div style={{ marginBottom: 12, paddingLeft: 16 }}>
                {q.options.map((opt, oi) => (
                  <div key={oi} style={{ marginBottom: 4 }}>
                    <Text>{String.fromCharCode(65 + oi)}. {opt}</Text>
                  </div>
                ))}
              </div>
            )}

            <Button
              type="dashed"
              size="small"
              icon={showAnswers[q.id] ? <EyeInvisibleOutlined /> : <EyeOutlined />}
              onClick={() => toggleAnswer(q.id)}
            >
              {showAnswers[q.id] ? '隐藏答案' : '显示答案'}
            </Button>

            {showAnswers[q.id] && (
              <div style={{
                marginTop: 12,
                padding: 12,
                background: '#f6ffed',
                border: '1px solid #b7eb8f',
                borderRadius: 6,
              }}>
                <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
                <Text strong>参考答案：</Text>
                <Text>{q.answer}</Text>
              </div>
            )}
          </Card>
        ))}
      </div>
    );
  };

  if (step === 'generating') {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin size="large" tip="AI 正在生成试卷..." />
      </div>
    );
  }

  if (step === 'display') {
    if (loading) {
      return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;
    }
    return renderExamPaper();
  }

  return renderNoteSelector();
};

export default Exam;

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listMyConversations } from '../services/directChatApi';
import type { DirectConversationSummary } from '../models/directConversation';

export default function ConversationListPage() {
  const navigate = useNavigate();
  const [conversations, setConversations] = useState<DirectConversationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listMyConversations()
      .then(setConversations)
      .catch((e) => setError(`Lỗi tải danh sách: ${e}`))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div>Đang tải...</div>;
  if (error) return <div>{error}</div>;

  return (
    <div>
      <h1>Trò chuyện</h1>
      {conversations.length === 0 ? (
        <p>Chưa có cuộc trò chuyện nào.</p>
      ) : (
        <ul>
          {conversations.map((c) => (
            <li key={c.conversationId}>
              <button onClick={() => navigate(`/direct-chats/${c.conversationId}`)}>
                {c.counterpartRole === 'MOTHER' ? 'Mẹ' : 'Chuyên gia'}
                {!c.expertAvailable && ' — Chuyên gia hiện không khả dụng'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

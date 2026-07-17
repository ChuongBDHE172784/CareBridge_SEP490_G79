import { useParams } from 'react-router-dom';
import ChatPanel from '../components/ChatPanel';

export default function ConversationRoomPage() {
  const { conversationId } = useParams<{ conversationId: string }>();
  if (!conversationId) return <div>Không tìm thấy cuộc trò chuyện.</div>;
  return <ChatPanel conversationId={conversationId} />;
}

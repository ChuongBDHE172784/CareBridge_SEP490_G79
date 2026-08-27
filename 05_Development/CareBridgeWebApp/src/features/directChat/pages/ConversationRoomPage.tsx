import { useParams } from 'react-router-dom';
import ChatPanel from '../components/ChatPanel';

export default function ConversationRoomPage() {
  const { conversationId } = useParams<{ conversationId: string }>();

  if (!conversationId) {
    return (
      <div className="p-8 font-sans text-center text-on-surface-variant">
        <h2 className="text-xl font-bold text-primary mb-2">Cuộc trò chuyện không tồn tại</h2>
        <p className="text-sm text-outline">Không tìm thấy mã cuộc trò chuyện được yêu cầu.</p>
      </div>
    );
  }

  return (
    <div className="p-8 font-sans space-y-6">
      <ChatPanel conversationId={conversationId} />
    </div>
  );
}

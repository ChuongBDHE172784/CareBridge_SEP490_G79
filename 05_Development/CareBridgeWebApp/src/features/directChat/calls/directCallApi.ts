import type { ConversationEventSignal } from '../../../shared/integrations/firebaseRealtime/conversationEventSignal';
import type { ConversationCall, ZegoJoinCredentials } from '../models/directConversation';
import * as directChatApi from '../services/directChatApi';

export interface DirectCallApiPort {
  initiate(conversationId: string, callType: 'VOICE' | 'VIDEO'): Promise<ConversationCall>;
  getCall(conversationId: string, callId: string): Promise<ConversationCall>;
  listActiveCalls(): Promise<ConversationCall[]>;
  markRinging(conversationId: string, callId: string): Promise<ConversationCall>;
  answer(conversationId: string, callId: string): Promise<ConversationCall>;
  decline(conversationId: string, callId: string): Promise<ConversationCall>;
  end(conversationId: string, callId: string): Promise<ConversationCall>;
  issueJoinCredentials(
    conversationId: string,
    callId: string
  ): Promise<ZegoJoinCredentials>;
}

export interface DirectCallSignalSource {
  subscribe(listener: (signal: ConversationEventSignal) => void): () => void;
}

export const directCallApi: DirectCallApiPort = {
  initiate: directChatApi.initiateCall,
  getCall: directChatApi.getCall,
  listActiveCalls: directChatApi.listActiveCalls,
  markRinging: directChatApi.markRinging,
  answer: directChatApi.answerCall,
  decline: directChatApi.declineCall,
  end: directChatApi.endCall,
  issueJoinCredentials: directChatApi.issueJoinCredentials,
};

import { createContext, useContext } from 'react';

export interface DirectCallContextValue {
  initiate(conversationId: string, callType: 'VOICE' | 'VIDEO'): Promise<void>;
}

export const DirectCallContext = createContext<DirectCallContextValue | null>(null);

export function useDirectCall(): DirectCallContextValue {
  const value = useContext(DirectCallContext);
  if (!value) throw new Error('DirectCallProvider is not mounted');
  return value;
}

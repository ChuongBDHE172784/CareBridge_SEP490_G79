import apiClient from '../../../shared/api/apiClient';

// BR-DCC-013: server derives the uid strictly from the caller's own JWT — this client
// never sends a target user id, there is no field to send one in.
export async function fetchFirebaseCustomToken(): Promise<string> {
  const { data } = await apiClient.post('/api/v1/firebase/custom-token');
  return data.data.firebaseCustomToken as string;
}

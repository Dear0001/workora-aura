const CHANNEL_NAME = 'workora-email-verification';
const STORAGE_KEY = 'workora-email-verification-event';
const COMPLETED_MESSAGE = 'verification-complete';

export function notifyEmailVerificationComplete(): void {
  if (typeof window === 'undefined') return;

  if (typeof BroadcastChannel !== 'undefined') {
    try {
      const channel = new BroadcastChannel(CHANNEL_NAME);
      channel.postMessage(COMPLETED_MESSAGE);
      channel.close();
      return;
    } catch (error) {
      console.warn('BroadcastChannel email verification notification failed.', error);
    }
  }

  try {
    window.localStorage.setItem(STORAGE_KEY, `${Date.now()}:${Math.random()}`);
  } catch (error) {
    console.warn('Cross-tab email verification notification is unavailable.', error);
  }
}

export function subscribeToEmailVerificationComplete(onComplete: () => void): () => void {
  if (typeof window === 'undefined') return () => undefined;

  let channel: BroadcastChannel | undefined;
  if (typeof BroadcastChannel !== 'undefined') {
    try {
      channel = new BroadcastChannel(CHANNEL_NAME);
    } catch (error) {
      console.warn('BroadcastChannel email verification listener failed.', error);
    }
  }

  const handleMessage = (event: MessageEvent<unknown>) => {
    if (event.data === COMPLETED_MESSAGE) onComplete();
  };
  channel?.addEventListener('message', handleMessage);

  const handleStorage = (event: StorageEvent) => {
    if (event.key === STORAGE_KEY && event.newValue !== null) onComplete();
  };

  window.addEventListener('storage', handleStorage);
  return () => {
    channel?.removeEventListener('message', handleMessage);
    channel?.close();
    window.removeEventListener('storage', handleStorage);
  };
}

/*
 * CareBridge Flutter Web Firebase Messaging worker.
 * Firebase Web configuration is public application metadata; credentials and
 * VAPID private material must never be placed in this file.
 */
importScripts(
  'https://www.gstatic.com/firebasejs/10.13.2/firebase-app-compat.js',
);
importScripts(
  'https://www.gstatic.com/firebasejs/10.13.2/firebase-messaging-compat.js',
);

firebase.initializeApp({
  apiKey: 'AIzaSyBfRpZazA7I-Hx6RxYCEIjeTkXpqbwmVVY',
  appId: '1:772548995876:web:570bb88680e8f883f261b1',
  messagingSenderId: '772548995876',
  projectId: 'project-d04b488f-17fb-4ae5-b64',
  authDomain: 'project-d04b488f-17fb-4ae5-b64.firebaseapp.com',
  storageBucket: 'project-d04b488f-17fb-4ae5-b64.firebasestorage.app',
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  // Notification payloads are displayed by the Firebase SDK. Rendering them
  // again here would produce duplicate browser notifications. Data-only
  // messages need the explicit fallback below.
  if (payload.notification) return;

  const data = payload.data || {};
  const title = data.title || 'CareBridge reminder';
  const options = {
    body: data.body || 'You have a scheduled item to review.',
    icon: '/icons/Icon-192.png',
    data,
  };
  return self.registration.showNotification(title, options);
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const notificationData = event.notification.data || {};
  const fcmData = notificationData.FCM_MSG?.data || {};
  const route = notificationData.route || fcmData.route;
  if (!route || !route.startsWith('/')) return;

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(
      (clients) => {
        const target = new URL(route, self.location.origin).href;
        for (const client of clients) {
          if ('focus' in client) {
            client.navigate(target);
            return client.focus();
          }
        }
        return self.clients.openWindow(target);
      },
    ),
  );
});

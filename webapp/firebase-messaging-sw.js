importScripts('https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js');
importScripts('https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging.js');

firebase.initializeApp({
  apiKey: "YOUR_API_KEY",
  authDomain: "shiftanddrift.firebaseapp.com",
  projectId: "shiftanddrift",
  messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
  appId: "YOUR_APP_ID"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage(payload => {
  console.log('[firebase-messaging-sw.js] Messaggio ricevuto:', payload);
  self.registration.showNotification(payload.notification.title, {
    body: payload.notification.body,
  });
});

importScripts('https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js');
importScripts('https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging.js');

firebase.initializeApp({
  apiKey: "AIzaSyAwSsoQSRoFH0DBtMDEG75CL9Z6-oRhZ00",
  authDomain: "shiftanddrift.firebaseapp.com",
  projectId: "shiftanddrift",
  messagingSenderId: "332080935366",
  appId: "1:332080935366:android:23467af08e9139644d02c7"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage(payload => {
  console.log('[firebase-messaging-sw.js] Messaggio ricevuto:', payload);
  self.registration.showNotification(payload.notification.title, {
    body: payload.notification.body,
  });
});

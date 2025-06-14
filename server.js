const express = require('express');
const admin = require('firebase-admin');

const app = express();
const port = process.env.PORT || 3000;

// Initialize Firebase Admin SDK from env variable
const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://shiftanddrift-c3fd6-default-rtdb.europe-west1.firebasedatabase.app"
});

app.get('/check-and-notify', async (req, res) => {
  const db = admin.database();
  const gamesSnap = await db.ref('games').once('value');
  const messages = [];

  gamesSnap.forEach((gameSnap) => {
    const game = gameSnap.val();
    const gameCode = gameSnap.key;

    if (game.status === 'rolling') {
      const players = game.players || {};
      for (const uid in players) {
        const player = players[uid];
        if (!player.isBot && (!player.roll || player.roll < 0)) {
          messages.push(
            db.ref(`/tokens/${uid}`).once('value').then((tokenSnap) => {
              const token = tokenSnap.val();
              if (token) {
                return admin.messaging().send({
                  token,
                  notification: {
                    title: 'È il tuo turno!',
                    body: 'Tira il dado per la griglia di partenza!',
                  },
                });
              }
              return null;
            })
          );
        }
      }
    }
  });

  await Promise.all(messages);
  res.send('Notifiche inviate (se presenti).');
});

app.listen(port, () => {
  console.log(`FCM Server listening on port ${port}`);
});

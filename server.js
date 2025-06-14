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

// Mappa degli stati del gioco -> testo della notifica
const notificationMessages = {
  rolling: {
    title: 'Preparazione gara!',
    body: 'La gara sta per cominciare, determinazione griglia di partenza!',
  },
  started: {
    title: 'La gara è iniziata!',
    body: 'Dai il massimo in pista!',
  },
  finished: {
    title: 'Gara conclusa',
    body: 'Scopri la classifica finale!',
  },
  // Stato di default se non specificato
  default: {
    title: 'Aggiornamento gioco',
    body: 'C’è un aggiornamento sul tuo gioco!',
  }
};

app.get('/check-and-notify', async (req, res) => {
  const gameCode = req.query.gameCode;
  if (!gameCode) {
    return res.status(400).send('Parametro gameCode mancante');
  }

  const db = admin.database();
  const gameSnap = await db.ref(`games/${gameCode}`).once('value');

  if (!gameSnap.exists()) {
    return res.status(404).send('Gioco non trovato');
  }

  const game = gameSnap.val();
  const players = game.players || {};
  const status = game.status || 'default';

  const notification = notificationMessages[status] || notificationMessages.default;

  const messages = Object.entries(players).map(async ([uid, player]) => {
    if (!player.isBot) {
      const tokenSnap = await db.ref(`/tokens/${uid}`).once('value');
      const token = tokenSnap.val();
      if (token) {
        return admin.messaging().send({
          token,
          notification
        });
      }
    }
    return null;
  });

  await Promise.all(messages);
  res.send(`Notifiche inviate per il gioco ${gameCode} (${status}).`);
});

app.listen(port, () => {
  console.log(`FCM Server listening on port ${port}`);
});


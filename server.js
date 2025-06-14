const express = require('express');
const admin = require('firebase-admin');

const app = express();
const port = process.env.PORT || 3000;

// Initialize Firebase Admin SDK
const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://shiftanddrift-c3fd6-default-rtdb.europe-west1.firebasedatabase.app"
});

// Messaggi di notifica associati allo stato del gioco
const notificationMessages = {
  rolling: {
    title: 'Preparazione gara!',
    body: 'La gara sta per cominciare, determinazione griglia di partenza!',
    click_action: 'ROLLING_ACTIVITY'
  },
  started: {
    title: 'La gara è iniziata!',
    body: 'Dai il massimo in pista!',
    click_action: 'GAME_ACTIVITY'
  },
  finished: {
    title: 'Gara conclusa',
    body: 'Scopri la classifica finale!',
    click_action: 'SUMMARY_ACTIVITY'
  },
  default: {
    title: 'Aggiornamento gioco',
    body: 'C’è un aggiornamento sul tuo gioco!',
    click_action: 'MAIN_ACTIVITY'
  }
};

app.get('/check-and-notify', async (req, res) => {
  const gameCode = req.query.gameCode;
  const fun = req.query.fun;
  const player = req.query.player;

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

  // Determina messaggio da inviare
  let notification = notificationMessages[status] || notificationMessages.default;

  // Se funzione è "newPlayer", personalizza la notifica
  if (fun === 'newPlayer' && player) {
    notification = {
      title: 'Nuovo giocatore!',
      body: `${player} si è aggiunto alla gara`,
      click_action: 'LOBBY_ACTIVITY'
    };
  }
  if (fun === 'deletePlayer' && player) {
    notification = {
      title: 'Giocatore uscito!',
      body: `${player} ha abbandonato la gara`,
      click_action: 'LOBBY_ACTIVITY'
    };
  }

  const messages = Object.entries(players).map(async ([uid, p]) => {
    if (!p.isBot) {
      const tokenSnap = await db.ref(`/tokens/${uid}`).once('value');
      const token = tokenSnap.val();
      if (token) {
        return admin.messaging().send({
          token,
          notification: {
            title: notification.title,
            body: notification.body,
            click_action: notification.click_action
          },
          data: {
            gameCode,
            target: notification.click_action,
            fun: fun || '',
            player: player || ''
          }
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




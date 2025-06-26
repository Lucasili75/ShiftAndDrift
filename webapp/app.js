const messaging = firebase.messaging();

document.getElementById("register").addEventListener("click", async () => {
  const uid = document.getElementById("uid").value.trim();

  if (!uid) {
    alert("Inserisci uno userId valido");
    return;
  }

  try {
    const token = await messaging.getToken({
      vapidKey: "BNDKTXEWE1-SPEZ-gc4dmgX7fRUae820aGiH9o_4WdA3KkRikUUwuMWQooC0odL4SCmmbnVLgT5rHnoICuPFO00"
    });

    console.log("Token generato:", token);

    // Salva il token nel database
    await firebase.database().ref("tokens/" + uid).set(token);

    alert("✅ Registrato! Ora puoi ricevere notifiche FCM.");
  } catch (error) {
    console.error("Errore nel recupero token:", error);
    alert("❌ Registrazione fallita. Vedi console.");
  }
});

// Ricezione in foreground
messaging.onMessage(payload => {
  console.log("📬 Messaggio ricevuto:", payload);
  alert(`🔔 ${payload.notification.title}: ${payload.notification.body}`);
});

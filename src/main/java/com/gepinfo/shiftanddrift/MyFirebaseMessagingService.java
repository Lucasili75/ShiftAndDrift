package com.gepinfo.shiftanddrift;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private DatabaseReference gamesRef;
    private final static String TAG = "MyFirebaseMessagingService";

    @Override
    public final void handleIntent(Intent intent) {
        Log.d(TAG, "FG=:" + isAppInForeground() + "FCM handleIntent! " + intent.getExtras());
        if (!isAppInForeground()) {
            showNotification(intent.getStringExtra("title"), intent.getStringExtra("body"), intent.getStringExtra("target"), intent.getStringExtra("gameCode"));
            return;
        }
        super.handleIntent(intent);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Salva il token nel tuo database (Firebase Realtime DB o Firestore)
        Log.d(TAG, "New Token: " + token);
        gamesRef = MyApplication.getGamesRef().child("token");
        gamesRef.child(MyApplication.getUid()).setValue(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        Log.d(TAG, "FCM Message Received! " + msg);

        String title = msg.getNotification() != null ? msg.getNotification().getTitle() : msg.getData().get("title");
        String body = msg.getNotification() != null ? msg.getNotification().getBody() : msg.getData().get("body");
        String target = msg.getData().get("target");
        String gameCode = msg.getData().get("gameCode");

        if ((!isAppInForeground()) || (MyApplication.getCurrentActivity().getClass() != getActivityFromTarget(target)))
            showNotification(title, body, target, gameCode);
    }

    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return false;

        final String packageName = getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private Class getActivityFromTarget(String target) {
        switch (target) {
            case "GAME_ACTIVITY":
                return CurrentGameActivity.class;
            case "WAIT_ACTIVITY":
                return GameActivity.class;
            case "ROLLING_ACTIVITY":
                return GridRollActivity.class;
            default:
                return MainActivity.class;
        }
    }

    private void showNotification(String title, String body, String target, String gameCode) {
        Intent intent = new Intent(this, getActivityFromTarget(target));
        Log.d(TAG, getActivityFromTarget(target).toString());

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("gameCode", gameCode);
        intent.putExtra("uid", MyApplication.getUid());
        intent.putExtra("playerName", MainActivity.playerName);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default_channel")
                .setSmallIcon(R.drawable.shift_and_drift_app)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify(1001, builder.build());
    }
}

package com.gepinfo.shiftanddrift;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {

    private static DatabaseReference gamesRef = null;
    private static FirebaseAuth auth = null;

    private static Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "default_channel",
                    "Notifiche generali",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canale predefinito per notifiche");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityResumed(Activity activity) {
                currentActivity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (currentActivity == activity) {
                    currentActivity = null;
                }
            }

            // Altri metodi non necessari per questo scopo
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public static DatabaseReference getGamesRef() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
            auth.signInAnonymously().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e("auth","ERROR AUTHENTICATION FAILED!");
                }
            });
        }
        if (gamesRef == null) {
            gamesRef = FirebaseDatabase.getInstance("https://shiftanddrift-c3fd6-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        }
        return gamesRef;
    }

    public static String getUid(){
        if(auth!=null){
            return auth.getCurrentUser().getUid();
        }
        Log.e("UID","ERROR GETTING UID! auth=null!");
        return "";
    }
}

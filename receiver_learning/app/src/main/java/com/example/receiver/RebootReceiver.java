package com.example.receiver;

import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.util.Log;

public class RebootReceiver extends BroadcastReceiver {
    private final static String TAG = "RebootReceiver";
    private final static String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive with action[" + action + "]");
        if (action.equals(ACTION_BOOT_COMPLETED)) {
            Log.i(TAG, "get ACTION_BOOT_COMPLETED, start MainActivity!");
            Intent new_intent = new Intent(ctx, MainActivity.class);
            new_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(new_intent);
            Log.i(TAG, "end of start MainActivity!");
        }
    }
}

package ch.teko.broadcastrecieverexample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private final String LOG_TAG = "NetworkChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d(LOG_TAG, intent.getAction());
            Log.d(LOG_TAG, ConnectivityManager.CONNECTIVITY_ACTION);

            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d(LOG_TAG, "if statement");
                Intent connIntent = new Intent(context, MainActivity.class);
                connIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
                connIntent.putExtra("connIntent", "CONNECTIVITY_CHANGED");
                context.startActivity(connIntent);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "onRecieve() error, " + String.valueOf(e));
        }
    }

}

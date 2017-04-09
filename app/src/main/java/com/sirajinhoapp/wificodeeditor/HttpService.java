package com.sirajinhoapp.wificodeeditor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

/**
 * Created by siraj on 05.04.17.
 */
public class HttpService extends Service {

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setTicker("Wifi Code Editor")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Wifi Code Editor is running.")
                .setContentIntent(pendingIntent).build();
        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // TODO Auto-generated method stub
   /*     Intent restartService = new Intent(getApplicationContext(),
                this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);

        //Restart the service once it has been killed android


        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 100, restartServicePI);
*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();

        //start a separate thread and start listening to your network object
    }
}

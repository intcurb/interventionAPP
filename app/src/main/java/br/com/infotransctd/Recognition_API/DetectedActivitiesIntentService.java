package br.com.infotransctd.Recognition_API;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class DetectedActivitiesIntentService extends IntentService {

    protected static final String TAG = DetectedActivitiesIntentService.class.getSimpleName();

    public DetectedActivitiesIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @SuppressWarnings("unchecked")
    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            boolean flag = true;

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

            for (DetectedActivity activity : detectedActivities) {
                Log.d(TAG, "Detected activity: " + activity.getType() + ", " + activity.getConfidence());
                if (flag) {
                    broadcastActivity(activity);
                    flag = false;
                } else {

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void broadcastActivity(DetectedActivity activity) {
        try {
            Intent intent = new Intent(Constants.BROADCAST_DETECTED_ACTIVITY);
//            Intent intent = new Intent(getApplicationContext(), GPS_Service.class);
//            intent.setAction(Constants.BROADCAST_DETECTED_ACTIVITY);
            intent.putExtra("type", activity.getType());
            intent.putExtra("confidence", activity.getConfidence());
            getApplicationContext().sendBroadcast(intent); //nao finaliza aplicação mas não chega no onReceive
//            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

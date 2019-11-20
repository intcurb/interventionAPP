package br.com.infotransctd.Recognition_API;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.DetectedActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.com.infotransctd.GPS.LocationData;
import br.com.infotransctd.GPS.RouteDate;
import br.com.infotransctd.MainActivity;
import br.com.infotransctd.R;
import br.com.infotransctd.Recognition_API.BackgroundDetectedActivitiesService;
import br.com.infotransctd.Recognition_API.Constants;

import static br.com.infotransctd.GPS.App.CHANNEL_ID;

public class GPS_Service extends Service {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private String meansOfTransport;
    Date dataInicial, dataFinal;
    ArrayList<LocationData> listOfLocation;
    private LocationData tempLocation;
    double listOfSpeed = 0.0;
    int numberOfGetSpeed = 0;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRefLat;
    CountDownTimer count, countSendLocations;
    Date dateNow;
    int interval = 120000, countDown = 1;   //interval = 2 minutes in millis and countDown is the steps to downgrade the millis
    String userId = "";
    String data = "";
    String initDate, finishDate;
    int sendInterval = 300000, sendCountDown = 1;
    private String bd = "locations"; //change between locations and locationsTest
    boolean onState = false, onTick = false;
    private Context context;

    private BroadcastReceiver broadcastReceiverActivity;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public GPS_Service() {
        listOfLocation = new ArrayList<>();
        tempLocation = new LocationData();
        dataInicial = Calendar.getInstance().getTime();
        initDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dataInicial);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        meansOfTransport = (String) intent.getExtras().get("meansOfTransport");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ITSMEI")
                .setContentText("O App está rodando!")
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            getApplicationContext().unregisterReceiver(broadcastReceiverActivity);

            dataFinal = Calendar.getInstance().getTime();
            finishDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dataFinal);

            double averageOfSpeed = 0;
            if (locationManager != null) {
                locationManager.removeUpdates(locationListener);
            }
            myRefLat = database.getReference(bd);
            if (listOfSpeed != 0 && numberOfGetSpeed != 0) {
                averageOfSpeed = listOfSpeed / numberOfGetSpeed;
            }

            LocationData location = listOfLocation.get(listOfLocation.size() - 1);

            String cityName = hereLocation(location.getLatitude(), location.getLongitude());

            RouteDate routeDate = new RouteDate(initDate, finishDate, listOfLocation, averageOfSpeed, meansOfTransport, cityName);
            myRefLat.push().setValue(routeDate);

            myRefLat = database.getReference("locationsWeb"); //troca a referencia do banco

            myRefLat.child(userId).removeValue(); //remove o valor temporário do banco

            count.cancel(); //Para o contador para não chamar o método do push
            countSendLocations.cancel();
            stopTracking();


//            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiverActivity);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {

        context = getApplicationContext();

//        context.registerReceiver(broadcastReceiverActivity, new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));

        if (broadcastReceiverActivity == null) {
            broadcastReceiverActivity = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                            int type = intent.getIntExtra("type", -1);
                            int confidence = intent.getIntExtra("confidence", 0);
                            handleUserActivity(type, confidence);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        context.registerReceiver(broadcastReceiverActivity, new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
//
//        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiverActivity,
//                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));

        startTracking();

        userId = "user - " + meansOfTransport + Calendar.getInstance().getTime(); //define um nome para o node child


        //CountDown para o banco temporário
        count = new CountDownTimer(interval, countDown) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                myRefLat = database.getReference("locationsWeb"); //Seleciona o node do banco

                try {
                    LocationData location = tempLocation;

                    String cityName = hereLocation(location.getLatitude(), location.getLongitude());

                    RouteDate routeDate = new RouteDate(tempLocation, meansOfTransport, cityName);

                    myRefLat.child(userId).setValue(routeDate);

                    count.cancel(); //Cancela o contador
                    count.start();  //Chama o contador de novo
                } catch (Exception e) {
                    String erro = e.getMessage();
                }
            }
        };
        //---------------------------------

        count.start(); //Starta o contador para subir a cada 2 minutos as locations no banco temporario

        sendLocation();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                dateNow = Calendar.getInstance().getTime();
                data = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(dateNow);
                LocationData locationDataTemp = new LocationData(location.getLatitude(), location.getLongitude());
                LocationData locationData = new LocationData(location.getLatitude(), location.getLongitude(), data);
                if (locationData != null && (locationDataTemp.getLatitude() != 0 || locationDataTemp.getLongitude() != 0)) {
                    numberOfGetSpeed++;
                    listOfLocation.add(locationData);
                    tempLocation = locationDataTemp;
                    listOfSpeed = listOfSpeed + (location.getSpeed() * 3.6);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        };

//        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
    }

    private String hereLocation(double lat, double lon) {
        String cityName = "";

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(lat, lon, 10);
            if (addresses.size() > 0) {
                for (Address addr : addresses) {
                    if (addr.getLocality() != null && addr.getLocality().length() > 0) {
                        cityName = addr.getLocality();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cityName;
    }

    @SuppressLint("MissingPermission")
    private void handleUserActivity(int type, int confidence) {
        try {
            switch (type) {
                case DetectedActivity.IN_VEHICLE: {
                    if (confidence > Constants.CONFIDENCE) {
                        onState = true;
                        onTick = false;
                        countSendLocations.cancel();
                        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
                    }
//                    Toast.makeText(getApplicationContext(), "IN_VEHICLE", Toast.LENGTH_SHORT).show();
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    if (confidence > Constants.CONFIDENCE) {
                        onState = true;
                        onTick = false;
                        countSendLocations.cancel();
                        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
                    }
//                    Toast.makeText(getApplicationContext(), "ON_BICYCLE", Toast.LENGTH_SHORT).show();
                    break;
                }
                case DetectedActivity.ON_FOOT: {
//                    Toast.makeText(getApplicationContext(), "ON_FOOT", Toast.LENGTH_SHORT).show();
                    break;
                }
                case DetectedActivity.RUNNING: {
//                    Toast.makeText(getApplicationContext(), "RUNNING", Toast.LENGTH_SHORT).show();
                    break;
                }
                case DetectedActivity.STILL: {
                    if (onState && !onTick) {
                        countSendLocations.start();
                        onState = false;
                        onTick = true;
                    }
//                    Toast.makeText(getApplicationContext(), "STILL", Toast.LENGTH_SHORT).show();
                    break;
                }
                case DetectedActivity.TILTING: {
//                    Toast.makeText(getApplicationContext(), "TILTING", Toast.LENGTH_SHORT).show();
                    break;
                }
                case DetectedActivity.WALKING: {
//                    Toast.makeText(getApplicationContext(), "WALKING", Toast.LENGTH_SHORT).show();
                    break;
                }
                case DetectedActivity.UNKNOWN: {
//                    Toast.makeText(getApplicationContext(), "UNKNOWN", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startTracking() {
        Intent intent1 = new Intent(getApplicationContext(), BackgroundDetectedActivitiesService.class);
        startService(intent1);
    }

    private void stopTracking() {
        Intent intent = new Intent(getApplicationContext(), BackgroundDetectedActivitiesService.class);
        stopService(intent);
    }

    private void sendLocation() {
        try {
            countSendLocations = new CountDownTimer(sendInterval, sendCountDown) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    myRefLat = database.getReference(bd);

                    try {
                        dataFinal = Calendar.getInstance().getTime();
                        finishDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dataFinal);

                        double averageOfSpeed = 0;
                        if (locationManager != null) {
                            locationManager.removeUpdates(locationListener);
                        }

                        if (listOfSpeed != 0 && numberOfGetSpeed != 0) {
                            averageOfSpeed = listOfSpeed / numberOfGetSpeed;
                        }

                        LocationData location = listOfLocation.get(listOfLocation.size() - 1);

                        String cityName = hereLocation(location.getLatitude(), location.getLongitude());

                        RouteDate routeDate = new RouteDate(initDate, finishDate, listOfLocation, averageOfSpeed, meansOfTransport, cityName);
                        myRefLat.child(userId).setValue(routeDate);

                        countSendLocations.cancel(); //Para o contador para não chamar o método do push
                        stopTracking();

                    } catch (Exception e) {
                        String erro = e.getMessage();
                    }
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

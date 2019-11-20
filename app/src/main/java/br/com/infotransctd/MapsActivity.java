package br.com.infotransctd;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import br.com.infotransctd.Interdictions.Interdiction;
import br.com.infotransctd.Interdictions.InterdictionsList;
import br.com.infotransctd.Interdictions.Local_Interdiction;
import br.com.infotransctd.Recognition_API.Constants;
import br.com.infotransctd.Recognition_API.GPS_Service;
import br.com.infotransctd.directionHelpers.FetchInterdictionsURL;
import br.com.infotransctd.directionHelpers.FetchURL;
import br.com.infotransctd.directionHelpers.TaskLoadedCallback;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, TaskLoadedCallback {

    GoogleMap map;
    private EditText txtOrigin;
    private EditText txtDestination;
    private LatLng origin, destination, origin2, destination2, mapZoom;
    private ImageButton btnSearch;
    private Button btnList;
    private MarkerOptions place1, place2, place3, place4, interdictionPlace1, interdictionPlace2;
    private List<MarkerOptions> markerListInterdictions = new ArrayList<>(), markerListRoutes = new ArrayList<>();
    private List<LatLng> interdictionsLatLng = new ArrayList<>(), routesLatLng = new ArrayList<>();
    private Polyline currentPolyline;
    private String databaseNode = "interdictions";
    private List<Interdiction> interdictions = new ArrayList<>();
    private boolean isInInterval = false;
    private List<Marker> markers = new ArrayList<>();
    private BroadcastReceiver broadcastReceiverActivity;

    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference(databaseNode);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        txtOrigin = (EditText) findViewById(R.id.txtOrigin);
        txtDestination = (EditText) findViewById(R.id.txtDestination);
        btnSearch = (ImageButton) findViewById(R.id.btnSearch);
        btnList = (Button) findViewById(R.id.btnList);

        mapZoom = new LatLng(-21.134455, -48.975450);

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    map.clear();
                    markerListInterdictions.clear();

                    for (DataSnapshot keyNode : dataSnapshot.getChildren()) {
                        isInInterval = false;
                        Interdiction interdiction = new Interdiction();

                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        Date dateNow = Calendar.getInstance().getTime();
                        String beginDate = keyNode.child("beginDate").getValue().toString();
                        String endDate = keyNode.child("endDate").getValue().toString();
                        Date dateBegin = new Date(df.parse(beginDate).getTime());
                        Date dateEnd = new Date(df.parse(endDate).getTime());
                        String formatedDate = df.format(dateNow);
                        Date dateToday = new Date(df.parse(formatedDate).getTime());
                        String status = keyNode.child("status").getValue().toString();

                        String[] date1 = beginDate.split(" ");
                        String[] date2 = endDate.split(" ");

                        if (status.equals("true")) {
                            if (dateToday.after(dateBegin) && dateToday.before(dateEnd)) {
                                isInInterval = true;
                            }
                            if (formatedDate.equals(date1[0]) || formatedDate.equals(date2[0])) {
                                isInInterval = true;
                            }
                        } else {
                            isInInterval = false;
                        }

                        if (isInInterval) {
                            interdiction.setBeginDate(keyNode.child("beginDate").getValue().toString());
                            interdiction.setEndDate(keyNode.child("endDate").getValue().toString());
                            interdiction.setOrganization(keyNode.child("organization").getValue().toString());
                            interdiction.setDescription(keyNode.child("description").getValue().toString());

                            Local_Interdiction origin = new Local_Interdiction(keyNode.child("origin").child("street").getValue().toString(),
                                    keyNode.child("origin").child("lat").getValue().toString(),
                                    keyNode.child("origin").child("lng").getValue().toString());

                            Local_Interdiction destination = new Local_Interdiction(keyNode.child("destination").child("street").getValue().toString(),
                                    keyNode.child("destination").child("lat").getValue().toString(),
                                    keyNode.child("destination").child("lng").getValue().toString());

                            interdiction.setOrigin(origin);
                            interdiction.setDestination(destination);

                            interdictions.add(interdiction);

                            //Converts in string the origin lat and lng
                            String latLong = interdiction.getOrigin().getLat() + "," + interdiction.getOrigin().getLng();

                            String[] latLongArray = latLong.split(",");

                            double lat = Double.parseDouble(latLongArray[0]);
                            double lng = Double.parseDouble(latLongArray[1]);

                            LatLng interdictionOriginWaypoint = new LatLng(lat, lng);
                            interdictionsLatLng.add(interdictionOriginWaypoint);

                            latLong = interdiction.getDestination().getLat() + "," + interdiction.getDestination().getLng();

                            latLongArray = latLong.split(",");

                            lat = Double.parseDouble(latLongArray[0]);
                            lng = Double.parseDouble(latLongArray[1]);

                            LatLng interdictionDestinationWaypoint = new LatLng(lat, lng);
                            interdictionsLatLng.add(interdictionDestinationWaypoint);

                            String url = getUrlFromInterdictions(interdictionOriginWaypoint, interdictionDestinationWaypoint, "driving");
                            new FetchInterdictionsURL(MapsActivity.this).execute(url, "driving");

                            String streetOrigin = interdiction.getOrigin().getStreet();
                            String streetDestination = interdiction.getDestination().getStreet();

                            interdictionPlace1 = new MarkerOptions().position(interdictionOriginWaypoint).title(streetOrigin);
                            interdictionPlace2 = new MarkerOptions().position(interdictionDestinationWaypoint).title(streetDestination);

                            markerListInterdictions.add(interdictionPlace1);
                            markerListInterdictions.add(interdictionPlace2);

                            markers.add(map.addMarker(interdictionPlace1));
                            markers.add(map.addMarker(interdictionPlace2));
                        }


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                onMapReady(map);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapsActivity.this, "Failed to read the database reference!", Toast.LENGTH_SHORT).show();
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Clear the polylines in the map

                markerListRoutes.clear();
                routesLatLng.clear();

                try {
                    map.clear();

                    //Get the origin and destination locations from the input
                    origin = getLatLongFromAddress(txtOrigin.getText().toString());
                    destination = getLatLongFromAddress(txtDestination.getText().toString());
                    origin2 = origin;
                    destination2 = destination;

                    routesLatLng.add(origin);
                    routesLatLng.add(destination);
                    routesLatLng.add(origin2);
                    routesLatLng.add(destination2);

                    if (origin == null) {
                        Toast.makeText(MapsActivity.this, "The origin cannot be found, please specify better!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    } else if (destination == null) {
                        Toast.makeText(MapsActivity.this, "The destination cannot be found, please specify better!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String url = getUrl(origin, destination, "driving");
                    String url2 = getUrl2(origin2, destination2, "driving");

                    new FetchURL(MapsActivity.this).execute(url, "driving");
                    new FetchURL(MapsActivity.this).execute(url2, "driving");

                    place1 = new MarkerOptions().position(origin).title("origin");
                    place2 = new MarkerOptions().position(destination).title("destination");
                    place3 = new MarkerOptions().position(origin2).title("origin");
                    place4 = new MarkerOptions().position(destination2).title("destination");

                    markerListRoutes.add(place1);
                    markerListRoutes.add(place2);
                    markerListRoutes.add(place3);
                    markerListRoutes.add(place4);

                    //Fetching part of interdictions to plot again in the map
                    String urlInterdictions = "";
                    for (Integer i = 0; i < interdictionsLatLng.size(); i += 2) {
                        urlInterdictions = getUrlFromInterdictions(interdictionsLatLng.get(i), interdictionsLatLng.get(i + 1), "driving");
                        new FetchInterdictionsURL(MapsActivity.this).execute(urlInterdictions, "driving");
                    }


                    onMapReady(map);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                String description, organization, beginDate, endDate, street1, stree2, lat1, lat2, lng1, lng2;
//                description = "Obra de Saneamento";
//                organization = "SEMAE";
//                beginDate = "15/09/2019";
//                endDate = "20/09/2019";
//                street1 = "Rua Catanduva";
//                stree2 = "Rua São Paulo";
//                lat1 = "-21.132017";
//                lat2 = "-21.128775";
//                lng1 = "-48.971795";
//                lng2 = "-48.979606";
//
//                Local_Interdiction origin = new Local_Interdiction(street1, lat1, lng1);
//                Local_Interdiction destination = new Local_Interdiction(stree2, lat2, lng2);
//
//                Interdiction inter = new Interdiction(description, organization, beginDate, endDate, origin, destination);
//                myRef.push().setValue(inter);

                try {
                    Intent i = new Intent(getApplicationContext(), InterdictionsList.class);
                    i.putExtra("interdictions", (Serializable) interdictions);
                    startActivity(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    private String getUrl2(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        //Alternative route
        String alternative = "&alternatives=true";
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + alternative + "&key=" + getString(R.string.google_directions);
        return url;
    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_directions);
        return url;
    }

    private String getUrlFromInterdictions(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_directions);
        return url;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        try {
            map = googleMap;

            //Zoom into catanduva
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(mapZoom, 13));
            map.clear();

            for (Integer i = 0; i < markers.size(); i++) {
                markers.get(i).remove();
            }

            for (Integer i = 0; i < markerListRoutes.size(); i++) {
                map.addMarker(markerListRoutes.get(i));
            }

            for (Integer i = 0; i < markerListInterdictions.size(); i++) {
                map.addMarker(markerListInterdictions.get(i));
            }

            restoreRoute(routesLatLng.get(0), routesLatLng.get(1), routesLatLng.get(2), routesLatLng.get(3));


//            if (place1 != null && place2 != null) {
//                map.addMarker(place1);
//                map.addMarker(place2);
//                map.addMarker(place3);
//                map.addMarker(place4);
//
//            }
//
//            for (Integer i = 0; i < markerList.size(); i++) {
//                markers.add(map.addMarker(markerList.get(i)));
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private LatLng getLatLongFromAddress(String address) {
        Geocoder gc = new Geocoder(getApplicationContext());
        Address location;
        LatLng lat_long = null;

        try {
            List<Address> places = gc.getFromLocationName(address, 1);
            location = places.get(0);
            lat_long = new LatLng(location.getLatitude(), location.getLongitude());

            return lat_long;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onTaskDone(Boolean interdiction, Object... values) {
//        if (currentPolyline != null)
//            currentPolyline.remove();

        if (interdiction) {
            map.addPolyline((PolylineOptions) values[0]).setColor(Color.RED);
        } else {
            map.addPolyline((PolylineOptions) values[0]);
        }

    }

    public void restoreRoute(LatLng a, LatLng b, LatLng c, LatLng d){
        String url = getUrl(a, b, "driving");
        String url2 = getUrl2(c, d, "driving");

        new FetchURL(MapsActivity.this).execute(url, "driving");
        new FetchURL(MapsActivity.this).execute(url2, "driving");
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiverActivity,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));

    }

}

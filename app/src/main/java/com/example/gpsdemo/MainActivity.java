package com.example.gpsdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.logging.Logger;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int LOCATION_REQUEST_UPDATE_INTERVAL = 10000;
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wayPointCounts;
    Switch sw_locationupdates, sw_gps;
    Button btn_newWayPoint, btn_showWayPointList, btn_showMap;

    Logger logger = new Logger("MAIN");

    // current location
    Location currentLocation;

    // list of saved locations
    List<Location> savedLocations;

    // Location request is a config file for all settings related to FusedLocationProviderClient.
    LocationRequest locationRequest;

    LocationCallback locationCallBack;

    // Google's API for location services. The majority of the app functions using this class.
    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Give each UI variable a value
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        tv_wayPointCounts = findViewById(R.id.tv_countOfCrumbs);

        sw_locationupdates = findViewById(R.id.sw_locationsupdates);
        sw_gps = findViewById(R.id.sw_gps);

        btn_newWayPoint = findViewById(R.id.btn_newWayPoint);
        btn_showWayPointList = findViewById(R.id.btn_showWayPointList);
        btn_showMap = findViewById(R.id.btn_showMap);

        // Set all properties of LocationRequest
        // We set the priority to high accuracy and the update interval to half a second
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_REQUEST_UPDATE_INTERVAL).build();

        // event that is triggered whenever the update interval is met
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // save the location
                Location location = locationResult.getLastLocation();
                currentLocation = location;
                updateUIValues(location);
            }
        };

        btn_newWayPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get the gps location


                // add the new location to the global list
                MyApplication myApplication = (MyApplication) getApplicationContext();
                savedLocations = myApplication.getMyLocations();
                savedLocations.add(currentLocation);

                logger.e(String.valueOf(currentLocation.getLatitude()));
                logger.e(tv_lat.getText().toString());
            }
        });

        btn_showWayPointList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, ShowSavedLocationsList.class);
                startActivity(i);
            }
        });

        btn_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(i);
            }
        });

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                if (sw_locationupdates.isChecked()) {
                    if (sw_gps.isChecked()) {
                        locationRequest = new LocationRequest.Builder(locationRequest).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build();
                        tv_sensor.setText("Using GPS sensors");
                    } else {
                        locationRequest = new LocationRequest.Builder(locationRequest).setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY).build();
                        tv_sensor.setText("Using Towers + WIFI");
                    }
                }
            }
        });

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_locationupdates.isChecked()) {

                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });


        updateGPS();
        startLocationUpdates();
    }

    private void stopLocationUpdates() {
        tv_updates.setText("Location is NOT being tracked");
        tv_lat.setText("Not tracking location");
        tv_lon.setText("Not tracking location");
        tv_speed.setText("Not tracking location");
        tv_address.setText("Not tracking location");
        tv_accuracy.setText("Not tracking location");
        tv_altitude.setText("Not tracking location");
        tv_sensor.setText("Not tracking location");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

    private void startLocationUpdates() {
        tv_updates.setText("Location is being tracked");

        if (sw_gps.isChecked()) {
            tv_sensor.setText("Using GPS sensors");
        } else {
            tv_sensor.setText("Using Towers + WIFI");
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case PERMISSIONS_FINE_LOCATION:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateGPS();
            } else {
                Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        }
    }

    private void updateGPS() {
        // get permissions from the user to track GPS
        // get the current location from the fused client
        // update the UI - i.e. set all properties in their associated text view items

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // user provided the permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // we got permissions. Put the values of location into the UI components
                    if (location != null) {
                        updateUIValues(location);
                        currentLocation = location;
                    } else {
                        tv_lat.setText("Location received was null");
                    }

                }
            });
        } else {
            // permissions not granted yet

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    private void updateUIValues(Location location) {
        // update all of the text view objects with a new location.
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        if (location.hasAltitude()) {
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        } else {
            tv_altitude.setText("Not available");
        }

        if (location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getSpeed()));
        } else {
            tv_speed.setText("Not available");
        }

        Geocoder geocoder = new Geocoder(MainActivity.this);

        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            tv_address.setText(addresses.get(0).getAddressLine(0));
        } catch (Exception e) {
            tv_address.setText("Unable to get street address");
        }

        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocations();

        // show the number of waypoints saved
        tv_wayPointCounts.setText(Integer.toString(savedLocations.size()));
    }

}
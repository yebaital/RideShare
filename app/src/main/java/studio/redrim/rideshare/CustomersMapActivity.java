package studio.redrim.rideshare;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String customerId;

    private FirebaseDatabase db;

    private DatabaseReference customersRef, availableDriversRef, driverRef, driverLocationRef;

    private Button settingsBtn, logoutBtn, requestRideBtn;

    private GeoLocation geoLocation;
    private LatLng customerPickUpLocation;

    private int radius = 1;
    private boolean driverFound = false;
    private boolean requestType = false;
    private String driverId;

    private GeoFire customerGeoFire, availableDriversGeoFire;
    private GeoQuery geoQuery;

    private Marker driverMarker, pickupMarker;

    private ValueEventListener driverLocationRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.customers_map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        settingsBtn = findViewById(R.id.customer_settings_button);
        logoutBtn = findViewById(R.id.customer_logout_button);
        requestRideBtn = findViewById(R.id.request_ride_button);

        customersRef = db.getReference()
                .child("Customers Requests");

        availableDriversRef = db.getReference()
                .child("Drivers Available");

        driverLocationRef = db.getReference().child("Drivers Working");

        customerGeoFire = new GeoFire(customersRef);

        createLocationRequest();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        });
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    geoLocation = new GeoLocation(
                            location.getLatitude(),
                            location.getLongitude());

                    customerPickUpLocation = new LatLng(location.getLatitude(), location.getLongitude());
                }
            }
        };



        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        requestRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestType) {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if (driverId != null) {
                        driverRef = db.getReference()
                                .child("users").child("drivers").child(driverId).child("customerId");

                        driverRef.removeValue();
                        driverId = null;
                    }
                    driverFound = false;
                    radius = 1;

                    customerGeoFire.removeLocation(customerId);

                    if (pickupMarker != null && driverMarker != null) {
                        pickupMarker.remove();
                        driverMarker.remove();
                    }

                    requestRideBtn.setText(getString(R.string.request_ride));
                } else {
                    requestType = true;
                    customerGeoFire.setLocation(customerId, geoLocation);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(customerPickUpLocation).title("Pick up location"));
                    requestRideBtn.setText(getString(R.string.checking_drivers));
                    getClosestDriver();
                }
            }
        });

    }

    private void getClosestDriver() {
        availableDriversGeoFire = new GeoFire(availableDriversRef);
        geoQuery = availableDriversGeoFire.queryAtLocation(geoLocation, radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestType) {
                    driverFound = true;
                    driverId = key;

                    driverRef = db.getReference()
                            .child("users").child("drivers").child(driverId);
                    HashMap driverMap = new HashMap();
                    driverMap.put("customerId", customerId);
                    driverRef.updateChildren(driverMap);

                    getDriverLocation();
                    requestRideBtn.setText(getString(R.string.getting_driver_location));
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    radius = radius + 1;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriverLocation() {
        driverLocationRefListener = driverLocationRef.child(driverId).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestType) {
                    List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                    double lat = 0;
                    double lng = 0;
                    requestRideBtn.setText(getString(R.string.driver_found));

                    if (driverLocationMap.get(0) != null && driverLocationMap.get(1) != null) {
                        lat = Double.parseDouble(driverLocationMap.get(0).toString());
                        lng = Double.parseDouble(driverLocationMap.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(lat, lng);
                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    Location customerLocation = new Location("");
                    customerLocation.setLatitude(customerPickUpLocation.latitude);
                    customerLocation.setLongitude(customerPickUpLocation.longitude);

                    Location driverLocation = new Location("");
                    driverLocation.setLatitude(driverLatLng.latitude);
                    driverLocation.setLongitude(driverLatLng.longitude);

                    float distance = customerLocation.distanceTo(driverLocation);

                    if (distance < 90) {
                        requestRideBtn.setText(getString(R.string.driver_arrived));
                    } else {
                        requestRideBtn.setText("Driver found: " + String.valueOf(distance) + " m away. \n Click to cancel the request.");
                    }

                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your driver's location"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
    }

    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            currentUser = mAuth.getCurrentUser();
            customerId = currentUser.getUid();
        } else {
            startActivity(new Intent(CustomersMapActivity.this, WelcomeActivity.class));
        }
        startLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}





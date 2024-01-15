package com.example.sturzerkennung;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

// Version 20.07.2020 10:43
// Finaler Push zu Git

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Attribute
    private static final String TAG = "MainActivity"; // zum abkürzen
    private LocationManager locationManager; // Um Zugriff auf Standort zu bekommen
    private LocationListener locationListener; // Für Standortveränderungen
    private SensorManager sensorManager;
    private Vibrator vibe;
    private MediaPlayer mediaPlayer;

    Sensor accelerometer;
    private TextView xAcce;
    private TextView yAcce;
    private TextView zAcce;
    private float xNew;
    private float yNew;
    private float zNew;

    Sensor gyroscope;
    private TextView gXacce;
    private TextView gYacce;
    private TextView gZacce;

    private TextView laengengrad;
    private TextView breitengrad;
    private TextView adresszeile;

    // Zum Vergleich, ob sich die Position verändet hat.
    private double latitude;
    private double longitude;
    private int genauigkeitLatLon = 4; // 3 = 111.1m; 4 = 11.1m; 5 = 1.1m

    private Button button_popUp;
    private int neigungscounterX;
    private int neigungscounterZ;

    private RadioButton radioButtonOnBike;
    private RadioButton radioButtonInPocket;

    private int counterForCurveOver;
    private int counterForDeleteTimer;
    private int counterForStartMeasuring;
    private int counterForLocationTenSec;

    DatabaseHelper userDataDB;
    String stringBreitengrad, stringLaengengrad, stringNotfallAdresse;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        // Initialisierung
        laengengrad = (TextView) findViewById(R.id.laengengrad);
        breitengrad = (TextView) findViewById(R.id.breitengrad);
        adresszeile = (TextView) findViewById(R.id.strasse);

        xAcce = (TextView) findViewById(R.id.xAcce);
        yAcce = (TextView) findViewById(R.id.yAcce);
        zAcce = (TextView) findViewById(R.id.zAcce);

        gXacce = (TextView) findViewById(R.id.gXacce);
        gYacce = (TextView) findViewById(R.id.gYacce);
        gZacce = (TextView) findViewById(R.id.gZacce);

        radioButtonOnBike = (RadioButton) findViewById(R.id.radioButtonOnBike);
        radioButtonInPocket = (RadioButton) findViewById(R.id.radioButtonInPocket);

        userDataDB = new DatabaseHelper(this);

        // Falls Notruf Button gedrückt, dann erscheint das PopUp
        button_popUp = (Button) findViewById(R.id.button_popUp);
        button_popUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Benachrichtigung
                Log.d(TAG, "onCreate: Button Notruf clicked");
                vibe.vibrate(500);
                mediaPlayer.start();

                // Öffne PopUp und Werte übergeben
                Bundle bundle = new Bundle();
                bundle.putString("Breitengrad", stringBreitengrad);
                bundle.putString("Längengrad", stringLaengengrad);
                bundle.putString("NotfallAdresse", stringNotfallAdresse);

                Intent i = new Intent(MainActivity.this, PopUp.class);
                i.putExtras(bundle);
                startActivity(i);

                // startActivity(new Intent(MainActivity.this, PopUp.class));
            }
        });

        // Vibration
        Log.d(TAG, "onCreate: Initializing Vibration Services");
        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // MediaPlayer zum Abspielen von Warningsound
        Log.d(TAG, "onCreate: Initializing MediaPlayer Services");
        mediaPlayer = MediaPlayer.create(this, R.raw.siren);

        Log.d(TAG, "onCreate: Initializing Sensor Services");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Accelerometer integration
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "onCreate: Registered accelerometer listener");
        } else {
            xAcce.setText("Accelerometer wird nicht unterstützt");
            yAcce.setText("");
            zAcce.setText("");
        }

        // Gyroscope integration
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscope != null) {
            sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "onCreate: Registered gyroscope listener");
        } else {
            gXacce.setText("Gyroscope wird nicht unterstützt");
            gYacce.setText("");
            gZacce.setText("");
        }


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

                // mod 10 da es nur alle 10 Sekunden erscheinen soll
                counterForLocationTenSec++;
                if ((counterForLocationTenSec % 10) == 0) {

                    // Falls sich der Standort nicht mehr ändert, öffnet sich ein PopUp
                    breitengrad.setText(null);
                    laengengrad.setText(null);


                    Log.d(TAG, "|----------------------------------- Debug -----------------------------------");
                    Log.d(TAG, "| counterForLocationTenSec: " + counterForLocationTenSec + " (Muss immer durch 10 teilbar sein)");
                    Log.d(TAG, "| latitude = " + location.getLatitude());
                    Log.d(TAG, "| longitude = " + location.getLongitude());
                    Log.d(TAG, "| ");
                    Log.d(TAG, "| latitude gerundet = " + round(location.getLatitude(), genauigkeitLatLon));
                    Log.d(TAG, "| longitude gerundet = " + round(location.getLongitude(), genauigkeitLatLon));
                    Log.d(TAG, "| ");
                    Log.d(TAG, "| Welcher Button ist gewählt? ");
                    Log.d(TAG, "| Auf dem Fahrrad: " + radioButtonOnBike.isChecked());
                    Log.d(TAG, "| In der Hosentasche: " + radioButtonInPocket.isChecked());
                    if (!radioButtonInPocket.isChecked() && !radioButtonOnBike.isChecked()) {
                        Log.d(TAG, "| Keiner ist gewählt!");
                    }
                    Log.d(TAG, "|-----------------------------------------------------------------------------");


                    if (radioButtonOnBike.isChecked() && !radioButtonInPocket.isChecked()) {
                        Log.d(TAG, "# # # # # # # # # # RadioButton (onLocationChanged): Auf dem Fahrrad # # # # # # # # # #");

                        if (latitude != round(location.getLatitude(), genauigkeitLatLon) || longitude != round(location.getLongitude(), genauigkeitLatLon)) {

                            Log.d(TAG, "////////// Location: Standort verändert sich //////////");

                            latitude = round(location.getLatitude(), genauigkeitLatLon);
                            longitude = round(location.getLongitude(), genauigkeitLatLon);

                            breitengrad.append("" + location.getLatitude());
                            laengengrad.append("" + location.getLongitude());

                            stringBreitengrad = ("" + location.getLatitude());
                            stringLaengengrad = ("" + location.getLongitude());
                            Log.d(TAG, "" + stringBreitengrad);
                            Log.d(TAG, "" + stringLaengengrad);

                        } else if (latitude == round(location.getLatitude(), genauigkeitLatLon) && longitude == round(location.getLongitude(), genauigkeitLatLon) && neigungscounterX == 1) {

                            Log.d(TAG, "////////// Location: Stehen geblieben und gefallen; PopUp geöffnet und Counter = " + neigungscounterX + " //////////");

                            // Benachrichtigung
                            vibe.vibrate(500);
                            mediaPlayer.start();

                            // Öffne PopUp und Werte übergeben
                            Bundle bundle = new Bundle();
                            bundle.putString("Breitengrad", stringBreitengrad);
                            bundle.putString("Längengrad", stringLaengengrad);
                            bundle.putString("NotfallAdresse", stringNotfallAdresse);

                            Intent i = new Intent(MainActivity.this, PopUp.class);
                            i.putExtras(bundle);
                            startActivity(i);

                            latitude = round(location.getLatitude(), genauigkeitLatLon);
                            longitude = round(location.getLongitude(), genauigkeitLatLon);

                            breitengrad.append("" + location.getLatitude());
                            laengengrad.append("" + location.getLongitude());

                            stringBreitengrad = ("" + location.getLatitude());
                            stringLaengengrad = ("" + location.getLongitude());
                            Log.d(TAG, "" + stringBreitengrad);
                            Log.d(TAG, "" + stringLaengengrad);

                            neigungscounterX = 0;

                        } else if (latitude == round(location.getLatitude(), genauigkeitLatLon) && longitude == round(location.getLongitude(), genauigkeitLatLon) && neigungscounterX == 0) {

                            Log.d(TAG, "////////// Location: Stehen geblieben, aber nicht gefallen //////////");

                            latitude = round(location.getLatitude(), genauigkeitLatLon);
                            longitude = round(location.getLongitude(), genauigkeitLatLon);

                            breitengrad.append("" + location.getLatitude());
                            laengengrad.append("" + location.getLongitude());

                            stringBreitengrad = ("" + location.getLatitude());
                            stringLaengengrad = ("" + location.getLongitude());
                            Log.d(TAG, "" + stringBreitengrad);
                            Log.d(TAG, "" + stringLaengengrad);

                        } else {
                            Log.d(TAG, "////////// Location: Komischer Standort //////////");
                        }

                    } else if (!radioButtonOnBike.isChecked() && radioButtonInPocket.isChecked()) {
                        Log.d(TAG, "# # # # # # # # # # RadioButton (onLocationChanged): In der Hosentasche # # # # # # # # # #");

                        if (latitude != round(location.getLatitude(), genauigkeitLatLon) || longitude != round(location.getLongitude(), genauigkeitLatLon)) {

                            Log.d(TAG, "////////// Location: Standort verändert sich //////////");

                            latitude = round(location.getLatitude(), genauigkeitLatLon);
                            longitude = round(location.getLongitude(), genauigkeitLatLon);

                            breitengrad.append("" + location.getLatitude());
                            laengengrad.append("" + location.getLongitude());

                            stringBreitengrad = ("" + location.getLatitude());
                            stringLaengengrad = ("" + location.getLongitude());
                            Log.d(TAG, "" + stringBreitengrad);
                            Log.d(TAG, "" + stringLaengengrad);

                        } else if (latitude == round(location.getLatitude(), genauigkeitLatLon) && longitude == round(location.getLongitude(), genauigkeitLatLon) && neigungscounterZ == 1) {

                            Log.d(TAG, "////////// Location: Stehen geblieben und gefallen; PopUp geöffnet und Counter = " + neigungscounterZ + " //////////");

                            // Benachrichtigung
                            vibe.vibrate(500);
                            mediaPlayer.start();

                            // Öffne PopUp und Werte übergeben
                            Bundle bundle = new Bundle();
                            bundle.putString("Breitengrad", stringBreitengrad);
                            bundle.putString("Längengrad", stringLaengengrad);
                            bundle.putString("NotfallAdresse", stringNotfallAdresse);

                            Intent i = new Intent(MainActivity.this, PopUp.class);
                            i.putExtras(bundle);
                            startActivity(i);

                            latitude = round(location.getLatitude(), genauigkeitLatLon);
                            longitude = round(location.getLongitude(), genauigkeitLatLon);

                            breitengrad.append("" + location.getLatitude());
                            laengengrad.append("" + location.getLongitude());

                            stringBreitengrad = ("" + location.getLatitude());
                            stringLaengengrad = ("" + location.getLongitude());
                            Log.d(TAG, "" + stringBreitengrad);
                            Log.d(TAG, "" + stringLaengengrad);

                            neigungscounterZ = 0;

                        } else if (latitude == round(location.getLatitude(), genauigkeitLatLon) && longitude == round(location.getLongitude(), genauigkeitLatLon) && neigungscounterZ == 0) {

                            Log.d(TAG, "////////// Location: Stehen geblieben, aber nicht gefallen //////////");

                            latitude = round(location.getLatitude(), genauigkeitLatLon);
                            longitude = round(location.getLongitude(), genauigkeitLatLon);

                            breitengrad.append("" + location.getLatitude());
                            laengengrad.append("" + location.getLongitude());

                            stringBreitengrad = ("" + location.getLatitude());
                            stringLaengengrad = ("" + location.getLongitude());
                            Log.d(TAG, "" + stringBreitengrad);
                            Log.d(TAG, "" + stringLaengengrad);

                        } else {
                            Log.d(TAG, "////////// Location: Komischer Standort //////////");
                        }

                    } else {
                        Log.d(TAG, "# # # # # # # # # # RadioButton (onLocationChanged): Nicht auf dem Fahrrad oder in der Tasche # # # # # # # # # #");
                        breitengrad.append("Noch nicht gewählt.");
                        laengengrad.append("Noch nicht gewählt.");
                    }


                    // Adresszeile wird befüllt
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addresses = null;
                    try {
                        addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (addresses.size() > 0) {
                        adresszeile.setText(null);
                        adresszeile.append(addresses.get(0).getAddressLine(0));
                        stringNotfallAdresse = ("" + addresses.get(0).getAddressLine(0));
                    }

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
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, 1);
                return;
            } else {
                requestLocationUpdate();
            }
        } else {
            requestLocationUpdate();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    requestLocationUpdate();
                return;
        }
    }

    private void requestLocationUpdate() {
        try {
            int sek = 0;
            locationManager.requestLocationUpdates("gps", sek * 1000, 0, locationListener);
        } catch (SecurityException se) {

        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.d(TAG, "onSensorChanged: X: " + sensorEvent.values[0] + " Y: " + sensorEvent.values[1] + " Z: " + sensorEvent.values[2]);

            xNew = sensorEvent.values[0];
            yNew = sensorEvent.values[1];
            zNew = sensorEvent.values[2];

            String xNewString = Float.toString(xNew);
            String yNewString = Float.toString(yNew);
            String zNewString = Float.toString(zNew);

            xAcce.setText(xNewString);
            yAcce.setText(yNewString);
            zAcce.setText(zNewString);


            // Die ersten 15 Sekunden soll noch nicht gemessen werden, da der Nutzer sich vorbereitet
            int sek = 15;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    // Nur zum mitteilen, dass jetzt das Messen beginnt.
                    if (counterForStartMeasuring == 0){
                        Log.d(TAG, "15 Sek vorbei, Messen beginnt");
                        counterForStartMeasuring++;
                    }

                    // Auf dem Fahrrad
                    if (radioButtonOnBike.isChecked() && !radioButtonInPocket.isChecked()) {
                        // Falls Neigung nach rechts oder links zu groß ist.
                        if (xNew >= 7 || xNew <= -7) {
                            neigungscounterX = 1;
                            Log.d(TAG, "NeigungscounterX erhöht auf: " + neigungscounterX);

                            // Timer setzt Wert wieder auf 0
                            int timerSek = 20;
                            Handler neigungscounterToZero = new Handler();
                            neigungscounterToZero.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    neigungscounterX = 0;
                                    Log.d(TAG, "NeigungscounterX verringert auf: " + neigungscounterX);
                                }
                            }, timerSek * 1000);
                        }

                    // In der Hosentasche
                    } else if (!radioButtonOnBike.isChecked() && radioButtonInPocket.isChecked()) {
                        // Falls Neigung nach rechts oder links zu groß ist.
                        if (zNew >= 7 || zNew <= -7) {
                            timer();  // Nach 5 Sek. counterForCurveOver = 1
                            deleteTimer(); // Nach 10 Sek. counterForCurveOver = 0
                            Log.d(TAG, "erste Neigung");
                        }

                        // Wenn nach 5 Sek. immernoch Schräglage = Unfall
                        if (counterForCurveOver == 1 && (zNew >= 7 || zNew <= -7)) {
                            neigungscounterZ = 1;
                            Log.d(TAG, "Neigung immer noch (5Sek), NeigungscounterX erhöht auf: " + neigungscounterZ);

                            // Timer setzt Wert wieder auf 0
                            int timerSek = 20;
                            Handler neigungscounterToZero = new Handler();
                            neigungscounterToZero.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    neigungscounterZ = 0;
                                    Log.d(TAG, "NeigungscounterZ verringert auf: " + neigungscounterZ);
                                }
                            }, timerSek * 1000);

                        }
                    } else {
                        Log.d(TAG, "RadioButton (onSensorChanged): Noch nicht gewählt");
                    }

                }




            }, sek * 1000);

        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            float xNewGyro = sensorEvent.values[0];
            float yNewGyro = sensorEvent.values[1];
            float zNewGyro = sensorEvent.values[2];

            String xNewGyroString = Float.toString(xNewGyro);
            String yNewGyroString = Float.toString(yNewGyro);
            String zNewGyroString = Float.toString(zNewGyro);

            gXacce.setText(xNewGyroString);
            gYacce.setText(yNewGyroString);
            gZacce.setText(zNewGyroString);

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // zum Runden von Längengrad und Breitengrad
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    // wird bei Sturtz oder Notfall aufgerufe; Infos an Console ausgegeben und versendet per Mail
    void hilfe(String pBreitengrad, String pLaengengrad, String pNotfallAdresse) {
        stringBreitengrad = pBreitengrad;
        stringLaengengrad = pLaengengrad;
        stringNotfallAdresse = pNotfallAdresse;


        Log.d(TAG, "***************************************************************** NOTFALL *****************************************************************");

        Cursor data = userDataDB.showData();
        int counterForNotruf = 0;
        if (data.getCount() != 0) {
            while (data.moveToNext()  && counterForNotruf == 0) {
                counterForNotruf++;
                Log.d(TAG, "Genauer Standort: --> Breitengrad = " + stringBreitengrad + "; --> Längengrad = " + stringLaengengrad);
                Log.d(TAG, "Ungefähre Unfalladresse: " + stringNotfallAdresse);
                Log.d(TAG, "Vorname: " + data.getString(1));
                Log.d(TAG, "Nachname: " + data.getString(2));
                Log.d(TAG, "Geburtsdatum: " + data.getString(3));
                Log.d(TAG, "Adresse: " + data.getString(4));
                Log.d(TAG, "Vorerkrankung: " + data.getString(5));

                // Mail
                String mail = "robert.lange.8293@googlemail.com";
                String subject = "Notfall: Es wurde ein Sturz erkannt!";
                String message = "Genauer Standort: " + "\n" +
                        "--> Breitengrad = " + stringBreitengrad + "; --> Längengrad = " + stringLaengengrad + "\n" + "\n" +
                        "Ungefähre Unfalladresse: " + stringNotfallAdresse + "\n" + "\n" +
                        "Vorname: " + data.getString(1) + "\n" +
                        "Nachname: " + data.getString(2) + "\n" +
                        "Geburtsdatum: " + data.getString(3) + "\n" +
                        "Adresse: " + data.getString(4) + "\n" +
                        "Vorerkrankung: " + data.getString(5);

                // Mail senden
                JavaMailAPI javaMailAPI = new JavaMailAPI(this, mail, subject, message);
                javaMailAPI.execute();
            }
        } else {
            Log.d(TAG, "Genauer Standort: --> Breitengrad = " + stringBreitengrad + "; --> Längengrad = " + stringLaengengrad);
            Log.d(TAG, "Ungefähre Unfalladresse: " + stringNotfallAdresse);
            Log.d(TAG, "Leider hat der Nutzer keine Angaben hinterlegt.");

            // Mail
            String mail = "robert.lange.8293@googlemail.com";
            String subject = "Notfall: Es wurde ein Sturz erkannt!";
            String message = "Genauer Standort: " + "\n" +
                    "--> Breitengrad = " + stringBreitengrad + "; --> Längengrad = " + stringLaengengrad + "\n" + "\n" +
                    "Ungefähre Unfalladresse: " + stringNotfallAdresse + "\n" + "\n" +
                    "Leider hat der Nutzer keine Angaben hinterlegt.";

            // Mail senden
            JavaMailAPI javaMailAPI = new JavaMailAPI(this, mail, subject, message);
            javaMailAPI.execute();
        }

        Log.d(TAG, "***************************************************************** NOTFALL *****************************************************************");
    }

    public void timer() {
        // Timer setzt Wert wieder auf 0
        int sek = 5;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                counterForCurveOver = 1;
            }
        }, sek * 1000);
    }

    //Dieser Timer soll den erstellten Counter vom ersten Timer nach 5 Sekunden zurücksetzen
    public void deleteTimer() {
        int sek = 10;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                counterForDeleteTimer++;
                counterForCurveOver = 0;
                Log.d(TAG, "counterForDeleteTimer Nr." + counterForDeleteTimer + " gelöscht");
            }
        }, sek * 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.settings:

                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

//    public void onPause(){
//        super.onPause();
//        Context context = getApplicationContext();
//        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
//        if (!taskInfo.isEmpty()) {
//            ComponentName topActivity = taskInfo.get(0).topActivity;
//            if (!topActivity.getPackageName().equals(context.getPackageName())) {
//
//                Toast.makeText(MainActivity.this, "YOU LEFT YOUR APP. ", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

}
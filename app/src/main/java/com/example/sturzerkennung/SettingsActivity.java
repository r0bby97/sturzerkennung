package com.example.sturzerkennung;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // zum abkürzen
    DatabaseHelper userDataDB;
    Button buttonSave, buttonView, buttonUpdate, buttonSaveAndUpdate;
    EditText vornameInput, nachnameInput, geburtsdatumInput, adresseInput, vorerkrankungInput;
    int counterForButton = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        userDataDB = new DatabaseHelper(this);

        vornameInput = (EditText) findViewById(R.id.editTextVorname);
        nachnameInput = (EditText) findViewById(R.id.editTextNachname);
        geburtsdatumInput = (EditText) findViewById(R.id.editTextGeburtsdatum);
        adresseInput = (EditText) findViewById(R.id.editTextAdresse);
        vorerkrankungInput = (EditText) findViewById(R.id.editTextVorerkrankung);
        buttonView = (Button) findViewById(R.id.buttonView);
        buttonSaveAndUpdate = (Button) findViewById(R.id.buttonSaveAndUpdate);

        viewData();
        saveAndUpdateData();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        }
    }


    // Hierdurch lassen sich Informationen aus der DB anzeigen nach drücken des Buttons
    public void viewData() {
        Cursor data1 = userDataDB.showData(); // Unten ein Zweites mal; Dieses ist für die initiale Befüllung
        if (data1.getCount() != 0) {
            while(data1.moveToNext()) {
                vornameInput.setText(data1.getString(1));
                nachnameInput.setText(data1.getString(2));
                geburtsdatumInput.setText(data1.getString(3));
                adresseInput.setText(data1.getString(4));
                vorerkrankungInput.setText(data1.getString(5));
            }
        }

        buttonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor data = userDataDB.showData();
                if (data.getCount() == 0) {
                    display("Fehler", "Keine Daten gefunden.");
                    return;
                }
                StringBuffer buffer = new StringBuffer();
                while(data.moveToNext()) {
                    buffer.append("Vorname: " + data.getString(1) + "\n");
                    buffer.append("Nachname: " + data.getString(2) + "\n");
                    buffer.append("Geburtsdatum: " + data.getString(3) + "\n");
                    buffer.append("Adresse: " + data.getString(4) + "\n");
                    buffer.append("Vorerkrankung: " + data.getString(5) + "\n");
                }
                display("Alle gespeicherten Daten:", buffer.toString());
            }
        });


    }

    // Der finale Speichernbutton
    public void saveAndUpdateData() {
        buttonSaveAndUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // addData
                if (counterForButton == 0) {
                    counterForButton++;
                    String vorname = vornameInput.getText().toString();
                    String nachname = nachnameInput.getText().toString();
                    String geburtsdatum = geburtsdatumInput.getText().toString();
                    String adresse = adresseInput.getText().toString();
                    String vorerkrankung = vorerkrankungInput.getText().toString();

                    boolean insertData = userDataDB.addData(vorname, nachname, geburtsdatum, adresse, vorerkrankung);
                    if (insertData == true) {
                        showToast("Eingaben gespeichert.");
                    } else {
                        showToast("Etwas ist schief gelaufen");
                    }
                } else {

                    // updateData
                    boolean update = userDataDB.updateData("1", vornameInput.getText().toString(),
                            nachnameInput.getText().toString(), geburtsdatumInput.getText().toString(), adresseInput.getText().toString(),
                            vorerkrankungInput.getText().toString());
                    if (update == true) {
                        showToast("Aktualisierung erfolgreich.");
                    } else {
                        showToast("Etwas ist schief gelaufen");
                    }

                }
            }
        });
    }

    // genutzt um Informationen am Bildschirm anzuzeigen
    public void display(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    private void showToast(String text) {
        Toast.makeText(SettingsActivity.this, text, Toast.LENGTH_LONG).show();
    }


}
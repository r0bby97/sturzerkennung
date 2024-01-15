package com.example.sturzerkennung;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PopUp extends MainActivity {

    private SeekBar seekBar_sos;
    private Button button_allOk;
    private TextView textViewTimer;
    String stringBreitengrad, stringLaengengrad, stringNotfallAdresse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pop_up_window);

        // Werte bekommen und Zwischenspeichern, später zurück zu MainActivity
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        stringBreitengrad = bundle.getString("Breitengrad");
        stringLaengengrad = bundle.getString("Längengrad");
        stringNotfallAdresse = bundle.getString("NotfallAdresse");

        // Initialisierung
        button_allOk = (Button) findViewById(R.id.button_allOk);
        seekBar_sos = (SeekBar) findViewById(R.id.seekBar_sos);
        textViewTimer = (TextView) findViewById(R.id.textViewTimer);


        final Handler handler = new Handler(); // Hier oben für CodeZeile (45) handler.removeCallbacksAndMessages(null);

        // Wenn Button gedrückt wird, dann zur Anwendung zurück
        button_allOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                handler.removeCallbacksAndMessages(null); // verhinder das Absendes des Notrufs, wenn man abgebrochen hat
                finish();
            }
        });

        // Slider Eintellungen
        seekBar_sos.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (seekBar.getProgress() > 95) {
                    hilfe(stringBreitengrad, stringLaengengrad, stringNotfallAdresse);
                    finish();
                } else {

                    seekBar.setThumb(getResources().getDrawable(R.mipmap.ic_launcher));
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if(progress>95){
                    seekBar.setThumb(getResources().getDrawable(R.mipmap.ic_launcher));
                }

            }
        });

        // Fenstereinstellungen
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int) (width * 0.8), (int) (height * 0.42));


        // Timer schließt Fenster und sendet Notruf
        int finishTime = 30; //30 Sekunden
        // Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                hilfe(stringBreitengrad, stringLaengengrad, stringNotfallAdresse);
            }
        }, finishTime * 1000);


        // Timer zählt von 30 herunter und setzt es ins TextView
        final Handler handler1 = new Handler();
        Runnable runnable1 = new Runnable() {
            int count = 30;
            @Override
            public void run() {
                count--;
                if (count >= 10) {
                    textViewTimer.setText("00:" + count);
                } else {
                    textViewTimer.setText("00:0" + count);
                                }
                handler1.postDelayed(this, 1000);
            }
        };
        handler1.postDelayed(runnable1, 1000);
    }

}

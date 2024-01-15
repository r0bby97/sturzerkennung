package com.example.sturzerkennung;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;


public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "userData.db";
    public static final String TABLE_NAME = "userData_table";
    public static final String COL1 = "ID";
    public static final String COL2 = "VORNAME";
    public static final String COL3 = "NACHNAME";
    public static final String COL4 = "GEBURTSTAG";
    public static final String COL5 = "ADRESSE";
    public static final String COL6 = "VORERKRANKUNG";


    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "VORNAME TEXT, NACHNAME TEXT, GEBURTSTAG TEXT, ADRESSE TEXT, VORERKRANKUNG TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addData(String vorname, String nachname, String geburtstag, String adresse, String vorerkrankung) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL2, vorname);
        contentValues.put(COL3, nachname);
        contentValues.put(COL4, geburtstag);
        contentValues.put(COL5, adresse);
        contentValues.put(COL6, vorerkrankung);

        long result = db.insert(TABLE_NAME, null, contentValues);
        // Falls insert nicht funktiort, wird -1 zurück gegeben
        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public boolean updateData(String id, String vorname, String nachname, String geburtstag, String adresse, String vorerkrankung) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1, id);
        contentValues.put(COL2, vorname);
        contentValues.put(COL3, nachname);
        contentValues.put(COL4, geburtstag);
        contentValues.put(COL5, adresse);
        contentValues.put(COL6, vorerkrankung);

        db.update(TABLE_NAME, contentValues, "ID='1'", null);
        return true;
    }

    public Cursor showData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        return data;
    }
}

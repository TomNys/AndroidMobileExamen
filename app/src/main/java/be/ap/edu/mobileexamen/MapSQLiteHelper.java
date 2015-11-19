package be.ap.edu.mobileexamen;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapSQLiteHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "mapsaver.db";
        private static final String TABLE_BIBLIOTHEKEN = "biblios";
        private static final int DATABASE_VERSION = 15;

        public MapSQLiteHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_BIBLIOTHEKEN + "(_id INTEGER PRIMARY KEY, naam STRING, point_lat STRING, point_lng STRING, grondopp STRING)";
            db.execSQL(CREATE_USERS_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BIBLIOTHEKEN);
            onCreate(db);
        }

        public ArrayList<Bibliotheek> getAllBibs() {
            ArrayList allBibs = new ArrayList<Bibliotheek>();
            SQLiteDatabase db = this.getReadableDatabase();
            //int count = db.rawQuery("select * from " + TABLE_ZONES, null).getCount();
            //Log.d("edu.ap.mapsaver", "Count : " + count);
            Cursor cursor = db.rawQuery("select * from " + TABLE_BIBLIOTHEKEN, null);
            if (cursor.moveToFirst()) {
                do {
                    String naam = cursor.getString(1);
                    String point_lat = cursor.getString(2);
                    String point_long = cursor.getString(3);
                    String grondopp = cursor.getString(4);
                    allBibs.add(new Bibliotheek(naam, point_lat, point_long, grondopp));
                } while (cursor.moveToNext());
            }

            return allBibs;
        }

        public void saveBiblio(JSONArray allBibs) {
            SQLiteDatabase db = this.getWritableDatabase();
            for (int i = 0; i < allBibs.length(); i++) {
                try {
                    JSONObject obj = (JSONObject) allBibs.get(i);
                    String naam = obj.getString("naam");
                    String point_lat = obj.getString("point_lat");
                    String point_lng = obj.getString("point_lng");
                    String grondOpp = obj.getString("grondopp");


                    ContentValues values = new ContentValues();
                    values.put("naam", naam);
                    values.put("point_lat", point_lat);
                    values.put("point_lng", point_lng);
                    values.put("grondopp", grondOpp);

                    db.insert(TABLE_BIBLIOTHEKEN, null, values);
                }
                catch(Exception ex) {
                    Log.e("edu.ap.mapsaver", ex.getMessage());
                }
            }
            db.close();
        }
}


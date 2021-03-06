package ru.ifmo.android_2015.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import ru.ifmo.android_2015.json.CityJsonParser;
import ru.ifmo.android_2015.json.CityParserCallback;
import ru.ifmo.android_2015.util.ObservableInputStream;
import ru.ifmo.android_2015.util.ProgressCallback;

public abstract class CityFileImporter implements CityParserCallback {

//    private SQLiteDatabase db;
    private int importedCount;

    private SQLiteStatement insert;

    public CityFileImporter(/*SQLiteDatabase db*/ SQLiteStatement insert) {
//        this.db = db;
        this.insert = insert;
    }

    public final synchronized void importCities(File srcFile,
                                                ProgressCallback progressCallback)
            throws IOException {

        InputStream in = null;

        try {
            long fileSize = srcFile.length();
            in = new FileInputStream(srcFile);
            in = new BufferedInputStream(in);
            in = new ObservableInputStream(in, fileSize, progressCallback);
            in = new GZIPInputStream(in);
            importCities(in);

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to close file: " + e, e);
                }
            }
        }
    }

    protected abstract CityJsonParser createParser();

    private void importCities(InputStream in) {
        CityJsonParser parser = createParser();
        try {
            parser.parseCities(in, this);

        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
        }
    }

    @Override
    public void onCityParsed(long id, String name, String country, double lat, double lon) {
        insertCity(/*db*/ insert, id, name, country, lat, lon);
        importedCount++;
        if (importedCount % 1000 == 0) {
            Log.d(LOG_TAG, "Processed " + importedCount + " cities");
        }
    }

    private boolean insertCity(/*SQLiteDatabase db, */
                               SQLiteStatement insert,
                               long id,
                               @NonNull String name,
                               @NonNull String country,
                               double latitude,
                               double longitude) {

//        final ContentValues values = new ContentValues();
//        values.put(CityContract.CityColumns.CITY_ID, id);
//        values.put(CityContract.CityColumns.NAME, name);
//        values.put(CityContract.CityColumns.COUNTRY, country);
//        values.put(CityContract.CityColumns.LATITUDE, latitude);
//        values.put(CityContract.CityColumns.LONGITUDE, longitude);

//        long rowId = db.insert(CityContract.Cities.TABLE, null /*nullColumnHack not needed*/, values);

        insert.bindLong(1, id);
        insert.bindString(2, name);
        insert.bindString(3, country);
        insert.bindDouble(4, latitude);
        insert.bindDouble(5, longitude);
        long rowId = insert.executeInsert();

        if (rowId < 0) {
            Log.w(LOG_TAG, "Failed to insert city: id=" + id + " name=" + name);
            return false;
        }
        return true;
    }

    private static final String LOG_TAG = "CityReader";

}

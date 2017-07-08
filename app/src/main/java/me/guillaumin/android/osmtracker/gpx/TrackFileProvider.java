package me.guillaumin.android.osmtracker.gpx;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

/**
 */

public final class TrackFileProvider extends ContentProvider {
    private static final String TAG = ExportToTempFileTask.class.getSimpleName();

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return new AbstractCursor() {
            @Override
            public int getCount() {
                return 0;
            }

            @Override
            public String[] getColumnNames() {
                return new String[0];
            }

            @Override
            public String getString(int column) {
                return null;
            }

            @Override
            public short getShort(int column) {
                return 0;
            }

            @Override
            public int getInt(int column) {
                return 0;
            }

            @Override
            public long getLong(int column) {
                return 0;
            }

            @Override
            public float getFloat(int column) {
                return 0;
            }

            @Override
            public double getDouble(int column) {
                return 0;
            }

            @Override
            public boolean isNull(int column) {
                return false;
            }
        };
    }

    @Override
    public String getType(Uri uri) {
        return "application/xml";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        String[] mimeTypes = {"application/xml", "application/gpx", "application/gpx+xml", "application/xml+gpx", "text/xml"};
        String mimeTypesFiltered = "";
        for (String mimeType : mimeTypes)
            if (mimeTypeFilter.contains(mimeType))
                mimeTypesFiltered += " " + mimeType;
        return mimeTypesFiltered.split(" ");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
    {
        ParcelFileDescriptor fileDescriptor = null;
        File sharedFile = ExportAndShareTask.sharedFiles.get(uri.getLastPathSegment());
        if (sharedFile != null)
            try {
                fileDescriptor = ParcelFileDescriptor.open(sharedFile, ParcelFileDescriptor.MODE_READ_ONLY);
            }
            catch (FileNotFoundException exception) {
                Log.e(TAG, "Could not find temporary file", exception);
            }
        else
            Log.e(TAG, "Requested file is not shared");
        return fileDescriptor;
    }
}

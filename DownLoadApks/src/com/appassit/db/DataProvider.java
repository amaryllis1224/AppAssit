package com.appassit.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.appassit.activitys.SLAppication;

/**
 */
public class DataProvider extends ContentProvider {

	protected static final String TAG = "DataProvider";

	/**
	 * DB锁
	 */
	protected static final Object DBLock = new Object();

	protected static final String AUTHORITY = "com.superassit.provider";

	public static final String SCHEME = "content://";

	// message
	public static final String PATH_WEATHER = "/weathers";

	public static final Uri WEATHER_CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_WEATHER);

	private static final int WEATHER = 0;

	public static final String WEATHER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.superassit.weather";

	private static final UriMatcher mUriMatcher;

	static {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(AUTHORITY, "weathers", WEATHER);
	}

	private static DBHelper mDbHelper;

	public static DBHelper getDBHelper() {
		if (mDbHelper == null) {
			mDbHelper = new DBHelper(SLAppication.getContext());
		}
		return mDbHelper;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		synchronized (DBLock) {
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			String table = matchTable(uri);
			queryBuilder.setTables(table);

			SQLiteDatabase db = getDBHelper().getReadableDatabase();
			// Cursor cursor = query(uri, projection, selection, selectionArgs, sortOrder);
			Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			return cursor;
		}
	}

	@Override
	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case WEATHER:
			return WEATHER_CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknow URI" + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		synchronized (DBLock) {
			String table = matchTable(uri);
			SQLiteDatabase db = getDBHelper().getWritableDatabase();
			long rowId = 0;
			db.beginTransaction();
			try {
				rowId = db.insert(table, null, values);
				db.setTransactionSuccessful();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				db.endTransaction();
			}
			if (rowId > 0) {
				Uri returnUri = ContentUris.withAppendedId(uri, rowId);
				getContext().getContentResolver().notifyChange(uri, null);
				return returnUri;
			}
			throw new SQLException("Failed to insert row into" + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		synchronized (DBLock) {
			SQLiteDatabase db = getDBHelper().getWritableDatabase();

			int count = 0;
			String table = matchTable(uri);
			db.beginTransaction();
			try {
				count = db.delete(table, selection, selectionArgs);
				db.setTransactionSuccessful();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				db.endTransaction();
			}
			getContext().getContentResolver().notifyChange(uri, null);
			return count;
		}
	}

	@SuppressWarnings("finally")
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		synchronized (DBLock) {
			SQLiteDatabase db = getDBHelper().getWritableDatabase();
			String table = matchTable(uri);

			int count = 0;
			db.beginTransaction();
			try {
				count = db.update(table, values, selection, selectionArgs);
				db.setTransactionSuccessful();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				getContext().getContentResolver().notifyChange(uri, null);
				return count;
			}
		}
	}

	protected String matchTable(Uri uri) {
		String table = null;
		switch (mUriMatcher.match(uri)) {
		case WEATHER:
			table = SunDataHelper.SunDBInfo.TABLE_NAME;
			break;
		default:
			throw new IllegalArgumentException("Unknow URI" + uri);
		}
		return table;
	}
}

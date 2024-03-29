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
package de.eidottermihi.rpicheck.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.eidottermihi.raspicheck.R;
import de.eidottermihi.rpicheck.db.CommandBean;
import de.eidottermihi.rpicheck.db.DeviceDbHelper;
import de.eidottermihi.rpicheck.db.RaspberryDeviceBean;
import de.eidottermihi.rpicheck.fragment.CommandPlaceholdersDialog;
import de.eidottermihi.rpicheck.fragment.CommandPlaceholdersDialog.PlaceholdersDialogListener;
import de.eidottermihi.rpicheck.fragment.PassphraseDialog;
import de.eidottermihi.rpicheck.fragment.PassphraseDialog.PassphraseDialogListener;
import de.eidottermihi.rpicheck.fragment.RunCommandDialog;
import de.eidottermihi.rpicheck.ssh.beans.Exported;
import io.freefair.android.injection.annotation.InjectView;
import io.freefair.android.injection.annotation.XmlLayout;
import io.freefair.android.injection.annotation.XmlMenu;
import io.freefair.android.injection.app.InjectionAppCompatActivity;

@XmlLayout(R.layout.activity_commands)
@XmlMenu(R.menu.activity_commands)
public class CustomCommandActivity extends InjectionAppCompatActivity implements OnItemClickListener, PassphraseDialogListener, PlaceholdersDialogListener {
    private static final int REQUEST_READ_PERMISSION_FOR_COMMAND = 1;

    private RaspberryDeviceBean currentDevice;

    @InjectView(R.id.commandListView)
    private ListView commandListView;

    private DeviceDbHelper deviceDb = new DeviceDbHelper(this);

    private Cursor fullCommandCursor;

    private Pattern dynamicPlaceHolderPattern = Pattern.compile("(\\$\\{[^*\\}]+\\})");
    private Pattern nonPromptingPlaceHolders = Pattern.compile("(\\%\\{[^*\\}]+\\})");

    private long commandId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show the Up button in the action bar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        Bundle extras = this.getIntent().getExtras();
        if (extras != null && extras.get("pi") != null) {
            currentDevice = (RaspberryDeviceBean) extras.get("pi");
        } else if (savedInstanceState.getSerializable("pi") != null) {
            currentDevice = (RaspberryDeviceBean) savedInstanceState.getSerializable("pi");
        }
        if (currentDevice != null) {
            getSupportActionBar().setTitle(currentDevice.getName());
            this.initListView(currentDevice);
        } else {
        }

    }

    /**
     * Init ListView with commands.
     *
     * @param pi
     */
    private void initListView(RaspberryDeviceBean pi) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                fullCommandCursor = deviceDb.getFullCommandCursor();
                return null;
            }

            @Override
            protected void onPostExecute(Void r) {
                CommandAdapter commandsAdapter = new CommandAdapter(CustomCommandActivity.this, fullCommandCursor, CursorAdapter.FLAG_AUTO_REQUERY);
                commandListView.setAdapter(commandsAdapter);
                commandListView.setOnItemClickListener(CustomCommandActivity.this);
                // commandListView.setOnItemLongClickListener(CustomCommandActivity.this);
                registerForContextMenu(commandListView);
            }
        }.execute();

    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.commandListView) {
            final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            CommandBean cmd = deviceDb.readCommand(info.id);
            menu.setHeaderTitle(cmd.getName());
            menu.add(Menu.NONE, 1, 1, R.string.command_context_edit);
            menu.add(Menu.NONE, 2, 2, R.string.command_context_delete);
            menu.add(Menu.NONE, 3, 3, R.string.command_context_run);

        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                break;
            case R.id.menu_new_command:
                // init intent
                Intent newCommandIntent = new Intent(CustomCommandActivity.this, NewCommandActivity.class);
                newCommandIntent.putExtras(this.getIntent().getExtras());
                this.startActivityForResult(newCommandIntent, NewCommandActivity.REQUEST_NEW);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        switch (menuItemIndex) {
            case 1:
                Intent newCommandIntent = new Intent(CustomCommandActivity.this, NewCommandActivity.class);
                newCommandIntent.putExtras(this.getIntent().getExtras());
                newCommandIntent.putExtra(NewCommandActivity.CMD_KEY_EDIT, info.id);
                this.startActivityForResult(newCommandIntent, NewCommandActivity.REQUEST_EDIT);
                break;
            case 2:
                this.deleteCommand(info.id);
                break;
            case 3:
                this.commandId = info.id;
                this.runCommand(info.id);
                break;
            default:
                break;
        }
        return true;
    }

    private void deleteCommand(final long id) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                deviceDb.deleteCommand(id);
                return null;
            }

            @Override
            protected void onPostExecute(Void r) {
                CustomCommandActivity.this.initListView(currentDevice);
            }
        }.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NewCommandActivity.REQUEST_NEW && resultCode == RESULT_OK) {
            // new cmd saved, update...
            Toast.makeText(this, R.string.toast_command_saved, Toast.LENGTH_SHORT).show();
            initListView(currentDevice);
        } else if (requestCode == NewCommandActivity.REQUEST_EDIT && resultCode == RESULT_OK) {
            Toast.makeText(this, R.string.toast_command_updated, Toast.LENGTH_SHORT).show();
            initListView(currentDevice);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentDevice != null) {
            outState.putSerializable("pi", currentDevice);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int itemPos, long itemId) {
        runCommand(itemId);
    }

    private void runCommand(long commandId) {
        this.commandId = commandId;
        ConnectivityManager connMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (currentDevice.usesAuthentificationMethod(RaspberryDeviceBean.AUTH_PUBLIC_KEY)
                    || currentDevice.usesAuthentificationMethod(RaspberryDeviceBean.AUTH_PUBLIC_KEY_WITH_PASSWORD)) {
                // need permission to read keyfile
                final String keyfilePath = currentDevice.getKeyfilePath();
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_READ_PERMISSION_FOR_COMMAND);
                    return;
                }
                if (currentDevice.usesAuthentificationMethod(RaspberryDeviceBean.AUTH_PUBLIC_KEY_WITH_PASSWORD)
                        && Strings.isNullOrEmpty(currentDevice.getKeyfilePass())) {
                    // must ask for key passphrase first
                    // dirty hack, saving commandId as "dialog type"
                    final String dialogType = commandId + "";
                    final DialogFragment passphraseDialog = new PassphraseDialog();
                    final Bundle args = new Bundle();
                    args.putString(PassphraseDialog.KEY_TYPE, dialogType);
                    passphraseDialog.setArguments(args);
                    passphraseDialog.setCancelable(false);
                    passphraseDialog.show(getSupportFragmentManager(), "passphrase");
                    return;
                }
            }
            openCommandDialog(commandId, currentDevice.getKeyfilePass());
        } else {
            Toast.makeText(this, R.string.no_connection, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_PERMISSION_FOR_COMMAND:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            runCommand(commandId);
                        }
                    }, 200);
                } else {
                    Toast.makeText(this, R.string.permission_private_key_error, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putLong("lastCommandId", commandId).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final long lastCommandId = sharedPreferences.getLong("lastCommandId", -1L);
        if (lastCommandId != -1L) {
            this.commandId = lastCommandId;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceDb != null) {
            deviceDb.close();
        }
    }

    /**
     * Opens the command dialog.
     *
     * @param keyPassphrase nullable: key passphrase
     */
    private void openCommandDialog(final long commandId, final String keyPassphrase) {
        final CommandBean command = deviceDb.readCommand(commandId);
        final ArrayList<String> dynamicPlaceholders = parseDynamicPlaceholders(command.getCommand());
        if (!dynamicPlaceholders.isEmpty()) {
            // need to get replacements for dynamic placeholders first
            DialogFragment placeholderDialog = new CommandPlaceholdersDialog();
            Bundle args2 = new Bundle();
            args2.putStringArrayList(CommandPlaceholdersDialog.ARG_PLACEHOLDERS, dynamicPlaceholders);
            args2.putSerializable(CommandPlaceholdersDialog.ARG_COMMAND, command);
            args2.putString(CommandPlaceholdersDialog.ARG_PASSPHRASE, keyPassphrase);
            placeholderDialog.setArguments(args2);
            placeholderDialog.show(getSupportFragmentManager(), "placeholders");
            return;
        }
        parseNonPromptingAndShow(keyPassphrase, command);
    }

    private void parseNonPromptingAndShow(String keyPassphrase, CommandBean command) {
        final DialogFragment runCommandDialog = new RunCommandDialog();
        final Bundle args = new Bundle();
        String cmdString = command.getCommand();
        Map<String, String> nonPromptingPlaceholders = parseNonPromptingPlaceholders(command.getCommand(), currentDevice);
        for (Map.Entry<String, String> entry : nonPromptingPlaceholders.entrySet()) {
            cmdString = cmdString.replace(entry.getKey(), entry.getValue());
        }
        command.setCommand(cmdString);
        args.putSerializable("pi", currentDevice);
        args.putSerializable("cmd", command);
        if (keyPassphrase != null) {
            args.putString("passphrase", keyPassphrase);
        }
        runCommandDialog.setArguments(args);
        runCommandDialog.show(getSupportFragmentManager(), "runCommand");
    }

    private Map<String, String> parseNonPromptingPlaceholders(String command, RaspberryDeviceBean currentDevice) {
        Map<String, String> nonPromptingPlaceholders = new HashMap<>();
        Matcher m = nonPromptingPlaceHolders.matcher(command);
        while (m.find()) {
            String placeholder = m.group();
            String placeholderValue = placeholder.substring(2, placeholder.length() - 1);
            if (placeholderValue.startsWith("pi.")) {
                // accessing properties of current pi device
                List<String> splitToList = Splitter.on('.').splitToList(placeholderValue);
                if (splitToList.size() == 2) {
                    String accessor = splitToList.get(1);
                    String value = getValueViaReflection(currentDevice, accessor);
                    if (value != null) {
                        nonPromptingPlaceholders.put(placeholder, value);
                    }
                } else {
                }
            } else if (placeholderValue.startsWith("date(")) {
                // parse format in braces
                final String format = placeholderValue.substring(5, placeholderValue.length() - 1);
                try {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
                    String value = simpleDateFormat.format(new Date());
                    nonPromptingPlaceholders.put(placeholder, value);
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return nonPromptingPlaceholders;
    }

    private String getValueViaReflection(RaspberryDeviceBean device, String accessor) {
        for (Method method : device.getClass().getMethods()) {
            if (method.isAnnotationPresent(Exported.class)) {
                if (method.getName().replaceFirst("get", "").toLowerCase().equals(accessor.toLowerCase())) {
                    try {
                        Object result = method.invoke(device);
                        if (result != null) {
                            return result.toString();
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    private ArrayList<String> parseDynamicPlaceholders(String commandString) {
        ArrayList<String> placeholders = new ArrayList<>();
        Matcher m = dynamicPlaceHolderPattern.matcher(commandString);
        while (m.find()) {
            String placeholder = m.group();
            placeholders.add(placeholder);
        }
        return placeholders;
    }

    @Override
    public void onPassphraseOKClick(DialogFragment dialog, String passphrase, boolean savePassphrase, String type) {
        if (savePassphrase) {
            currentDevice.setKeyfilePass(passphrase);
            currentDevice.setModifiedAt(new Date());
            new Thread() {
                @Override
                public void run() {
                    deviceDb.update(currentDevice);
                }
            }.start();
        }
        // dirty hack: type is commandId
        Long commandId = Long.parseLong(type);
        openCommandDialog(commandId, passphrase);
    }

    @Override
    public void onPassphraseCancelClick() {
        // do nothing
    }

    @Override
    public void onPlaceholdersOKClick(CommandBean command, String keyPassphrase) {
        parseNonPromptingAndShow(keyPassphrase, command);
    }

    @Override
    public void onPlaceholdersCancelClick() {
        // do nothing
    }

}

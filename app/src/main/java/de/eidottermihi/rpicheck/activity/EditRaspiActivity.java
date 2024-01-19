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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.eidottermihi.raspicheck.R;
import de.eidottermihi.rpicheck.activity.helper.Constants;
import de.eidottermihi.rpicheck.activity.helper.Validation;
import de.eidottermihi.rpicheck.db.DeviceDbHelper;
import de.eidottermihi.rpicheck.db.RaspberryDeviceBean;
import io.freefair.android.injection.annotation.InjectView;
import io.freefair.android.injection.annotation.XmlLayout;
import io.freefair.android.injection.annotation.XmlMenu;

@XmlLayout(R.layout.activity_raspi_edit)
@XmlMenu(R.menu.activity_raspi_edit)
public class EditRaspiActivity extends AbstractFileChoosingActivity implements OnItemSelectedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditRaspiActivity.class);

    public static final int REQUEST_EDIT = 10;

    // assigning view elements to fields
    @InjectView(R.id.edit_raspi_name_editText)
    private EditText editTextName;
    @InjectView(R.id.edit_raspi_host_editText)
    private EditText editTextHost;
    @InjectView(R.id.edit_raspi_user_editText)
    private EditText editTextUser;
    @InjectView(R.id.ssh_password_edit_text)
    private EditText editTextPass;
    @InjectView(R.id.edit_raspi_ssh_port_editText)
    private EditText editTextSshPortOpt;
    @InjectView(R.id.edit_raspi_desc_editText)
    private EditText editTextDescription;
    @InjectView(R.id.edit_raspi_sudoPass_editText)
    private EditText editTextSudoPass;

    @InjectView(R.id.ssh_password_layout)
    private TextInputLayout sshPasswordLayout;


    private DeviceDbHelper deviceDb;
    private RaspberryDeviceBean deviceBean;

    private Validation validator = new Validation();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raspi_edit);
        // Show the Up button in the action bar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init sql db
        deviceDb = new DeviceDbHelper(this);

        // read device information
        if (this.getIntent().hasExtra(Constants.EXTRA_DEVICE_ID)) {
            int deviceId = this.getIntent().getExtras().getInt(Constants.EXTRA_DEVICE_ID);
            deviceBean = deviceDb.read(deviceId);
        } else {
            deviceBean = new RaspberryDeviceBean();
            deviceBean.setPort(22);
            deviceBean.setAuthMethod(RaspberryDeviceBean.AUTH_PASSWORD);
        }

        fillFromBean();
    }

    private void fillFromBean() {
        // fill fields first that will always be shown
        editTextName.setText(deviceBean.getName());
        editTextHost.setText(deviceBean.getHost());
        editTextUser.setText(deviceBean.getUser());
        editTextSshPortOpt.setText(deviceBean.getPort() + "");
        editTextDescription.setText(deviceBean.getDescription());
        editTextSudoPass.setText(deviceBean.getSudoPass());

        // switch auth method
        final String method = deviceBean.getAuthMethod();
        switchAuthMethodsInView(method);
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
                return true;
            case R.id.menu_save:
                updateRaspi();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addRaspiToDb(final String name, final String host, final String user,
                              final String authMethod, String sshPort, final String description,
                              String sudoPass, final String sshPass, final String keyPass, final String keyPath) {
        // if sshPort is empty, use default port (22)
        if (Strings.isNullOrEmpty(sshPort)) {
            sshPort = getText(R.string.default_ssh_port).toString();
        }
        if (Strings.isNullOrEmpty(sudoPass)) {
            sudoPass = "";
        }
        final String port = sshPort, pass = sudoPass;
        new Thread() {
            @Override
            public void run() {
                deviceDb.create(name, host, user, sshPass, Integer.parseInt(port),
                        description, pass, authMethod, keyPath, keyPass);
            }
        }.start();
    }

    private void updateRaspi() {
        boolean validationSuccessful = validator.validatePiEditData(this, editTextName, editTextHost, editTextUser,
                editTextPass, editTextSshPortOpt, editTextSudoPass);
        if (validationSuccessful) {
            // getting credentials from textfields
            final String name = editTextName.getText().toString().trim();
            final String host = editTextHost.getText().toString().trim();
            final String user = editTextUser.getText().toString().trim();
            final String sshPort = editTextSshPortOpt.getText().toString().trim();
            final String sudoPass = editTextSudoPass.getText().toString().trim();
            final String description = editTextDescription.getText().toString().trim();
            final String pass = editTextPass.getText().toString().trim();

            if (getIntent().hasExtra(Constants.EXTRA_DEVICE_ID)){
                updateRaspiInDb(name, host, user, pass, sshPort, description, sudoPass,
                        RaspberryDeviceBean.AUTH_PASSWORD,
                        null, null);
            } else {
                addRaspiToDb(name, host, user, RaspberryDeviceBean.AUTH_PASSWORD, sshPort, description, sudoPass, pass, null, null);
            }

            Toast.makeText(this, R.string.update_successful, Toast.LENGTH_SHORT).show();
            // back to main
            this.setResult(RESULT_OK);
            this.finish();
        }
    }

    private void updateRaspiInDb(String name, String host, String user,
                                 String pass, String sshPort, String description, String sudoPass,
                                 String authMethod, String keyfilePath, String keyfilePass) {
        // if sudoPass is null use empty pass
        if (Strings.isNullOrEmpty(sudoPass)) {
            sudoPass = "";
        }
        deviceBean.setName(name);
        deviceBean.setHost(host);
        deviceBean.setUser(user);
        deviceBean.setPass(pass);
        deviceBean.setPort(Integer.parseInt(sshPort));
        deviceBean.setDescription(description);
        deviceBean.setSudoPass(sudoPass);
        deviceBean.setAuthMethod(authMethod);
        deviceBean.setKeyfilePath(keyfilePath);
        deviceBean.setKeyfilePass(keyfilePass);
        new Thread() {
            @Override
            public void run() {
                deviceDb.update(deviceBean);
            }
        }.start();
    }

    private void switchAuthMethodsInView(String method) {
        if (method.equals(RaspberryDeviceBean.AUTH_PASSWORD)) {
            // show only ssh password
            sshPasswordLayout.setVisibility(View.VISIBLE);
            editTextPass.setText(deviceBean.getPass());
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
                               long arg3) {
        final String selectedAuthMethod = RaspberryDeviceBean.SPINNER_AUTH_METHODS[pos];
        LOGGER.debug("Auth method selected: {}", selectedAuthMethod);
        this.switchAuthMethodsInView(selectedAuthMethod);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {

    }

}
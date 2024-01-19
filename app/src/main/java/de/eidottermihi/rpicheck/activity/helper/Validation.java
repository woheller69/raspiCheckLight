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
package de.eidottermihi.rpicheck.activity.helper;

import android.content.Context;
import android.widget.EditText;

import com.google.common.base.Strings;

import de.eidottermihi.raspicheck.R;

/**
 * A helper class for validating user input.
 *
 * @author Michael
 */
public class Validation {

    /**
     * Validates if user input is valid.
     *
     * @param context
     * @param commandEditText
     * @param timeoutEditText
     * @return true if valid
     */
    public boolean validateNewCmdData(Context context, EditText commandEditText, EditText timeoutEditText) {
        boolean dataValid = true;
        if (!checkNonOptionalTextField(commandEditText,
                context.getString(R.string.validation_command_blank))) {
            dataValid = false;
        }
        if (!validateTimeout(timeoutEditText)) {
            dataValid = false;
        }
        return dataValid;
    }

    /**
     * Validates if user input is valid.
     *
     * @param name
     * @param host
     * @param user
     * @param password
     * @param port
     * @param sudoPass
     * @return true, if input is valid
     */
    public boolean validatePiEditData(Context context, EditText name, EditText host, EditText user, EditText password,
                                      EditText port, EditText sudoPass) {
        boolean dataValid = true;
        // check non-optional fields
        if (!checkNonOptionalTextField(name,
                context.getString(R.string.validation_msg_name))) {
            dataValid = false;
        }
        if (!checkNonOptionalTextField(host,
                context.getString(R.string.validation_msg_host))) {
            dataValid = false;
        }
        if (!checkNonOptionalTextField(user,
                context.getString(R.string.validation_msg_user))) {
            dataValid = false;
        }
        if (!validatePort(port)) {
            dataValid = false;
        }
        // check auth method
            // ssh password must be present
            if (!checkNonOptionalTextField(password,
                    context.getString(R.string.validation_msg_password))) {
                dataValid = false;
            }
        return dataValid;
    }

    /**
     * Validates if user input for core data is valid.
     *
     * @param name
     * @param host
     * @param user
     * @return true, if input is valid
     */
    public boolean validatePiCoreData(Context context, EditText name,
                                      EditText host, EditText user) {
        boolean dataValid = true;
        if (!checkNonOptionalTextField(name,
                context.getString(R.string.validation_msg_name))) {
            dataValid = false;
        }
        if (!checkNonOptionalTextField(host,
                context.getString(R.string.validation_msg_host))) {
            dataValid = false;
        }
        if (!checkNonOptionalTextField(user,
                context.getString(R.string.validation_msg_user))) {
            dataValid = false;
        }
        return dataValid;
    }

    /**
     * Checks if a Textfield is not blank.
     *
     * @param textfield    the EditText to check
     * @param errorMessage the errorMessage to set if validation fails
     * @return true, if valid, else false
     */
    public boolean checkNonOptionalTextField(EditText textfield,
                                             String errorMessage) {
        // get text
        final String text = textfield.getText().toString().trim();
        if (Strings.isNullOrEmpty(text)) {
            textfield.setError(errorMessage);
            return false;
        }
        return true;
    }

    public boolean validatePort(EditText editTextSshPort) {
        boolean portValid = true;
        // range 1 to 65535
        try {
            final Long sshPort = Long.parseLong(editTextSshPort.getText()
                    .toString());
            if (sshPort < 1 || sshPort > 65535) {
                portValid = false;
            }
        } catch (NumberFormatException e) {
            portValid = false;
        }
        if (!portValid) {
            editTextSshPort.setError(editTextSshPort.getContext().getText(
                    R.string.validation_msg_port));
        }
        return portValid;
    }

    public boolean validateTimeout(EditText timeoutEditText) {
        boolean timeoutValid = true;
        // minimum 1
        try {
            final Integer timeout = Integer.parseInt(timeoutEditText.getText()
                    .toString());
            if (timeout < 1) {
                timeoutValid = false;
            }
        } catch (NumberFormatException e) {
            timeoutValid = false;
        }
        if (!timeoutValid) {
            timeoutEditText.setError(timeoutEditText.getContext().getText(
                    R.string.validation_msg_timeout));
        }
        return timeoutValid;
    }

}

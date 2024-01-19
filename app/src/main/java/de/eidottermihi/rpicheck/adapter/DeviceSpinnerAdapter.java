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
package de.eidottermihi.rpicheck.adapter;

import android.content.Context;
import android.database.Cursor;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.eidottermihi.raspicheck.R;
import de.eidottermihi.rpicheck.activity.helper.CursorHelper;
import de.eidottermihi.rpicheck.db.RaspberryDeviceBean;

/**
 * Adapter for cursor-backed Device Spinners.
 */
public class DeviceSpinnerAdapter extends ResourceCursorAdapter {
    private final LayoutInflater inflater;

    /**
     * @param context the Context
     * @param c full device cursor
     * @param alwaysWithUserHost true, if user@host should always be displayed (not only in dropdown view)
     */
    public DeviceSpinnerAdapter(Context context, Cursor c, boolean alwaysWithUserHost) {
        super(context, alwaysWithUserHost ? R.layout.device_row_dropdown : R.layout.device_row,
                c, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        this.inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        RaspberryDeviceBean device = CursorHelper.readDevice(cursor);
        TextView name = (TextView) view.findViewById(R.id.deviceRowName);
        name.setText(device.getName());
        TextView userHost = (TextView) view.findViewById(R.id.deviceRowUserHost);
        if (userHost != null) {
            userHost.setText(String.format("%s@%s", device.getUser(), device.getHost()));
        }
    }

    @Override
    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.device_row_dropdown, parent, false);
    }


}

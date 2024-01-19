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

import android.os.AsyncTask;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.eidottermihi.rpicheck.ssh.beans.NetworkInterfaceInformation;
import de.eidottermihi.rpicheck.ssh.beans.ProcessBean;
import de.eidottermihi.rpicheck.beans.QueryBean;
import de.eidottermihi.rpicheck.ssh.beans.RaspiMemoryBean;
import de.eidottermihi.rpicheck.beans.UptimeBean;
import de.eidottermihi.rpicheck.ssh.beans.VcgencmdBean;
import de.eidottermihi.rpicheck.ssh.IQueryService;
import de.eidottermihi.rpicheck.ssh.LoadAveragePeriod;
import de.eidottermihi.rpicheck.ssh.impl.RaspiQuery;
import de.eidottermihi.rpicheck.ssh.impl.RaspiQueryException;

/**
 * @author Michael
 */
public class SSHQueryTask extends AsyncTask<String, Integer, QueryBean> {

    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getPercentInstance();

    private final AsyncQueryDataUpdate delegate;

    private LoadAveragePeriod loadAveragePeriod;

    public SSHQueryTask(AsyncQueryDataUpdate delegate,
                        LoadAveragePeriod loadAvgPeriod) {
        super();
        this.delegate = delegate;
        this.loadAveragePeriod = loadAvgPeriod;
    }

    public SSHQueryTask(AsyncQueryDataUpdate delegate) {
        super();
        this.delegate = delegate;
        this.loadAveragePeriod = LoadAveragePeriod.FIVE_MINUTES;
    }

    @Override
    protected QueryBean doInBackground(String... params) {
        // create and do query
        IQueryService queryService = new RaspiQuery((String) params[0], (String) params[1],
                Integer.parseInt(params[3]));
        final String pass = params[2];
        boolean hideRootProcesses = Boolean.parseBoolean(params[4]);
        final String privateKeyPath = params[5];
        final String privateKeyPass = params[6];
        QueryBean bean = new QueryBean();
        final long msStart = new Date().getTime();
        bean.setErrorMessages(new ArrayList<String>());
        try {
            publishProgress(5);
            if (privateKeyPath != null) {
                File f = new File(privateKeyPath);
                if (privateKeyPass == null) {
                    // connect with private key only
                    queryService.connectWithPubKeyAuth(f.getPath());
                } else {
                    // connect with key and passphrase
                    queryService.connectWithPubKeyAuthAndPassphrase(
                            f.getPath(), privateKeyPass);
                }
            } else {
                queryService.connect(pass);
            }
            publishProgress(20);
            final VcgencmdBean vcgencmdBean = queryService.queryVcgencmd();
            publishProgress(40);
            final Double loadAvg = queryService
                    .queryLoadAverage(this.loadAveragePeriod);
            publishProgress(50);
            final Double uptime = queryService.queryUptime();
            publishProgress(60);
            RaspiMemoryBean memory = queryService.queryMemoryInformation();
            publishProgress(65);
            bean.setModel(queryService.queryModel());
            publishProgress(70);
            String serialNo = queryService.queryCpuSerial();
            publishProgress(72);
            bean.setArchitecture(queryService.queryCpuArchitecture());
            publishProgress(75);
            List<ProcessBean> processes = queryService
                    .queryProcesses(!hideRootProcesses);
            publishProgress(80);
            final List<NetworkInterfaceInformation> networkInformation = queryService
                    .queryNetworkInformation();
            publishProgress(85);
            bean.setDisks(queryService.queryDiskUsage());
            publishProgress(90);
            bean.setDistri(queryService.queryDistributionName());
            publishProgress(92);
            bean.setKernelVer(queryService.queryKernelVersion());
            publishProgress(95);
            bean.setSystemtime(queryService.querySystemtime());
            queryService.disconnect();
            publishProgress(100);
            bean.setVcgencmdInfo(vcgencmdBean);
            bean.setLastUpdate(Calendar.getInstance().getTime());
            bean.setStartup(new UptimeBean(uptime).getRunningPretty());
            bean.setAvgLoad(NUMBER_FORMAT.format(loadAvg));
            bean.setMemoryBean(memory);
            bean.setSerialNo(serialNo);
            bean.setNetworkInfo(networkInformation);
            bean.setProcesses(processes);
            for (String error : bean.getErrorMessages()) {
            }
        } catch (RaspiQueryException e) {
            bean.setException(e);
        } finally {
            try {
                queryService.disconnect();
            } catch (RaspiQueryException e) {
            }
        }
        final long msFinish = new Date().getTime();
        final long durationInMs = msFinish - msStart;
        return bean;
    }

    @Override
    protected void onPostExecute(QueryBean result) {
        // update query data
        delegate.onQueryFinished(result);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        final Integer totalProgress = values[0];
        delegate.onQueryProgress(totalProgress);
        super.onProgressUpdate(values);
    }

}
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
package de.eidottermihi.rpicheck.ssh.impl.queries;

import com.google.common.base.Optional;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.eidottermihi.rpicheck.ssh.GenericQuery;
import de.eidottermihi.rpicheck.ssh.beans.NetworkInterfaceInformation;
import de.eidottermihi.rpicheck.ssh.beans.WlanBean;
import de.eidottermihi.rpicheck.ssh.impl.RaspiQueryException;

public class NetworkInformationQuery extends GenericQuery<List<NetworkInterfaceInformation>> {

    private static final Pattern IWCONFIG_LINK_PATTERN = Pattern.compile("Link Quality=([0-9]{1,3})\\/([0-9]{1,3})");
    private static final Pattern IWCONFIG_LEVEL_DBM_PATTERN = Pattern.compile("Signal level=(.*)\\s(dBm)");
    private static final Pattern IWCONFIG_LEVEL_PERCENTAGE_PATTERN = Pattern.compile("Signal level=([0-9]{1,3})\\/([0-9]{1,3})");
    private static final Pattern IPADDRESS_PATTERN = Pattern
            .compile("\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
    private static final Double LINK_QUALITY_MAXIMUM = 70.0;

    public NetworkInformationQuery(SSHClient sshClient) {
        super(sshClient);
    }

    @Override
    public List<NetworkInterfaceInformation> run() throws RaspiQueryException {
        List<NetworkInterfaceInformation> interfacesInfo = new ArrayList<NetworkInterfaceInformation>();
        // 1. find all network interfaces (excluding loopback interface) and
        // check carrier
        List<String> interfaces = this.queryInterfaceList();
        for (String interfaceName : interfaces) {
            NetworkInterfaceInformation interfaceInfo = new NetworkInterfaceInformation();
            interfaceInfo.setName(interfaceName);
            // check carrier
            interfaceInfo.setHasCarrier(this.checkCarrier(interfaceName));
            interfacesInfo.add(interfaceInfo);
        }
        List<NetworkInterfaceInformation> wirelessInterfaces = new ArrayList<NetworkInterfaceInformation>();
        // 2. for every interface with carrier check ip adress
        for (NetworkInterfaceInformation interfaceBean : interfacesInfo) {
            if (interfaceBean.isHasCarrier()) {
                interfaceBean.setIpAdress(this.queryIpAddress(interfaceBean
                        .getName()));
                // check if interface is wireless (interface name starts with
                // "wl")
                if (interfaceBean.getName().startsWith("wl")) {
                    // add to wireless interfaces list
                    wirelessInterfaces.add(interfaceBean);
                }
            }
        }
        // 3. query signal level and link status of wireless interfaces
        if (wirelessInterfaces.size() > 0) {
            this.queryWlanInfo(wirelessInterfaces);
        }
        return interfacesInfo;
    }

    /**
     * Queries the link level and signal quality of the wireless interfaces via iwconfig.
     *
     * @param wirelessInterfaces a List with wireless interfaces
     * @throws RaspiQueryException if something goes wrong
     */
    private void queryWlanInfo(
            List<NetworkInterfaceInformation> wirelessInterfaces)
            throws RaspiQueryException {
        // find path to to iwconfig executable
        Optional<String> iwconfigPath = findPathToExecutable("iwconfig");
        for (NetworkInterfaceInformation nic :
                wirelessInterfaces) {
            WlanBean wlanBean = this.queryWirelessInterface(nic.getName(), iwconfigPath);
            if (wlanBean != null) {
                nic.setWlanInfo(wlanBean);
            }
        }
    }

    /**
     * Uses "whereis" to find path to the specified executable.
     *
     * @param executableBinary
     * @return the first path
     */
    private Optional<String> findPathToExecutable(String executableBinary) throws RaspiQueryException {
        try {
            Session session = getSSHClient().startSession();
            session.allocateDefaultPTY();
            final String cmdString = "LC_ALL=C /usr/bin/whereis " + executableBinary;
            final Session.Command cmd = session.exec(cmdString);
            cmd.join(30, TimeUnit.SECONDS);
            final Integer exitStatus = cmd.getExitStatus();
            String output = IOUtils.readFully(cmd.getInputStream())
                    .toString();
            if (exitStatus == 0) {
                final String[] splitted = output.split("\\s");
                if (splitted.length >= 2) {
                    String path = splitted[1].trim();
                    return Optional.of(path);
                } else {
                    return Optional.absent();
                }
            } else {
                return Optional.absent();
            }
        } catch (IOException e) {
            throw RaspiQueryException.createTransportFailure(e);
        }
    }


    /**
     * Tries to read the wireless interface signal strength using 'iwconfig' utility.
     *
     * @param interfaceName the interface name (e.g. 'wlan0')
     * @param iwconfigPath  the path to iwconfig executable (e.g. '/sbin/iwconfig'), if available
     * @return a {@link WlanBean} or null if parsing etc. failed
     */
    private WlanBean queryWirelessInterface(String interfaceName, Optional<String> iwconfigPath) throws RaspiQueryException {
        if (iwconfigPath.isPresent()) {
            return queryWirelessInterfaceWithIwconfig(interfaceName, iwconfigPath.get());
        } else {
            return queryWirelessInterfaceWithProcNetWireless(interfaceName);
        }
    }

    /**
     * Queries the link level and signal quality of the wireless interfaces via
     * "cat /proc/net/wireless".
     *
     * @param interfaceName name of the wireless interface
     * @throws RaspiQueryException if something goes wrong
     */
    private WlanBean queryWirelessInterfaceWithProcNetWireless(String interfaceName)
            throws RaspiQueryException {
        Session session;
        try {
            session = getSSHClient().startSession();
            final String cmdString = "cat /proc/net/wireless";
            final Session.Command cmd = session.exec(cmdString);
            cmd.join(30, TimeUnit.SECONDS);
            String output = IOUtils.readFully(cmd.getInputStream()).toString();
            return this.parseProcNetWireless(output, interfaceName);
        } catch (IOException e) {
            throw RaspiQueryException.createTransportFailure(e);
        }
    }

    private WlanBean parseProcNetWireless(String output, String interfaceName) {
        final String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.startsWith("Inter-") || line.startsWith(" face")) {
                continue;
            }
            final String[] cols = line.split("\\s+");
            if (cols.length >= 11) {
                // getting interface name
                final String name = cols[1].replace(":", "");
                if (interfaceName.equalsIgnoreCase(name)) {
                    final String linkQuality = cols[3].replace(".", "");
                    final String signalLevel = cols[4].replace(".", "");
                    Integer linkQualityInt = null;
                    try {
                        linkQualityInt = Integer.parseInt(linkQuality);
                    } catch (NumberFormatException e) {
                    }
                    Integer signalLevelInt = null;
                    try {
                        signalLevelInt = Integer.parseInt(signalLevel);
                    } catch (Exception e) {
                    }
                    if (signalLevelInt == null || linkQualityInt == null) {
                        return null;
                    }
                    int signalLevelPercentage;
                    int linkQualityPercentage = linkQualityInt;
                    if (signalLevelInt < 0) {
                        // signal is in dBm
                        signalLevelPercentage = computeSignalLevelPercentage(Double.valueOf(signalLevelInt));
                        // link quality presumably x/70
                        if (linkQualityInt <= 70) {
                            linkQualityPercentage = (int) computeLinkQualityPercentage(Double.valueOf(linkQualityInt), LINK_QUALITY_MAXIMUM);
                        }
                    } else {
                        // already percentage
                        signalLevelPercentage = signalLevelInt;
                    }
                    final WlanBean wlanInfo = new WlanBean();
                    wlanInfo.setLinkQuality(linkQualityPercentage);
                    wlanInfo.setSignalLevel(signalLevelPercentage);
                    return wlanInfo;
                }
            }
        }
        return null;
    }

    private WlanBean queryWirelessInterfaceWithIwconfig(String interfaceName, String iwconfigPath) throws RaspiQueryException {
        Session session;
        try {
            session = getSSHClient().startSession();
            session.allocateDefaultPTY();
            final String cmdString = "LC_ALL=C " + iwconfigPath + " " + interfaceName;
            final Session.Command cmd = session.exec(cmdString);
            cmd.join(30, TimeUnit.SECONDS);
            String output = IOUtils.readFully(cmd.getInputStream())
                    .toString();
            return this.parseIwconfigOutput(output);
        } catch (IOException e) {
            throw RaspiQueryException.createTransportFailure(e);
        }
    }

    private WlanBean parseIwconfigOutput(String output) {
        final Matcher linkQualityMatcher = IWCONFIG_LINK_PATTERN.matcher(output);
        if (linkQualityMatcher.find()) {
            String linkValue = linkQualityMatcher.group(1);
            String linkMax = linkQualityMatcher.group(2);
            final Double value = Double.valueOf(linkValue);
            final Double maxValue = Double.valueOf(linkMax);
            double percentageValue = 0.0;
            if (maxValue != 100) {
                percentageValue = computeLinkQualityPercentage(value, LINK_QUALITY_MAXIMUM);
            } else {
                // already percentage
                percentageValue = value;
            }
            WlanBean wlanBean = new WlanBean();
            wlanBean.setLinkQuality((int) percentageValue);
            Matcher signalLevelMatcher = IWCONFIG_LEVEL_DBM_PATTERN.matcher(output);
            if (signalLevelMatcher.find()) {
                Double dbmValue = Double.valueOf(signalLevelMatcher.group(1));
                int signalLevelPercentage = computeSignalLevelPercentage(dbmValue);
                wlanBean.setSignalLevel(signalLevelPercentage);
            } else {
                signalLevelMatcher = IWCONFIG_LEVEL_PERCENTAGE_PATTERN.matcher(output);
                if (signalLevelMatcher.find()) {
                    double signalLevelPercentage = Double.valueOf(signalLevelMatcher.group(1));
                    wlanBean.setSignalLevel((int) signalLevelPercentage);
                } else {
                }
            }
            return wlanBean;
        } else {
            return null;
        }
    }

    private int computeSignalLevelPercentage(Double dbmValue) {
        // using a linear interpolation as described here:
        // https://stackoverflow.com/questions/15797920/how-to-convert-wifi-signal-strength-from-quality-percent-to-rssi-dbm
        double min = 0;
        double max = 100;
        int percentage = (int) Math.min(Math.max(2 * (dbmValue + 100), min), max);
        return percentage;
    }

    private double computeLinkQualityPercentage(Double value, Double linkMaximum) {
        double percentageValue = (100 / linkMaximum) * value;
        return percentageValue;
    }

    /**
     * Queries the ip address of a network interface.
     *
     * @param name the interface name (eth0, wlan0, ...).
     * @return the IP address
     * @throws RaspiQueryException
     */
    private String queryIpAddress(String name) throws RaspiQueryException {
        Session session;
        try {
            session = getSSHClient().startSession();
            session.allocateDefaultPTY();
            final String cmdString = "ip -f inet addr show dev " + name
                    + " | sed -n 2p";
            final Session.Command cmd = session.exec(cmdString);
            cmd.join(30, TimeUnit.SECONDS);
            final String output = IOUtils.readFully(
                    cmd.getInputStream()).toString();
            final Matcher m = IPADDRESS_PATTERN.matcher(output);
            if (m.find()) {
                final String ipAddress = m.group().trim();
                return ipAddress;
            } else {
            }
            session = getSSHClient().startSession();
            session.allocateDefaultPTY();
            final String ifConfigCmd = "/sbin/ifconfig " + name
                    + " | grep \"inet addr\"";
            final Session.Command ifCfgCmd = session.exec(ifConfigCmd);
            ifCfgCmd.join(30, TimeUnit.SECONDS);
            final String ifconfigOutput = IOUtils.readFully(
                    ifCfgCmd.getInputStream()).toString();
            final Matcher m2 = IPADDRESS_PATTERN.matcher(ifconfigOutput);
            if (m2.find()) {
                final String ipAddress = m2.group().trim();
                return ipAddress;
            } else {
                return null;
            }
        } catch (IOException e) {
            throw RaspiQueryException.createTransportFailure(e);
        }
    }

    /**
     * Checks if the specified interface has a carrier up and running via
     * "cat /sys/class/net/[interface]/carrier".
     *
     * @param interfaceName the interface to check
     * @return true, when the interface has a carrier up and running
     * @throws RaspiQueryException if something goes wrong
     */
    private boolean checkCarrier(String interfaceName)
            throws RaspiQueryException {
        Session session;
        try {
            session = getSSHClient().startSession();
            final String cmdString = "cat /sys/class/net/" + interfaceName + "/carrier";
            final Session.Command cmd = session.exec(cmdString);
            cmd.join(30, TimeUnit.SECONDS);
            final String output = IOUtils.readFully(cmd.getInputStream()).toString();
            if (output.contains("1")) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            throw RaspiQueryException.createTransportFailure(e);
        }
    }

    /**
     * Queries which interfaces are available via "/sys/class/net". Loopback
     * interfaces are excluded.
     *
     * @return a List with all interface names (eth0, wlan0,...).
     * @throws RaspiQueryException if something goes wrong
     */
    private List<String> queryInterfaceList() throws RaspiQueryException {
        Session session;
        try {
            session = getSSHClient().startSession();
            final String cmdString = "ls -1 /sys/class/net";
            final Session.Command cmd = session.exec(cmdString);
            cmd.join(30, TimeUnit.SECONDS);
            final String output = IOUtils.readFully(
                    cmd.getInputStream()).toString();
            final String[] lines = output.split("\n");
            final List<String> interfaces = new ArrayList<String>();
            for (String interfaceLine : lines) {
                if (!interfaceLine.startsWith("lo")) {
                    interfaces.add(interfaceLine);
                }
            }
            return interfaces;
        } catch (IOException e) {
            throw RaspiQueryException.createTransportFailure(e);
        }
    }
}

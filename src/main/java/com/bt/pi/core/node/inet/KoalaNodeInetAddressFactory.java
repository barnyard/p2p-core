package com.bt.pi.core.node.inet;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.environment.params.Parameters;

import com.bt.pi.core.conf.Property;
import com.bt.pi.core.exception.KoalaNodeInitializationException;

/**
 * To be used by spring to construct InetAddress for KoalaNode.
 * 
 * @author juan
 * 
 */
@Component("koalaNodeInetAddressFactory")
public class KoalaNodeInetAddressFactory {

    private static Log LOG = LogFactory.getLog(KoalaNodeInetAddressFactory.class);

    private static final String BIND_ADDRESS_PATTERN_REGEX_PARAM = "bind_address_pattern_regex";

    private String[] preferredNetworkInterfaceList;
    private boolean allowLoopbackAddresses;
    private String addressPattern;

    @Resource
    private Parameters parameters;

    public KoalaNodeInetAddressFactory() {
        preferredNetworkInterfaceList = new String[0];
        allowLoopbackAddresses = true;
        addressPattern = null;
        parameters = null;
    }

    public InetAddress lookupInetAddress() {
        LOG.debug(String.format("Looking up inet address..."));

        if (parameters.contains(BIND_ADDRESS_PATTERN_REGEX_PARAM)) {
            this.setAddressPattern(parameters.getString(BIND_ADDRESS_PATTERN_REGEX_PARAM));
        }
        try {
            for (int i = 0; i < preferredNetworkInterfaceList.length; i++) {
                NetworkInterface networkInterface = getNetworkInterface(preferredNetworkInterfaceList[i]);
                if (networkInterface != null) {
                    InetAddress inetAddress = getInetAddress(networkInterface);
                    if (inetAddress != null)
                        return inetAddress;
                }
            }

            for (Enumeration<NetworkInterface> en = getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = en.nextElement();
                InetAddress inetAddress = getInetAddress(networkInterface);
                if (inetAddress != null)
                    return inetAddress;
            }
        } catch (SocketException e) {
            throw new KoalaNodeInitializationException("Failed to get address to bind to for Koala node", e);
        }
        throw new KoalaNodeInitializationException(String.format("Failed to find a suitable address to bind to for Koala node - pattern specified was %s", addressPattern));
    }

    private InetAddress getInetAddress(NetworkInterface networkInterface) {
        for (Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses(); inetAddresses.hasMoreElements();) {
            InetAddress inetAddress = inetAddresses.nextElement();
            LOG.debug(String.format("Checking address %s", inetAddress.toString()));
            if (!allowLoopbackAddresses && inetAddress.isLoopbackAddress()) {
                continue;
            }

            if (this.addressPattern == null || this.addressPattern.length() < 1 || (inetAddress.getHostAddress().matches(this.addressPattern) || inetAddress.getHostName().matches(this.addressPattern))) {
                LOG.debug(String.format("Got address %s, interface %s", inetAddress.toString(), networkInterface.getDisplayName()));
                return inetAddress;
            }
        }
        return null;
    }

    protected NetworkInterface getNetworkInterface(String name) throws SocketException {
        return NetworkInterface.getByName(name);
    }

    protected Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        return NetworkInterface.getNetworkInterfaces();
    }

    @Property(key = "preferred.network.interface.list", defaultValue = "", required = false)
    public void setPreferredNetworkInterfaceList(String value) {
        if (!StringUtils.isBlank(value)) {
            LOG.debug(String.format("Setting preferred network interface list: %s", value));
            preferredNetworkInterfaceList = value.split(",");
        }
    }

    public void setAllowLoopbackAddresses(boolean aAllowLoopbackAddresses) {
        this.allowLoopbackAddresses = aAllowLoopbackAddresses;
    }

    public void setAddressPattern(String anAddressPattern) {
        this.addressPattern = anAddressPattern;
    }

}

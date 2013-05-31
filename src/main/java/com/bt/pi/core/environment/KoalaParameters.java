package com.bt.pi.core.environment;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import rice.environment.params.ParameterChangeListener;
import rice.environment.params.Parameters;

/*
 * load freepastry.params from the classpath (inside freepastry.jar) and, if injected via Spring,
 * overload properties object on top
 */
@Component
public class KoalaParameters implements Parameters {
    private static final String COLON = ":";
    private static final String ARRAY_SPACER = ",";
    private Properties properties;
    private Set<ParameterChangeListener> changeListeners;

    public KoalaParameters() {
        this.changeListeners = new HashSet<ParameterChangeListener>();
        this.properties = new Properties();
        try {
            loadFreePastryParams();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("failed to load freepastry.params");
        }
    }

    private void loadFreePastryParams() throws IOException {
        ClassLoader loader = this.getClass().getClassLoader();
        // some VMs report the bootstrap classloader via null-return
        if (loader == null)
            loader = ClassLoader.getSystemClassLoader();
        InputStream is = loader.getResourceAsStream("freepastry.params");
        this.properties.load(is);
    }

    @Resource
    public void setProperties(Properties p) {
        for (Entry<Object, Object> entry : p.entrySet()) {
            this.properties.setProperty((String) entry.getKey(), (String) entry.getValue());
        }
    }

    // for unit testing
    protected Set<ParameterChangeListener> getChangeListeners() {
        return this.changeListeners;
    }

    @Override
    public void addChangeListener(ParameterChangeListener p) {
        this.changeListeners.add(p);
    }

    @Override
    public boolean contains(String name) {
        return this.properties.containsKey(name);
    }

    @Override
    public boolean getBoolean(String paramName) {
        return Boolean.parseBoolean(this.properties.getProperty(paramName, "false"));
    }

    @Override
    public double getDouble(String paramName) {
        return Double.parseDouble(this.properties.getProperty(paramName));
    }

    @Override
    public float getFloat(String paramName) {
        return Float.parseFloat(this.properties.getProperty(paramName));
    }

    @Override
    public InetAddress getInetAddress(String paramName) throws UnknownHostException {
        return InetAddress.getByName(getString(paramName));
    }

    @Override
    public InetSocketAddress getInetSocketAddress(String paramName) throws UnknownHostException {
        return parseInetSocketAddress(getString(paramName));
    }

    private InetSocketAddress parseInetSocketAddress(String name) throws UnknownHostException {
        String host = name.substring(0, name.indexOf(COLON));
        String port = name.substring(name.indexOf(COLON) + 1);

        try {
            return new InetSocketAddress(InetAddress.getByName(host), Integer.parseInt(port));
        } catch (UnknownHostException uhe) {
            System.err.println("ERROR: Unable to find IP for ISA " + name + " - returning null.");
            return null;
        }
    }

    @Override
    public InetSocketAddress[] getInetSocketAddressArray(String paramName) throws UnknownHostException {
        if (getString(paramName).length() == 0)
            return new InetSocketAddress[0];

        String[] addresses = StringUtils.trimAllWhitespace(getString(paramName)).split(ARRAY_SPACER);
        List<InetSocketAddress> result = new LinkedList<InetSocketAddress>();

        for (int i = 0; i < addresses.length; i++) {
            InetSocketAddress address = parseInetSocketAddress(addresses[i]);

            if (address != null)
                result.add(address);
        }

        return (InetSocketAddress[]) result.toArray(new InetSocketAddress[result.size()]);
    }

    @Override
    public int getInt(String paramName) {
        return Integer.parseInt(getString(paramName));
    }

    @Override
    public long getLong(String paramName) {
        return Long.parseLong(getString(paramName));
    }

    @Override
    public String getString(String paramName) {
        String result = this.properties.getProperty(paramName);
        if (null == result)
            return null;
        return result.trim();
    }

    @Override
    public String[] getStringArray(String paramName) {
        String list = getString(paramName);

        if (list == null)
            return new String[0];
        return list.equals("") ? new String[0] : list.split(ARRAY_SPACER);
    }

    @Override
    public void remove(String name) {
        this.properties.remove(name);
    }

    @Override
    public void removeChangeListener(ParameterChangeListener p) {
        this.changeListeners.remove(p);
    }

    @Override
    public void setBoolean(String paramName, boolean val) {
        throw new NotImplementedException();
    }

    @Override
    public void setDouble(String paramName, double val) {
        throw new NotImplementedException();
    }

    @Override
    public void setFloat(String paramName, float val) {
        throw new NotImplementedException();
    }

    @Override
    public void setInetAddress(String paramName, InetAddress val) {
        throw new NotImplementedException();
    }

    @Override
    public void setInetSocketAddress(String paramName, InetSocketAddress val) {
        throw new NotImplementedException();
    }

    @Override
    public void setInetSocketAddressArray(String paramName, InetSocketAddress[] val) {
        throw new NotImplementedException();
    }

    @Override
    public void setInt(String paramName, int val) {
        throw new NotImplementedException();
    }

    @Override
    public void setLong(String paramName, long val) {
        throw new NotImplementedException();
    }

    @Override
    public void setString(String paramName, String val) {
        this.properties.setProperty(paramName, val);
    }

    @Override
    public void setStringArray(String paramName, String[] value) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < value.length; i++) {
            buffer.append(value[i]);
            if (i < value.length - 1)
                buffer.append(ARRAY_SPACER);
        }

        setString(paramName, buffer.toString());
    }

    @Override
    public void store() throws IOException {
        // throw new NotImplementedException();
    }

    @Override
    public void restoreDefault(String arg0) {
        // throw new NotImplementedException();
    }
}

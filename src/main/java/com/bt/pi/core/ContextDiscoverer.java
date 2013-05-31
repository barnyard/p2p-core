package com.bt.pi.core;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ContextDiscoverer {
    private static final Log LOG = LogFactory.getLog(ContextDiscoverer.class);
    private static final String COMMA = ",";
    private static final String NAME_PREFIX = "applicationContext-p2p-";
    private static final String NAME_SUFFIX = ".xml";
    private static final String INTEGRATION = "integration";

    public ContextDiscoverer() {
    }

    public String findPiContexts() {
        LOG.debug("findPiContexts()");
        String classpath = getClasspath();
        LOG.debug(String.format("classpath : %s", classpath));

        Set<String> results = new HashSet<String>();
        for (String pathElement : classpath.split(System.getProperty("path.separator"))) {
            LOG.debug(String.format("searching %s", pathElement));
            File f = new File(pathElement);
            if (!f.exists())
                continue;
            if (f.isDirectory()) {
                processDir(f, results);
            }
            if (f.isFile() && f.getName().endsWith(".jar")) {
                processFile(f, results);
            }
        }

        StringBuffer buffer = new StringBuffer();
        String sep = "";
        for (String result : results) {
            buffer.append(sep).append(result);
            sep = COMMA;
        }
        return buffer.toString();
    }

    private void processFile(File f, Set<String> results) {
        LOG.debug(String.format("looking in %s", f.getAbsolutePath()));
        try {
            JarFile jarFile = new JarFile(f);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.isDirectory())
                    continue;
                if (matches(jarEntry.getName())) {
                    results.add(jarEntry.getName());
                }
            }
        } catch (IOException e) {
            LOG.error(String.format("Error processing jar file %s", f.getAbsolutePath()), e);
        }
    }

    private void processDir(File f, Set<String> results) {
        File[] files = f.listFiles();
        for (File file : files) {
            if (matches(file.getName())) {
                results.add(file.getName());
            }
        }
    }

    private boolean matches(String name) {
        if (name.contains(INTEGRATION))
            return false;
        return name.startsWith(NAME_PREFIX) && name.endsWith(NAME_SUFFIX);
    }

    protected String getClasspath() {
        return System.getProperty("java.class.path");
    }
}

package com.bt.pi.core.logging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import rice.environment.logging.LogManager;
import rice.environment.logging.Logger;

import com.bt.pi.core.conf.Property;

@Component
public class Log4JLogManager implements LogManager {
    private static final String UNCHECKED = "unchecked";
    private String logLevel;
    private ConcurrentMap<MapKey, PiLog4JLogger> loggerMap;

    public Log4JLogManager() {
        logLevel = null;
        this.loggerMap = new ConcurrentHashMap<MapKey, PiLog4JLogger>();
    }

    @SuppressWarnings(UNCHECKED)
    @Override
    public Logger getLogger(Class clazz, String instance) {
        MapKey key = new MapKey(clazz, instance);
        if (!this.loggerMap.containsKey(key))
            this.loggerMap.put(key, new PiLog4JLogger(clazz, instance, logLevel));
        return this.loggerMap.get(key);
    }

    @Property(key = "pastry.log.level", defaultValue = "debug")
    public void setPastryLogLevel(String level) {
        logLevel = level;
    }

    private static class MapKey {
        private String className;
        private String instance;

        @SuppressWarnings(UNCHECKED)
        public MapKey(Class aClazz, String anInstance) {
            this.className = aClazz.getName();
            this.instance = anInstance;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((instance == null) ? 0 : instance.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MapKey other = (MapKey) obj;
            if (className == null) {
                if (other.className != null)
                    return false;
            } else if (!className.equals(other.className))
                return false;
            if (instance == null) {
                if (other.instance != null)
                    return false;
            } else if (!instance.equals(other.instance))
                return false;
            return true;
        }
    }
}

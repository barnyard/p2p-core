/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.core.util.common;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.springframework.stereotype.Component;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Parses command lines similarly to how a shell would.
 */
@Component
public class CommandLineParser {

    public CommandLineParser() {
    }

    /**
     * Splits commands on spaces, but arguments can be single quoted if they contain spaces.
     * 
     * Escaping of single quotes is done by doubling, except immediately following a space, where 4 single quotes must
     * be used instead.
     * 
     */
    public String[] parse(String commandLine) {
        try {
            CsvPreference preferences = new CsvPreference('\'', ' ', "\r\n");
            List<String> commands = new CsvListReader(new StringReader(commandLine), preferences).read();
            return commands.toArray(new String[commands.size()]);
        } catch (IOException e) {
            throw new RuntimeException("Could not parse commandLine: '" + commandLine + "'", e);
        }
    }

}

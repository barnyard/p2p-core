//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.cli;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PreDestroy;

import com.bt.pi.core.cli.commands.Command;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.cli.commands.CommandParseException;
import com.bt.pi.core.cli.commands.CommandParser;
import com.bt.pi.core.cli.commands.ExitCommand;
import com.bt.pi.core.cli.commands.KoalaNodeCommand;
import com.bt.pi.core.node.KoalaNode;

public class KoalaCli implements Runnable {
    private BufferedReader bufferedReader;
    private boolean stopping;
    private CountDownLatch stoppedLatch;
    private CommandParser commandParser;
    private KoalaNode koalaNode;

    public KoalaCli(KoalaNode aKoalaNode) {
        bufferedReader = null;
        commandParser = null;
        stopping = false;
        stoppedLatch = new CountDownLatch(1);
        koalaNode = aKoalaNode;
    }

    public void setCommandParser(CommandParser aCommandParser) {
        this.commandParser = aCommandParser;
    }

    public void start() {
        printStartMessage();
        new Thread(this).start();

        try {
            stoppedLatch.await();
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    @PreDestroy
    public synchronized void stop() {
        this.stopping = true;
    }

    public void run() {
        bufferedReader = new BufferedReader(new InputStreamReader(getInputStream()));
        boolean stopNow;
        synchronized (this) {
            stopNow = this.stopping;
        }

        try {
            String line;
            do {
                System.out.print("koala> ");
                line = bufferedReader.readLine();
                if (line == null)
                    break;

                Command command;
                try {
                    command = commandParser.parse(line);
                    System.out.println();

                    if (command instanceof KoalaNodeCommand)
                        ((KoalaNodeCommand) command).setKoalaNode(koalaNode);
                    command.execute(System.out);

                    if (command instanceof ExitCommand) {
                        if (koalaNode != null) {
                            koalaNode.stop();
                        }
                        break;
                    }
                } catch (CommandParseException e) {
                    System.err.println(e.getMessage());
                } catch (CommandExecutionException e) {
                    System.err.println(e.getMessage());
                }
            } while (!stopNow);

            bufferedReader.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        stoppedLatch.countDown();
    }

    protected InputStream getInputStream() {
        return System.in;
    }

    private void printStartMessage() {
        System.out.println("          ___   ");
        System.out.println("        {~._.~} ");
        System.out.println("         ( Y )  ");
        System.out.println("        ()~*~() ");
        System.out.println("        (_)-(_) ");
        System.out.println("                ");
        System.out.println("Koala CLI interface - g'day mate!");
        if (koalaNode != null) {
            System.out.println("  Bound to " + koalaNode.getInetAddress() + ", port " + koalaNode.getPort());
            System.out.println("  " + (koalaNode.getStartedNewRing() ? "Started a new ring" : "Joined existing ring"));
        }
        System.out.println();
    }
}

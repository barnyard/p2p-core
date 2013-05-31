package com.bt.pi.core;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.node.KoalaNode;

@Component
public class CloudPlatform {
    private static final String OPTION_B = "b";
    private static final String OPTION_P = "p";
    private static final String OPTION_A = "a";
    private static final String OPTION_S = "s";

    private static Log logger = LogFactory.getLog(CloudPlatform.class);
    private KoalaNode koalaNode;

    public CloudPlatform() {
        koalaNode = null;
    }

    @Resource
    public void setKoalaNode(KoalaNode aKoalaNode) {
        koalaNode = aKoalaNode;
    }

    public KoalaNode getKoalaNode() {
        return koalaNode;
    }

    public void start(CommandLine line) {
        if (line.hasOption(OPTION_A)) {
            koalaNode.setAddressPattern(line.getOptionValue(OPTION_A));
        }
        if (line.hasOption(OPTION_P)) {
            koalaNode.setPort(Integer.parseInt(line.getOptionValue(OPTION_P)));
        }
        if (line.hasOption(OPTION_B)) {
            koalaNode.setPreferredBootstraps(line.getOptionValue(OPTION_B));
        }
        if (line.hasOption(OPTION_S)) {
            String addr = line.getOptionValue(OPTION_A) != null ? line.getOptionValue(OPTION_A).replace("^", "") : "";
            koalaNode.setNodeIdFile("nodeId-" + addr + "-" + line.getOptionValue(OPTION_P) + ".txt");
        }

        try {
            koalaNode.start();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new RuntimeException(t);
        }
    }

    @PreDestroy
    public void stop() {
        if (null != koalaNode)
            koalaNode.stop();
        else
            logger.debug("KoalaNode is null. Probably not started yet.");
    }
}

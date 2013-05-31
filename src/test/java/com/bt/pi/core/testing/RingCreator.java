package com.bt.pi.core.testing;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import rice.p2p.commonapi.NodeHandle;

import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.environment.KoalaEnvironment;
import com.bt.pi.core.environment.KoalaParameters;
import com.bt.pi.core.logging.Log4JLogManager;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.node.inet.KoalaNodeInetAddressFactory;
import com.bt.pi.core.pastry_override.PiSelectorManager;

public class RingCreator {
    private static Log LOG = LogFactory.getLog(RingCreator.class);
    public static final String NODE_ID_FILE = "nodeIdFile";
    public static final String DEFAULT_CONTEXT_FILE = "/applicationContext-p2p-core-integration.xml";

    private static SortedMap<String, AbstractApplicationContext> applicationContexts;
    private static Map<Integer, KoalaNode> nodes;
    public static final int STARTING_PORT = 10321;
    private Random random;
    private static KoalaParameters params;
    private static String bootstrapList;

    private static int region;
    private static int zone;
    private static Properties properties;
    private static String contextFile = DEFAULT_CONTEXT_FILE;

    public RingCreator(int numNodes, int region, int zone, Properties p) {
        this(numNodes, region, zone, p, null);
    }

    public RingCreator(int numNodes, int aRegion, int aZone, Properties p, String aContextFile) {
        applicationContexts = new TreeMap<String, AbstractApplicationContext>();
        region = aRegion;
        zone = aZone;
        random = new Random();
        nodes = new HashMap<Integer, KoalaNode>();
        bootstrapList = "";
        properties = p;
        if (aContextFile != null)
            contextFile = aContextFile;
        createNodes(numNodes);
    }

    private static void initializeProperties(AbstractApplicationContext applicationContext) {
        if (properties == null)
            properties = (Properties) applicationContext.getBean("properties");
        params = (KoalaParameters) applicationContext.getBean("koalaParameters");
        addProperties(properties);
        params.setProperties(properties);
    }

    public static AbstractApplicationContext createApplicationContextAndStoreInMap(int port) {
        AbstractApplicationContext applicationContext = new GenericApplicationContext();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader((BeanDefinitionRegistry) applicationContext);
        reader.loadBeanDefinitions(new ClassPathResource(contextFile));
        applicationContext.refresh();
        String key = Integer.toString(port);
        System.err.println(String.format("Storing application context in map with key %s", key));
        applicationContexts.put(key, applicationContext);
        return applicationContext;
    }

    public void setAvailabilityZone(int aZone) {
        zone = aZone;
    }

    public void setRegion(int aRegion) {
        region = aRegion;
    }

    private static void addProperties(Properties p) {
        p.setProperty("pastry_socket_allow_loopback", "true");
        p.setProperty("pastry_factory_selectorPerNode", "true");
        p.setProperty("pastry_factory_processorPerNode", "true");
        p.setProperty("pastry_leafSetMaintFreq", "4");
        p.setProperty("pastry_protocol_router_routeMsgVersion", "0");
        p.setProperty("pastry_lSetSize", "10");
    }

    public void createNodes(int numNodes) {
        for (int i = 0; i < numNodes; i++) {
            createANode(getRingSize());
            startNextNode(true);
        }
    }

    public static int createANode(int ringSize) {
        // int ringSize = getRingSize();
        int port = STARTING_PORT + ringSize;
        int consolePort = 6789 + ringSize;
        BeanPropertiesMunger.setConsolePort(consolePort);

        AbstractApplicationContext applicationContext = createApplicationContextAndStoreInMap(port);

        try {
            initializeInetAddress(applicationContext);
            initializeProperties(applicationContext);

            KoalaNode node = applicationContext.getBean(KoalaNode.class);
            initializePiSelectorManager(node);
            System.err.println("Setting node port: " + port);
            node.setPort(port);
            System.err.println("Setting koalaIdFactory with region: " + region + " and availability zone: " + zone);
            node.setMessageQueueCheckPeriodInMinutes(1);
            node.setNodeIdFile(NODE_ID_FILE + ringSize);

            node.setPreferredBootstraps(bootstrapList);
            node.setCanStartNewRing(ringSize == 0);

            nodes.put(ringSize, node);
            System.err.println("Created number " + ringSize + " node in port: " + port);
            return ringSize;
        } catch (Exception e) {
            LOG.error(e.getMessage() + " : " + e, e);
            return -1;
        }
    }

    private static void initializeInetAddress(AbstractApplicationContext applicationContext) throws UnknownHostException {
        KoalaNodeInetAddressFactory factory = applicationContext.getBean(KoalaNodeInetAddressFactory.class);
        factory.setAllowLoopbackAddresses(true);
        factory.setAddressPattern("^" + InetAddress.getLocalHost().getHostAddress());
        if (StringUtils.isEmpty(bootstrapList)) {
            // this is the same method that KoalaNode calls to get InetAddress.
            InetAddress inetAddress = factory.lookupInetAddress();

            bootstrapList = inetAddress.getHostAddress() + ":" + STARTING_PORT;
            LOG.debug("Initializing Bootstraplist to: " + bootstrapList);
        }
    }

    private static void initializePiSelectorManager(KoalaNode node) {
        KoalaEnvironment environment = (KoalaEnvironment) ReflectionTestUtils.getField(node, "environment");
        PiSelectorManager piSelectorManager = (PiSelectorManager) ReflectionTestUtils.getField(environment, "selectorManager");
        piSelectorManager.setMaxExecuteDueTaskTimeMillis(800);
        piSelectorManager.setMaxInvocationTimeMillis(800);
        piSelectorManager.setSelectorThreadLoopSleepMillis(100);
        piSelectorManager.setParameters(params);
        Log4JLogManager logManager = new Log4JLogManager();
        // logManager.setPastryLogLevel("all");
        environment.setLogManager(logManager);
        piSelectorManager.setLogManager(logManager);
    }

    private void startNextNode(boolean startApps) {
        new NodeStarter(nodes.get(nodes.size() - 1), startApps).run();
        // wait a bit for the first node as the rest bootstrap.
        waitForNode(nodes.size() - 1);
    }

    public void waitForNode(int nodeNumber) {
        waitForNode(nodes.get(nodeNumber));
    }

    public void waitForNode(KoalaNode node) {
        int count = 0;
        while (node == null || node.getPastryNode() == null || (!node.getPastryNode().isReady() && count < 500)) {
            count++;
            sleep(150);
        }
        LOG.debug("node " + node.getPastryNode().getId().toStringFull() + " is ready after count of " + count);
    }

    private void sleep(long wait) {
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage() + " : " + e);
        }
    }

    public void startNode(int pos, boolean startApps) {
        new NodeStarter(nodes.get(pos), startApps).run();
    }

    public KoalaNode findNodeForHandle(NodeHandle nh) {
        KoalaNode foundNode = null;
        for (int i = 0; i < getRingSize(); i++) {
            if (getNode(i).getPastryNode().getLocalNodeHandle().equals(nh)) {
                foundNode = getNode(i);
            }
        }
        return foundNode;
    }

    public KoalaNode getNode(int pos) {
        return nodes.get(pos);
    }

    public Map<Integer, KoalaNode> getNodes() {
        return nodes;
    }

    public ApplicationContext getApplicationContextForNode(int pos) {
        KoalaNode node = getNode(pos);
        return applicationContexts.get(Integer.toString(node.getPort()));
    }

    public KoalaNode getBootStrapNode() {
        return getNode(0);
    }

    public KoalaNode getRandomNode() {
        return getNode(getRandomNodeId());
    }

    public int getRandomNodeId() {
        return random.nextInt(nodes.size());
    }

    public int getRingSize() {
        return nodes.size();
    }

    public void destroy() {
        int count = 1;
        for (AbstractApplicationContext applicationContext : applicationContexts.values()) {
            LOG.info("Destroying application context" + count++);
            applicationContext.close();
            applicationContext.destroy();
            applicationContext = null;
        }
        deleteStorage();
        applicationContexts.clear();
        nodes.clear();
    }

    private void deleteStorage() {
        for (KoalaNode node : nodes.values()) {
            String rootDir = node.getPersistentDhtStorage().getRoot();
            try {
                FileUtils.deleteDirectory(new File(rootDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setContextFile(String aContextFile) {
        contextFile = aContextFile;
    }

    public String getContextFile() {
        return contextFile;
    }

    public void setBootstrapList(String aBootstrapList) {
        bootstrapList = aBootstrapList;
    }

    public DhtClientFactory getDhtClientFactoryForNode(int nodeIndex) {
        return applicationContexts.get(Integer.toString(nodes.get(nodeIndex).getPort())).getBean(DhtClientFactory.class);
    }
}
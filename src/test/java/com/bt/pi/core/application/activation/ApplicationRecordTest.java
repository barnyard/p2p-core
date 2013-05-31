package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.conf.IllegalAnnotationException;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.scope.NodeScope;

public class ApplicationRecordTest {
    @EntityScope(scope = NodeScope.GLOBAL)
    private final class MyGlobalScopedApplicationRecord extends GlobalScopedApplicationRecord {
        private MyGlobalScopedApplicationRecord(String applicationName, long dataVersion, int requiredActive) {
            super(applicationName, dataVersion, requiredActive);
        }

        @Override
        protected TimeStampedPair<String> newTimeStampedPair(Id nodeId) {
            return new TimeStampedPair<String>(nodeId.toStringFull()) {
                ;
                @Override
                protected long generateCurrentTimestamp() {
                    return timestamp;
                }
            };
        }
    }

    private static final String FOO = "foo";
    private ApplicationRecord applicationRecord;
    private long timestamp;
    private SortedMap<String, TimeStampedPair<String>> expectedCurrentlyActiveNodeMap;
    private int expectedRequiredActive = 6;
    private long expectedVersion = 1999L;
    private Id id;
    private KoalaJsonParser koalaJsonParser;
    private KoalaIdFactory koalaIdFactory;
    private String ip1 = "1.1.1.1";
    private String ip2 = "2.2.2.2";
    private String ip3 = "3.3.3.3";
    private Id anotherId;
    private Id yetAnotherId;

    @Before
    public void before() {
        timestamp = System.currentTimeMillis();
        koalaIdFactory = new KoalaIdFactory();
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());
        id = koalaIdFactory.buildId("test");
        anotherId = koalaIdFactory.buildId("test1");
        yetAnotherId = koalaIdFactory.buildId("test2");

        koalaJsonParser = new KoalaJsonParser();
        expectedCurrentlyActiveNodeMap = new TreeMap<String, TimeStampedPair<String>>();

        applicationRecord = new MyGlobalScopedApplicationRecord("testApp", 2, expectedRequiredActive);
        applicationRecord.setVersion(expectedVersion);
    }

    @Test
    public void testGettersAndSetters() {
        // setup
        applicationRecord.addCurrentlyActiveNode(id, -1);
        applicationRecord.setApplicationName(FOO);

        expectedCurrentlyActiveNodeMap.put("test", new TimeStampedPair<String>(id.toStringFull()));
        SortedMap<String, String> activeNodeMap = new TreeMap<String, String>();
        activeNodeMap.put("test", id.toStringFull());
        applicationRecord.setCurrentlyActiveNodeMap(activeNodeMap);

        // act & assert
        assertEquals(FOO, applicationRecord.getApplicationName());
        assertEquals(id.toStringFull(), applicationRecord.getActiveNodeMap().get("test").getObject());
        assertEquals(expectedCurrentlyActiveNodeMap.size(), applicationRecord.getRequiredActive());
        assertEquals(1, applicationRecord.getNumCurrentlyActiveNodes());
        assertEquals(expectedVersion, applicationRecord.getVersion());
    }

    @Test
    public void testEquals() {
        // setup
        ApplicationRecord app1 = new GlobalScopedApplicationRecord(applicationRecord.getApplicationName(), applicationRecord.getVersion(), applicationRecord.getRequiredActive());
        app1.addCurrentlyActiveNode(id, -1);
        app1.setVersion(expectedVersion);

        // act
        applicationRecord.addCurrentlyActiveNode(id, -1);

        // assert
        assertEquals(applicationRecord, app1);
        assertEquals(applicationRecord.hashCode(), app1.hashCode());
    }

    @Test
    public void testToString() {
        // setup
        ApplicationRecord app = new GlobalScopedApplicationRecord("foo", 1234L);
        app.addCurrentlyActiveNode(id, -1);

        // act
        String res = app.toString();

        // assert
        System.out.println(res);
    }

    @Test
    public void shouldBeAbleToAddActiveNode() {
        // act
        boolean res = applicationRecord.addCurrentlyActiveNode(id, -1);

        // assert
        assertTrue(res);
        assertEquals(expectedRequiredActive, applicationRecord.getActiveNodeMap().size());
        assertTrue(applicationRecord.containsNodeId(id));
        assertEquals(1, applicationRecord.getNumCurrentlyActiveNodes());
        assertNotNull(applicationRecord.getAssociatedResource(id));
        assertEquals(timestamp, applicationRecord.getActiveNodeMap().get(applicationRecord.getAssociatedResource(id)).getTimeStamp());
    }

    @Test
    public void shouldBeAbleToAbsorbAdditionOfDuplicateActiveNode() {
        // setup
        applicationRecord.addCurrentlyActiveNode(id, -1);
        timestamp = 123L;

        // act
        boolean res = applicationRecord.addCurrentlyActiveNode(id, -1);

        // assert
        assertTrue(res);
        assertEquals(expectedRequiredActive, applicationRecord.getActiveNodeMap().size());
        assertTrue(applicationRecord.containsNodeId(id));
        assertEquals(timestamp, applicationRecord.getActiveNodeMap().get(applicationRecord.getAssociatedResource(id)).getTimeStamp());
    }

    @Test
    public void shouldBeAbleToTimestampActiveNode() {
        // setup
        applicationRecord.addCurrentlyActiveNode(id, -1);
        timestamp = 123L;

        // act
        boolean res = applicationRecord.timestampActiveNode(id);

        // assert
        assertTrue(res);
        assertEquals(timestamp, applicationRecord.getActiveNodeMap().get(applicationRecord.getAssociatedResource(id)).getTimeStamp());
    }

    @Test
    public void timestampActiveNodeShouldReturnFalseIfNotFound() {
        // act
        boolean result = applicationRecord.timestampActiveNode(id);

        // assert
        assertFalse(result);
    }

    @Test
    public void shouldDoNothingIfTimestampingNonActiveNodeAsActive() {
        // act
        boolean res = applicationRecord.timestampActiveNode(id);

        // assert
        assertFalse(res);
    }

    @Test
    public void shouldBeAbleToAbsorbAdditionOfDuplicateActiveNodeWhenOnlyOneSlot() {
        // setup
        ApplicationRecord appRecord = new GlobalScopedApplicationRecord("OnlyOneTaken");
        appRecord.addCurrentlyActiveNode(id, -1);

        // act
        boolean res = appRecord.addCurrentlyActiveNode(id, -1);

        // assert
        assertTrue(res);
        assertEquals(1, appRecord.getActiveNodeMap().size());
        assertEquals(1, appRecord.getNumCurrentlyActiveNodes());
        assertTrue(appRecord.containsNodeId(id));
    }

    @Test
    public void shouldNotAddIfThereAreEnoughCurrentlyActiveNodes() {
        // setup
        Id existingId = rice.pastry.Id.build("alreadyExisting");
        ApplicationRecord appWithOneRequiredSlot = new GlobalScopedApplicationRecord("one-required", 1, 1);
        appWithOneRequiredSlot.addCurrentlyActiveNode(existingId, -1);

        // act
        boolean res = appWithOneRequiredSlot.addCurrentlyActiveNode(id, -1);

        // assert
        assertFalse(res);
        assertEquals(1, appWithOneRequiredSlot.getActiveNodeMap().size());
        assertEquals(1, appWithOneRequiredSlot.getNumCurrentlyActiveNodes());
        assertFalse(appWithOneRequiredSlot.containsNodeId(id));
        assertTrue(appWithOneRequiredSlot.containsNodeId(existingId));
    }

    @Test
    public void shouldRemoveExistentNode() {
        // setup
        applicationRecord.addCurrentlyActiveNode(id, -1);
        System.err.println(koalaJsonParser.getJson(applicationRecord));

        // act
        boolean res = applicationRecord.removeActiveNode(id);

        // assert
        System.err.println(koalaJsonParser.getJson(applicationRecord));
        assertTrue(res);
        assertFalse(applicationRecord.containsNodeId(id));
    }

    @Test
    public void shouldRemoveExistentNode2() {
        // setup
        applicationRecord.addCurrentlyActiveNode(id, -1);
        Id id1 = koalaIdFactory.buildId("test1");
        applicationRecord.addCurrentlyActiveNode(id1, -1);

        // act
        boolean res = applicationRecord.removeActiveNode(id);

        // assert
        assertTrue(res);
        assertFalse(applicationRecord.containsNodeId(id));
        assertTrue(applicationRecord.containsNodeId(id1));
    }

    @Test
    public void shouldNoOpWhenRemovingNonExistentNode() {
        // setup
        applicationRecord.addCurrentlyActiveNode(id, -1);

        // act
        boolean res = applicationRecord.removeActiveNode(rice.pastry.Id.build("moo"));

        // assert
        assertFalse(res);
        assertTrue(applicationRecord.containsNodeId(id));
        assertEquals(1, applicationRecord.getNumCurrentlyActiveNodes());
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJson() throws Exception {
        // setup
        applicationRecord.addCurrentlyActiveNode(id, -1);

        // act
        String json = koalaJsonParser.getJson(applicationRecord);
        ApplicationRecord reverse = (GlobalScopedApplicationRecord) koalaJsonParser.getObject(json, GlobalScopedApplicationRecord.class);

        // assert
        assertEquals(applicationRecord.getApplicationName(), reverse.getApplicationName());
        assertEquals(applicationRecord.getActiveNodeMap().size(), reverse.getActiveNodeMap().size());
        assertEquals(applicationRecord.getActiveNodeMap(), reverse.getActiveNodeMap());
        assertEquals(applicationRecord, reverse);
        assertTrue(reverse.getActiveNodeMap().get("1").toString().contains(InetAddress.getLocalHost().getHostName()));
    }

    @Test
    public void shouldReturnOnlyNodeAsClosestNode() {
        // setup
        Id refId = rice.pastry.Id.build("1");
        Id activeId = rice.pastry.Id.build("22");

        applicationRecord.addCurrentlyActiveNode(activeId, -1);

        // act
        Id res = applicationRecord.getClosestActiveNodeId(refId);

        // assert
        assertEquals(activeId, res);
    }

    @Test
    public void shouldReturnCloserOfTwoNodesWhenBothAbove() {
        // setup
        Id refId = rice.pastry.Id.build("1");
        Id activeId = rice.pastry.Id.build("12");
        applicationRecord.addCurrentlyActiveNode(activeId, -1);
        applicationRecord.addCurrentlyActiveNode(rice.pastry.Id.build("FF"), -1);

        // act
        Id res = applicationRecord.getClosestActiveNodeId(refId);

        // assert
        assertEquals(activeId, res);
    }

    @Test
    public void shouldReturnCloserOfTwoNodesWhenBothBelow() {
        // setup
        Id refId = rice.pastry.Id.build("10");
        Id activeId = rice.pastry.Id.build("05");
        applicationRecord.addCurrentlyActiveNode(activeId, -1);
        applicationRecord.addCurrentlyActiveNode(rice.pastry.Id.build("03"), -1);

        // act
        Id res = applicationRecord.getClosestActiveNodeId(refId);

        // assert
        assertEquals(activeId, res);
    }

    @Test
    public void shouldReturnCloserOfTwoNodesWhenOneAboveOneBelow() {
        // setup
        Id refId = rice.pastry.Id.build("10");
        Id activeId = rice.pastry.Id.build("05");
        applicationRecord.addCurrentlyActiveNode(activeId, -1);
        applicationRecord.addCurrentlyActiveNode(rice.pastry.Id.build("03"), -1);

        // act
        Id res = applicationRecord.getClosestActiveNodeId(refId);

        // assert
        assertEquals(activeId, res);
    }

    @Test
    public void shouldReturnCloserOfTwoNodesWrapAroundTop() {
        // setup
        Id refId = rice.pastry.Id.build("F0");
        Id activeId = rice.pastry.Id.build("03");
        applicationRecord.addCurrentlyActiveNode(activeId, -1);
        applicationRecord.addCurrentlyActiveNode(rice.pastry.Id.build("A0"), -1);

        // act
        Id res = applicationRecord.getClosestActiveNodeId(refId);

        // assert
        assertEquals(activeId, res);
    }

    @Test
    public void shouldReturnCloserOfTwoNodesWrapAroundBottom() {
        // setup
        Id refId = rice.pastry.Id.build("03");
        Id activeId = rice.pastry.Id.build("FF");
        applicationRecord.addCurrentlyActiveNode(activeId, -1);
        applicationRecord.addCurrentlyActiveNode(rice.pastry.Id.build("A0"), -1);

        // act
        Id res = applicationRecord.getClosestActiveNodeId(refId);

        // assert
        assertEquals(activeId, res);
    }

    @Test
    public void shouldReturnClosestOfFourNodes() {
        // setup
        Id refId = rice.pastry.Id.build("80");
        Id activeId = rice.pastry.Id.build("45");
        applicationRecord.addCurrentlyActiveNode(rice.pastry.Id.build("03"), -1);
        applicationRecord.addCurrentlyActiveNode(rice.pastry.Id.build("05"), -1);
        applicationRecord.addCurrentlyActiveNode(rice.pastry.Id.build("45"), -1);
        applicationRecord.addCurrentlyActiveNode(rice.pastry.Id.build("F5"), -1);

        // act
        Id res = applicationRecord.getClosestActiveNodeId(refId);

        // assert
        assertEquals(activeId, res);
        assertEquals(4, applicationRecord.getNumCurrentlyActiveNodes());
    }

    @Test
    public void shouldReturnNullIfNoNodes() {
        // setup
        ApplicationRecord empptyAppRecord = new GlobalScopedApplicationRecord("blah");
        Id refId = rice.pastry.Id.build("1");

        // act
        Id res = empptyAppRecord.getClosestActiveNodeId(refId);

        // assert
        assertEquals(null, res);
    }

    @Test
    public void shouldBeAbleToRemoveAResource() {
        // setup
        List<String> resourcesToRemove = new ArrayList<String>();
        resourcesToRemove.add("1");
        resourcesToRemove.add("3");
        int expected = expectedRequiredActive - resourcesToRemove.size();

        // act
        applicationRecord.removeResources(resourcesToRemove);

        // assert
        assertEquals(expected, applicationRecord.getRequiredActive());
    }

    // for migrating from data version where active node set just has a map of ip address to nodeId, to
    // ip address to Timestamp pair
    @Test
    public void testDeserializingFromOldAndNewApplicationRecord() throws Exception {
        // setup
        String json = "{\"type\" : \"ApplicationRecord\", \"version\" : 108, \"url\" : \"app:pi-network-manager\", \"applicationName\" : \"pi-network-manager\", \"requiredActive\" : 1, \"currentlyActiveNodeMap\" : { \"10.19.1.200/24\" : \"010138EBAAE248B0E69DD51283E34921F6B50000\" } }";

        // act
        ApplicationRecord result = (ApplicationRecord) koalaJsonParser.getObject(json, RegionScopedApplicationRecord.class);

        // assert
        assertEquals("RegionScopedApplicationRecord", result.getType());
        assertEquals(108, result.getVersion());
        assertEquals("regionapp:pi-network-manager", result.getUrl());
        assertEquals("pi-network-manager", result.getApplicationName());
        assertEquals(1, result.getRequiredActive());
        assertEquals(1, result.getActiveNodeMap().size());
        assertTrue(result.getActiveNodeMap().containsKey("10.19.1.200/24"));
        assertEquals("010138EBAAE248B0E69DD51283E34921F6B50000", result.getActiveNodeMap().get("10.19.1.200/24").getObject());

        // setup again
        String json2 = koalaJsonParser.getJson(result);

        // act again
        ApplicationRecord result2 = (ApplicationRecord) koalaJsonParser.getObject(json2, RegionScopedApplicationRecord.class);

        // assert again
        assertEquals("RegionScopedApplicationRecord", result2.getType());
        assertEquals(108, result2.getVersion());
        assertEquals("regionapp:pi-network-manager", result2.getUrl());
        assertEquals("pi-network-manager", result2.getApplicationName());
        assertEquals(1, result2.getRequiredActive());
        assertEquals(1, result2.getActiveNodeMap().size());
        assertTrue(result2.getActiveNodeMap().containsKey("10.19.1.200/24"));
        assertEquals("010138EBAAE248B0E69DD51283E34921F6B50000", result2.getActiveNodeMap().get("10.19.1.200/24").getObject());
    }

    // additonal test to check for where the nodeid of the pair in null
    @Test
    public void testDeserializingFromOldAndNewApplicationRecordWithANullNodeId() throws Exception {
        // setup
        String json = "{\"type\" : \"ApplicationRecord\", \"version\" : 108, \"url\" : \"app:pi-network-manager\", \"applicationName\" : \"pi-network-manager\", \"requiredActive\" : 2,"
                + " \"currentlyActiveNodeMap\" : { \"10.19.1.200/24\" : \"010138EBAAE248B0E69DD51283E34921F6B50000\",\"10.19.1.333/24\" : null }" + " }";

        // act
        ApplicationRecord result = (ApplicationRecord) koalaJsonParser.getObject(json, RegionScopedApplicationRecord.class);

        // assert
        assertEquals("RegionScopedApplicationRecord", result.getType());
        assertEquals(108, result.getVersion());
        assertEquals("regionapp:pi-network-manager", result.getUrl());
        assertEquals("pi-network-manager", result.getApplicationName());
        assertEquals(2, result.getRequiredActive());
        assertEquals(2, result.getActiveNodeMap().size());
        assertTrue(result.getActiveNodeMap().containsKey("10.19.1.200/24"));
        assertTrue(result.getActiveNodeMap().containsKey("10.19.1.333/24"));
        assertEquals("010138EBAAE248B0E69DD51283E34921F6B50000", result.getActiveNodeMap().get("10.19.1.200/24").getObject());
        assertTrue(result.getActiveNodeMap().get("10.19.1.200/24").getTimeStamp() > 0);
        assertNull(result.getActiveNodeMap().get("10.19.1.333/24"));

        // setup again
        String json2 = koalaJsonParser.getJson(result);

        // act again
        ApplicationRecord result2 = (ApplicationRecord) koalaJsonParser.getObject(json2, RegionScopedApplicationRecord.class);

        // assert again
        assertEquals("RegionScopedApplicationRecord", result2.getType());
        assertEquals(108, result2.getVersion());
        assertEquals("regionapp:pi-network-manager", result2.getUrl());
        assertEquals("pi-network-manager", result2.getApplicationName());
        assertEquals(2, result.getRequiredActive());
        assertEquals(2, result.getActiveNodeMap().size());
        assertTrue(result.getActiveNodeMap().containsKey("10.19.1.200/24"));
        assertTrue(result.getActiveNodeMap().containsKey("10.19.1.333/24"));
        assertEquals("010138EBAAE248B0E69DD51283E34921F6B50000", result.getActiveNodeMap().get("10.19.1.200/24").getObject());
        assertTrue(result.getActiveNodeMap().get("10.19.1.200/24").getTimeStamp() > 0);
        assertNull(result.getActiveNodeMap().get("10.19.1.333/24"));
    }

    @Test
    public void testAddCurrentlyActiveNodeUsesEmptySlot() {
        // setup
        applicationRecord = new GlobalScopedApplicationRecord("testApp", 1, Arrays.asList(new String[] { ip1, ip2 }));
        assertTrue(applicationRecord.addCurrentlyActiveNode(id, -1));

        // act
        boolean result = applicationRecord.addCurrentlyActiveNode(anotherId, -1);

        // assert
        assertTrue(result);
        assertEquals(2, applicationRecord.getActiveNodeMap().size());
        assertTrue(applicationRecord.getActiveNodeMap().get(ip1).getObject().equals(id.toStringFull()));
        assertTrue(applicationRecord.getActiveNodeMap().get(ip2).getObject().equals(anotherId.toStringFull()));
    }

    @Test
    public void testAddCurrentlyActiveNodeUsesEmptySlotBeforeExpiredEntry() {
        // setup
        applicationRecord = new GlobalScopedApplicationRecord("testApp", 1, Arrays.asList(new String[] { ip1, ip2, ip3 }));
        assertTrue(applicationRecord.addCurrentlyActiveNode(id, -1));
        assertTrue(applicationRecord.addCurrentlyActiveNode(anotherId, -1));
        applicationRecord.getActiveNodeMap().get(ip2).setTimeStamp(123L);

        // act
        boolean result = applicationRecord.addCurrentlyActiveNode(yetAnotherId, 30);

        // assert
        assertTrue(result);
        assertEquals(3, applicationRecord.getActiveNodeMap().size());
        assertTrue(applicationRecord.getActiveNodeMap().get(ip1).getObject().equals(id.toStringFull()));
        assertTrue(applicationRecord.getActiveNodeMap().get(ip2).getObject().equals(anotherId.toStringFull()));
        assertTrue(applicationRecord.getActiveNodeMap().get(ip3).getObject().equals(yetAnotherId.toStringFull()));
    }

    @Test
    public void testAddCurrentlyActiveNodeUsesExpiredSlotIfNoEmptyAvailable() throws InterruptedException {
        // setup
        applicationRecord = new GlobalScopedApplicationRecord("testApp", 1, Arrays.asList(new String[] { ip1, ip2 }));
        assertTrue(applicationRecord.addCurrentlyActiveNode(id, -1));
        assertTrue(applicationRecord.addCurrentlyActiveNode(anotherId, -1));
        applicationRecord.getActiveNodeMap().get(ip2).setTimeStamp(System.currentTimeMillis() - 1001);

        // act
        boolean result = applicationRecord.addCurrentlyActiveNode(yetAnotherId, 1);

        // assert
        assertTrue(result);
        assertEquals(2, applicationRecord.getActiveNodeMap().size());
        assertTrue(applicationRecord.getActiveNodeMap().get(ip1).getObject().equals(id.toStringFull()));
        assertTrue(applicationRecord.getActiveNodeMap().get(ip2).getObject().equals(yetAnotherId.toStringFull()));
    }

    @Test
    public void testAddCurrentlyActiveNodeReturnsFalseEvenIfExpiredWhenCalledWithoutExpiry() throws InterruptedException {
        // setup
        applicationRecord = new GlobalScopedApplicationRecord("testApp", 1, Arrays.asList(new String[] { ip1, ip2 }));
        assertTrue(applicationRecord.addCurrentlyActiveNode(id, -1));
        assertTrue(applicationRecord.addCurrentlyActiveNode(anotherId, -1));
        applicationRecord.getActiveNodeMap().get(ip2).setTimeStamp(123L);

        // act
        boolean result = applicationRecord.addCurrentlyActiveNode(yetAnotherId, -1);

        // assert
        assertFalse(result);
        assertEquals(2, applicationRecord.getActiveNodeMap().size());
        assertTrue(applicationRecord.getActiveNodeMap().get(ip1).getObject().equals(id.toStringFull()));
        assertTrue(applicationRecord.getActiveNodeMap().get(ip2).getObject().equals(anotherId.toStringFull()));
    }

    @Test
    public void testAddCurrentlyActiveNodeReturnsFalseWhenNoFreeOrExpired() {
        // setup
        applicationRecord = new GlobalScopedApplicationRecord("testApp", 1, Arrays.asList(new String[] { ip1, ip2 }));
        assertTrue(applicationRecord.addCurrentlyActiveNode(id, -1));
        assertTrue(applicationRecord.addCurrentlyActiveNode(anotherId, -1));

        // act
        boolean result = applicationRecord.addCurrentlyActiveNode(yetAnotherId, 30);

        // assert
        assertFalse(result);
        assertEquals(2, applicationRecord.getActiveNodeMap().size());
        assertTrue(applicationRecord.getActiveNodeMap().get(ip1).getObject().equals(id.toStringFull()));
        assertTrue(applicationRecord.getActiveNodeMap().get(ip2).getObject().equals(anotherId.toStringFull()));
    }

    @Test(expected = IllegalAnnotationException.class)
    public void testThatClosestActiveNodeIdThrowsExceptionIfAppRecordClassDoesNotContainEntityScopeAnnotation() throws Exception {
        // setup
        ApplicationRecord applicationRecord = new ApplicationRecord() {
            @Override
            public String getUrl() {
                return null;
            }

            @Override
            public String getUriScheme() {
                return null;
            }

            @Override
            public String getType() {
                return null;
            }
        };

        // act
        applicationRecord.getClosestActiveNodeId(anotherId);
    }
}

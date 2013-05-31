package com.bt.pi.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.GlobalScopedApplicationRecord;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.exception.KoalaException;
import com.bt.pi.core.past.content.payload.PastContentPayload;

public class KoalaJsonParserTest {
    private long number = 12321432L;
    private String computer = "computer";
    private String nodeHandle = "nodeHandle";
    private double cpu = 12.4;
    private KoalaJsonParser koalaJsonParser;
    private TestPayload pojo;
    JSONObject body;
    JSONObject machinehealthpayload;
    private ObjectMapper mockMapper;

    public KoalaJsonParserTest(){}

    @Before
    public void before() throws JSONException {
        pojo = new TestPayload(number, nodeHandle, computer, number, number, cpu);
        koalaJsonParser = new KoalaJsonParser();
        machinehealthpayload = new JSONObject();
        machinehealthpayload.put(computer, computer);
        machinehealthpayload.put(nodeHandle, nodeHandle);
        machinehealthpayload.put("freeDiskSpace", number);
        machinehealthpayload.put("totalDiskSpace", number);
        machinehealthpayload.put("eventTime", number);
        machinehealthpayload.put("averageCPULoad", cpu);

        mockMapper = mock(ObjectMapper.class);
    }

    @Test
    public void testGetJson() throws Exception {
        // act
        String json = StringUtils.deleteWhitespace(koalaJsonParser.getJson(pojo));

        // assert
        assertTrue(json.contains("\"computer\":\"computer\""));
        assertTrue(json.contains("\"freeDiskSpace\":12321432"));
        assertTrue(json.contains("\"totalDiskSpace\":12321432"));
        assertTrue(json.contains("\"averageCPULoad\":12.4"));
        assertTrue(json.contains("\"nodeHandle\":\"nodeHandle\""));
        assertTrue(json.contains("\"eventTime\":12321432"));
    }

    @Test
    public void testGetJsonDoesNotMarshalNulls() throws Exception {
        // setup
        @SuppressWarnings("unused")
        final class MyTestClass {
            public String getNonNullString() {
                return "helloworld";
            }

            public String getNullString() {
                return null;
            }
        }
        MyTestClass test = new MyTestClass();

        // act
        String json = StringUtils.deleteWhitespace(koalaJsonParser.getJson(test));

        // assert
        assertTrue(json.contains("\"nonNullString\":\"helloworld\""));
        assertFalse(json.contains("\"nullString\":null"));
    }

    @Test
    public void testGetJsonIsValidJson() throws Exception {
        String json = koalaJsonParser.getJson(pojo);

        // act
        JSONObject myPojo = new JSONObject(json);

        // assert
        assertEquals(computer, myPojo.get(computer));
        assertEquals(nodeHandle, myPojo.get(nodeHandle));
        assertEquals(number, myPojo.getLong("freeDiskSpace"), 0.0);
        assertEquals(number, myPojo.getLong("totalDiskSpace"), 0.0);
        assertEquals(number, myPojo.getLong("eventTime"), 0.0);
        assertEquals(cpu, myPojo.getDouble("averageCPULoad"), 0.0);
    }

    @Test
    public void testGetObject() throws Exception {
        // act
        TestPayload result = (TestPayload) koalaJsonParser.getObject(machinehealthpayload.toString(), TestPayload.class);

        assertEquals(pojo, result);
    }

    @Test
    public void testExtraValueInJson() throws Exception {
        // setup
        machinehealthpayload.put("extraValue", "extraValue");

        // act
        TestPayload myPojo = (TestPayload) koalaJsonParser.getObject(machinehealthpayload.toString(), TestPayload.class);

        // assert
        assertEquals(computer, pojo.getComputer());
        assertEquals(nodeHandle, myPojo.getNodeHandle());
        assertEquals(number, myPojo.getFreeDiskSpace(), 0.0);
        assertEquals(number, myPojo.getTotalDiskSpace(), 0.0);
        assertEquals(number, myPojo.getEventTime(), 0.0);
        assertEquals(cpu, myPojo.getAverageCPULoad(), 0.0);
    }

    @Test
    public void testMissingValueInJson() throws Exception {
        // setup
        machinehealthpayload.remove(computer);

        // act
        TestPayload myPojo = (TestPayload) koalaJsonParser.getObject(machinehealthpayload.toString(), TestPayload.class);

        // assert
        assertNull(myPojo.getComputer());
        assertEquals(nodeHandle, myPojo.getNodeHandle());
        assertEquals(number, myPojo.getFreeDiskSpace(), 0.0);
        assertEquals(number, myPojo.getTotalDiskSpace(), 0.0);
        assertEquals(number, myPojo.getEventTime(), 0.0);
        assertEquals(cpu, myPojo.getAverageCPULoad(), 0.0);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = KoalaException.class)
    public void testMapperThrowsIOExceptionOnGetObject() throws JsonGenerationException, JsonMappingException, IOException {
        // setup
        koalaJsonParser.overrideDefaultObjectMapper(mockMapper);
        when(mockMapper.readValue(isA(String.class), (Class) anyObject())).thenThrow(new IOException());

        // act
        koalaJsonParser.getObject(machinehealthpayload.toString(), TestPayload.class);
    }

    @Test(expected = KoalaException.class)
    public void testMapperThrowsIOExceptionOnGetJson() throws JsonGenerationException, JsonMappingException, IOException {
        // setup
        koalaJsonParser.overrideDefaultObjectMapper(mockMapper);
        doThrow(new IOException()).when(mockMapper).writeValue(isA(OutputStream.class), anyObject());

        // act
        koalaJsonParser.getJson(machinehealthpayload);
    }

    @Test(expected = KoalaException.class)
    public void testMapperThrowsJsonGenerationExceptionOnGetJson() throws JsonGenerationException, JsonMappingException, IOException {
        // setup
        koalaJsonParser.overrideDefaultObjectMapper(mockMapper);
        doThrow(new JsonGenerationException("yay")).when(mockMapper).writeValue(isA(OutputStream.class), anyObject());

        // act
        koalaJsonParser.getJson(machinehealthpayload);
    }

    @Test(expected = KoalaException.class)
    public void testMapperThrowsJsonMappingExceptionOnGetJson() throws JsonGenerationException, JsonMappingException, IOException {
        // setup
        koalaJsonParser.overrideDefaultObjectMapper(mockMapper);
        doThrow(new JsonMappingException("yay")).when(mockMapper).writeValue(isA(OutputStream.class), anyObject());

        // act
        koalaJsonParser.getJson(machinehealthpayload);
    }

    @Test
    public void testStaticGetterWithNoSetter() {
        TestPojo pojo = new TestPojo();
        pojo.setFoo("fooYeah");

        String json = koalaJsonParser.getJson(pojo);

        TestPojo pojo2 = (TestPojo) koalaJsonParser.getObject(json, TestPojo.class);

        assertEquals(pojo, pojo2);
    }

    @Test
    public void testJacksonWithList() throws Exception {
        Map<String, TestPojo> numbers = new HashMap<String, TestPojo>();
        numbers.put("1", new TestPojo("1"));
        numbers.put("2", new TestPojo("2"));
        numbers.put("3", new TestPojo("3"));
        AnotherLevel otherPojo = new AnotherLevel();
        otherPojo.setNumbers(numbers);

        String json = koalaJsonParser.getJson(otherPojo);

        AnotherLevel result = (AnotherLevel) koalaJsonParser.getObject(json, AnotherLevel.class);
        String resultJson = koalaJsonParser.getJson(result);

        assertEquals(otherPojo, result);
        assertEquals(json, resultJson);
    }

    @Test
    public void shouldReturnValueOfFieldWithSimpleJson() throws Exception {
        // setup
        String json = "{\"type\" : \"MachineHealthPayload\",\"computer\" : \"computer\",\"nodeHandle\" : \"nodeHandle\",\"freeDiskSpace\" : 12321432,\"totalDiskSpace\" : 12321432,\"key\" : \"value\",\"averageCPULoad\" : 12.4}";
        // act
        String result = koalaJsonParser.getValueOfScalar(json, "key");

        // assert
        assertEquals("value", result);
    }

    @Test
    public void shouldReturnValueOfFieldWithJsonContainingList() throws Exception {
        // setup
        String json = "{\"type\" : \"MachineHealthPayload\",\"stringList\" : [{\"type\" : \"list1\",\"freeDiskSpace\" : 0,\"key\" : \"not value\"}, {\"type\" : \"list2\",\"freeDiskSpace\" : 0,\"key\" : \"not value again\"}],\"totalDiskSpace\" : 12321432,\"key\" : \"value\",\"averageCPULoad\" : 12.4}";

        // act
        String result = koalaJsonParser.getValueOfScalar(json, "key");

        // assert
        assertEquals("value", result);
    }

    @Test
    public void shouldReturnNullIfAskedForValueOfNonScalar() throws Exception {
        // setup
        String json = "{\"type\" : \"MachineHealthPayload\",\"stringList\" : [{\"type\" : \"list1\",\"freeDiskSpace\" : 0,\"key\" : \"not value\"}, {\"type\" : \"list2\",\"freeDiskSpace\" : 0,\"key\" : \"not value again\"}],\"totalDiskSpace\" : 12321432,\"key\" : \"value\",\"averageCPULoad\" : 12.4}";

        // act
        String result = koalaJsonParser.getValueOfScalar(json, "stringList");

        // assert
        assertNull(result);
    }

    @Test
    public void shouldReturnNullIfAskedForNonExistentKey() throws Exception {
        // setup
        String json = "{\"type\" : \"MachineHealthPayload\",\"stringList\" : [{\"type\" : \"list1\",\"freeDiskSpace\" : 0,\"key\" : \"not value\"}, {\"type\" : \"list2\",\"freeDiskSpace\" : 0,\"key\" : \"not value again\"}],\"totalDiskSpace\" : 12321432,\"key\" : \"value\",\"averageCPULoad\" : 12.4}";

        // act
        String result = koalaJsonParser.getValueOfScalar(json, "foobar");

        // assert
        assertNull(result);
    }

    @Test
    public void shouldReturnValueOfFieldWithJsonContainingMap() throws Exception {
        // setup
        String json = "{\"type\" : \"MachineHealthPayload\", \"numbers\" : {\"3\" : {\"name\" : \"foobar\",\"stringList\" : [{\"type\" : \"list1\",\"freeDiskSpace\" : 0,\"key\" : \"not value\"}, {\"type\" : \"list2\",\"freeDiskSpace\" : 0,\"key\" : \"not value again\"}],\"foo\" : \"3\"},\"2\" : {\"name\" : \"foobar\",\"foo\" : \"2\"},\"1\" : {\"name\" : \"foobar\",\"foo\" : \"1\"}},\"key\":\"value\"}";

        // act
        String result = koalaJsonParser.getValueOfScalar(json, "key");

        // assert
        assertEquals("value", result);
    }

    @Test
    @Ignore
    public void testGetJsonOnLargeObject() throws Exception {
        // setup
        final ApplicationRecord rec = new GlobalScopedApplicationRecord("big mama");
        final List<String> resources = new ArrayList<String>();
        for (int i = 0; i < 40000; i++)
            resources.add(Long.toString(new Random().nextLong()));
        rec.addResources(resources);

        // act
        long startTimestamp = System.currentTimeMillis();
        String json = koalaJsonParser.getJson(rec);
        long endTimestamp = System.currentTimeMillis();

        Thread.sleep(3000);
        System.err.println("Time taken: " + (endTimestamp - startTimestamp));
        System.err.println("Num chars: " + json.length());
    }
}

class TestPojo {
    String foo;
    String test;
    List<TestPayload> stringList;

    private static final String NAME = "foobar";

    public TestPojo() {

    }

    public TestPojo(String bar) {
        foo = bar;
        stringList = new ArrayList<TestPayload>();
        stringList.add(new TestPayload(System.currentTimeMillis(), bar, bar, 23L, 0, 33.3));
        stringList.add(new TestPayload(System.currentTimeMillis(), bar, bar, 23L, 0, 33.3));
    }

    public List<TestPayload> getStringList() {
        return stringList;
    }

    public void setStringList(List<TestPayload> list) {
        stringList = list;
    }

    public String getName() {
        return TestPojo.NAME;
    }

    public String getFoo() {
        return foo;
    }

    public String getNull() {
        return null;
    }

    public String getTest() {
        return test;
    }

    public void setFoo(String str) {
        foo = str;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof TestPojo))
            return false;
        TestPojo castOther = (TestPojo) other;
        return new EqualsBuilder().append(foo, castOther.foo).append(test, castOther.test).append(stringList, castOther.stringList).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(foo).append(test).append(stringList).toHashCode();
    }

}

class AnotherLevel extends PiEntityBase {

    private Map<String, TestPojo> numbers;

    public AnotherLevel() {
    }

    public void setNumbers(Map<String, TestPojo> numbers2) {
        numbers = numbers2;
    }

    public Map<String, TestPojo> getNumbers() {
        return numbers;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AnotherLevel))
            return false;
        AnotherLevel castOther = (AnotherLevel) other;
        return new EqualsBuilder().append(numbers, castOther.numbers).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(numbers).toHashCode();
    }

    public String getUrl() {
        return null;
    }

    @Override
    public String getUriScheme() {
        return getClass().getSimpleName();
    }
}

class TestPayload implements PastContentPayload {
    private static final String TYPE = "MachineHealthPayload";
    private static final long serialVersionUID = 1L;
    private String computer;
    private long freeDiskSpace;
    private long totalDiskSpace;
    private double averageCPULoad;
    private String nodeHandle;
    private long eventTime;

    public TestPayload(long timestamp, String aNodeHandle, String computerName, long totalspace, long freespace, double cpuLoad) {
        setEventTime(timestamp);
        setNodeHandle(aNodeHandle);
        setComputer(computerName);
        setTotalDiskSpace(totalspace);
        setFreeDiskSpace(freespace);
        setAverageCPULoad(cpuLoad);
    }

    public TestPayload() {
    }

    public void setAverageCPULoad(double cPULoad) {
        this.averageCPULoad = cPULoad;
    }

    public double getAverageCPULoad() {
        return averageCPULoad;
    }

    public String getComputer() {
        return computer;
    }

    public void setComputer(String computerName) {
        this.computer = computerName;
    }

    public void setNodeHandle(String nh) {
        nodeHandle = nh;
    }

    public String getNodeHandle() {
        return nodeHandle;
    }

    public long getTotalDiskSpace() {
        return totalDiskSpace;
    }

    public void setTotalDiskSpace(long totalSpace) {
        this.totalDiskSpace = totalSpace;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long aTimeStamp) {
        this.eventTime = aTimeStamp;
    }

    public long getFreeDiskSpace() {
        return freeDiskSpace;
    }

    public void setFreeDiskSpace(long freeSpace) {
        this.freeDiskSpace = freeSpace;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof TestPayload))
            return false;
        TestPayload castOther = (TestPayload) other;
        return new EqualsBuilder().append(computer, castOther.computer).append(freeDiskSpace, castOther.freeDiskSpace).append(totalDiskSpace, castOther.totalDiskSpace).append(averageCPULoad, castOther.averageCPULoad)
                .append(nodeHandle, castOther.nodeHandle).append(eventTime, castOther.eventTime).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(computer).append(freeDiskSpace).append(totalDiskSpace).append(averageCPULoad).append(nodeHandle).append(eventTime).toHashCode();
    }

    // @JsonIgnore
    @Override
    public String getType() {
        return TestPayload.TYPE;
    }

}

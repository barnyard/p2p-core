package com.bt.pi.core.scribe;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class KoalaScribeDataParserTest {

    private KoalaPiEntityFactory parser;
    private JSONObject networkNodeUpdateJsonObject;
    private List<PiEntity> koalaScribeTypes;

    @Before
    public void before() throws JSONException {
        parser = new KoalaPiEntityFactory();
        parser.setKoalaJsonParser(new KoalaJsonParser());
        koalaScribeTypes = new ArrayList<PiEntity>();
        parser.setPiEntityTypes(koalaScribeTypes);
        networkNodeUpdateJsonObject = new JSONObject(parser.getJson(new TestScribeData(true, true)));
    }

    @Test
    public void testParseScribeData() {
        koalaScribeTypes.add(new TestScribeData());
        parser.setPiEntityTypes(koalaScribeTypes);
        assertTrue(parser.getPiEntity(networkNodeUpdateJsonObject.toString()) instanceof TestScribeData);
    }

    /*
     * These negative tests ensure that we don't puke hard. I don't think it
     * would be a good idea for us to die on a bad message. Log them and keep
     * the node running.
     */

    @Test
    public void testParseScribeDataWithNullJSONString() {
        assertNull(parser.getPiEntity(null));
    }

    @Test
    public void testParseScribeDataWithMalformedJSONString() {
        assertNull(parser.getPiEntity("{#@$@#$R#@$@#$E@#ER#"));
    }

    @Test
    public void testParseScribeDataWithUnknownType() throws Exception {
        JSONObject badObject = networkNodeUpdateJsonObject.put(PiEntity.TYPE_PARAM, "anUnknownTypeToConfuseYou");
        assertNull(parser.getPiEntity(badObject.toString()));
    }

    @Test
    public void testParseScribeDataWithKnownTypeAndBadMapping() throws Exception {
        koalaScribeTypes.add(new TestScribeData());
        assertNull(parser.getPiEntity(networkNodeUpdateJsonObject.toString()));
    }
}

class TestScribeData extends PiEntityBase {
    public static final String TYPE = "NetworkNodeUpdate";
    private boolean connected;
    private boolean existingNode;

    public TestScribeData(boolean isConnected, boolean isExistingNode) {
        this();
        connected = isConnected;
        existingNode = isExistingNode;
    }

    public TestScribeData() {
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean nodeconnected) {
        this.connected = nodeconnected;
    }

    public boolean isExistingNode() {
        return existingNode;
    }

    public void setExistingNode(boolean isexistingNode) {
        this.existingNode = isexistingNode;
    }

    @Override
    public String getType() {
        return TestScribeData.TYPE;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof TestScribeData))
            return false;
        TestScribeData castOther = (TestScribeData) other;
        return new EqualsBuilder().append(isConnected(), castOther.isConnected()).append(isExistingNode(), castOther.isExistingNode()).append(this.getType(), castOther.getType()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(connected).toHashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder().append("connected=" + connected).append(",existingNode=" + existingNode).toString();
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getUriScheme() {
        return getClass().getSimpleName();
    }
}

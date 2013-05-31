/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.reporter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReportableEntityTest {
    private static final String NODE_ID = "nodeId";

    @Test
    public void itShouldHaveTheNodeId() {
        ReportableEntity<Object> reportableEntity = new ReportableEntity<Object>(NODE_ID) {
            @Override
            public String getType() {
                return null;
            }

            @Override
            public String getUrl() {
                return null;
            }

            @Override
            public Object[] getKeysForMap() {
                return null;
            }

            @Override
            public int getKeysForMapCount() {
                return 0;
            }

            @Override
            public int compareTo(Object arg0) {
                return 0;
            }

            @Override
            public boolean equals(Object other) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public long getCreationTime() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getUriScheme() {
                // TODO Auto-generated method stub
                return null;
            }
        };

        assertEquals(NODE_ID, reportableEntity.getNodeId());
    }
}

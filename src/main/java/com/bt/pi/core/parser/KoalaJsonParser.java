//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.springframework.stereotype.Component;

import com.bt.pi.core.exception.KoalaException;

@Component
public class KoalaJsonParser {
    private static final Log LOG = LogFactory.getLog(KoalaJsonParser.class);
    private static final String COLON = ": ";
    private JsonFactory jsonFactory;
    private ObjectMapper mapper;

    public KoalaJsonParser() {
        jsonFactory = new JsonFactory();

        mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        mapper.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected void overrideDefaultObjectMapper(ObjectMapper objectMapper) {
        mapper = objectMapper;
    }

    public String getValueOfScalar(String json, String fieldName) {
        JsonParser jsonParser = null;
        try {
            jsonParser = jsonFactory.createJsonParser(json);
            if (jsonParser.nextToken() != JsonToken.START_OBJECT)
                throw new KoalaException("Json string did not start with object");

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String name = jsonParser.getCurrentName();
                JsonToken nextValue = jsonParser.nextToken();
                String result = jsonParser.getText();
                if (!nextValue.isScalarValue())
                    jsonParser.skipChildren();
                else if (name.equals(fieldName))
                    return result;
            }
        } catch (JsonParseException e) {
            LOG.error(e.getMessage() + COLON + e);
            throw new KoalaException(e);
        } catch (IOException e) {
            LOG.error(e.getMessage() + COLON + e);
            throw new KoalaException(e);
        } finally {
            if (jsonParser != null)
                try {
                    jsonParser.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage() + COLON + e);
                    throw new KoalaException(e);
                }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public Object getObject(String json, Class clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            LOG.error(e.getMessage() + COLON + e);
            throw new KoalaException(e);
        }
    }

    public String getJson(Object o) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("getJson for %s", o == null ? "null" : o.getClass()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            mapper.writeValue(out, o);
        } catch (JsonGenerationException e) {
            LOG.error(e.getMessage() + COLON + e);
            throw new KoalaException(e);
        } catch (JsonMappingException e) {
            LOG.error(e.getMessage() + COLON + e);
            throw new KoalaException(e);
        } catch (IOException e) {
            LOG.error(e.getMessage() + COLON + e);
            throw new KoalaException(e);
        }
        String json = new String(out.toByteArray());
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Parsed entity: %s", json));
        return json;
    }
}

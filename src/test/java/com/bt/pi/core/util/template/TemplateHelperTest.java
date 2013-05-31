package com.bt.pi.core.util.template;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.util.FileUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import freemarker.template.Configuration;

public class TemplateHelperTest {
    private TemplateHelper templateHelper = new TemplateHelper();
    private File tmpFile;

    @Before
    public void before() throws Exception {
        Configuration configuration = new Configuration();
        tmpFile = File.createTempFile("test", "ftl");
        File dir = tmpFile.getParentFile();
        configuration.setDirectoryForTemplateLoading(dir);
        setField(templateHelper, "freeMarkerConfiguration", configuration);
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

    @Test
    public void testGenerate() {
        // setup
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("name", "adrian");
        FileUtil.writeAsString(tmpFile, "name=${name}");

        // act
        String result = templateHelper.generate(tmpFile.getName(), model);

        // assert
        assertEquals("name=adrian", result);
    }
}

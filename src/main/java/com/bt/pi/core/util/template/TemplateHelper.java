package com.bt.pi.core.util.template;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
public class TemplateHelper {
    private static final Log LOG = LogFactory.getLog(TemplateHelper.class);
    private static final String ERROR_PROCESSING_TEMPLATE = "error processing template %s";

    @Resource
    private Configuration freeMarkerConfiguration;

    public TemplateHelper() {
    }

    public String generate(String template, Map<String, Object> model) {
        LOG.debug(String.format("generate(%s, %s)", template, model));
        LOG.debug(freeMarkerConfiguration.getSettings());
        try {
            Template template2 = freeMarkerConfiguration.getTemplate(template);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template2, model);
        } catch (IOException e) {
            LOG.error(String.format(ERROR_PROCESSING_TEMPLATE, template), e);
            throw new RuntimeException(String.format(ERROR_PROCESSING_TEMPLATE, template), e);
        } catch (TemplateException e) {
            LOG.error(String.format(ERROR_PROCESSING_TEMPLATE, template), e);
            throw new RuntimeException(String.format(ERROR_PROCESSING_TEMPLATE, template), e);
        }
    }
}

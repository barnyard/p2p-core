package com.bt.pi.core.mail;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;

@Component
public class MailSender {
    private static final Log LOG = LogFactory.getLog(MailSender.class);
    @Resource
    private JavaMailSender javaMailSender;
    private String fromAddress;

    public MailSender() {
        this.javaMailSender = null;
    }

    @Property(key = "mail.from.address", defaultValue = "PI@bt.com")
    public void setFromAddress(String aFromAddress) {
        this.fromAddress = aFromAddress;
    }

    public void send(String to, String subject, String text) {
        LOG.debug(String.format("send(%s, %s, %s)", to, subject, text));
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
            helper.setText(text, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromAddress);
            // SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            // simpleMailMessage.setTo(to);
            // simpleMailMessage.setSubject(subject);
            // simpleMailMessage.setText(text);
            // simpleMailMessage.setFrom(fromAddress);
            // javaMailSender.send(simpleMailMessage);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            LOG.error(e);
        }
    }
}

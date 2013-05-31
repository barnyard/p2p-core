package com.bt.pi.core.mail;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mail.javamail.JavaMailSender;

@RunWith(MockitoJUnitRunner.class)
public class MailSenderTest {
    @InjectMocks
    private MailSender mailSender = new MailSender();
    @Mock
    private JavaMailSender javaMailSender;
    private String text = "this is the message line 1" + System.getProperty("line.separator") + "this is line 2";
    private String subject = "this is a test";
    private String to = "fred@bloggs.com";
    private String fromAddress = "pi.cloud@bt.com";
    @Mock
    private MimeMessage mimeMessage;

    @Test
    public void testSend() throws Exception {
        // setup
        this.mailSender.setFromAddress(fromAddress);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // act
        this.mailSender.send(to, subject, text);

        // assert
        verify(javaMailSender).send(mimeMessage);
    }
}

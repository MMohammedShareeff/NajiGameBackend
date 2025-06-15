package com.naji.email;

import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String email, String subject, String body){
        if(Objects.isNull(email) || email.isEmpty()){
            throw new ResourceNotFoundException(ExceptionsMessages.getResourceNotFoundMessage(String.class));
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setFrom("najiGame");
        message.setText(body);
        mailSender.send(message);
    }
}

package com.naji.redis;

import com.naji.email.EmailService;
import com.naji.player.PlayerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    private final RedisTemplate<String, String> verificationTemplate;
    private final RedisTemplate<String, PlayerRequest> playerRequestTemplate;

    public RedisService(
            @Qualifier("verificationCodeRedisTemplate") RedisTemplate<String, String> verificationTemplate,
            @Qualifier("playerRequestRedisTemplate") RedisTemplate<String, PlayerRequest> playerRequestTemplate,
            EmailService emailService) {
        this.verificationTemplate = verificationTemplate;
        this.playerRequestTemplate = playerRequestTemplate;
        this.emailService = emailService;
    }

    public void saveVerificationCode(String email) {
        String code = generateVerificationCode();
        logger.debug("Generated code for {}: {}", email, code);

        verificationTemplate.opsForValue().set("verification:" +  email, code, 5, TimeUnit.MINUTES);
        logger.debug("Stored verification code in Redis: {}", code);

        String body = String.format("Your verification code for Naji game is %s\n" +
                "If you did not request this code.. just ignore this email.", code);
        emailService.sendEmail(email, "Naji email verification required", body);
    }

    public void savePassword(String email, String password){
        verificationTemplate.opsForValue().set("newPass:" + email, password, 5, TimeUnit.MINUTES);
        logger.info("new password for an account with the email:" + email);
    }

    public String getPassword(String email){
        return verificationTemplate.opsForValue().get("newPass:" + email);
    }

    public void saveAccountData(PlayerRequest playerRequest) {
        String email = playerRequest.getEmail();
        playerRequestTemplate.opsForValue().set("account:" + email, playerRequest, 5, TimeUnit.MINUTES);
        logger.info(":account data stored temporarily for the email: " + email);
    }

    public PlayerRequest getAccountData(String email) {
        return playerRequestTemplate.opsForValue().get("account:" + email);
    }

    public String getVerificationCode(String email) {
        String code = verificationTemplate.opsForValue().get("verification:" + email);
        logger.info("Retrieved verification code for email {}: {}", email, code);
        return code;
    }

    public boolean validateVerificationCode(String email, String code) {
        String storedCode = getVerificationCode(email);
        logger.info("Validating code for email {}: expected {}, received {}", email, storedCode, code);
        return Objects.nonNull(storedCode) && storedCode.equals(code);
    }

    public String generateVerificationCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        SecureRandom random = new SecureRandom();

        StringBuilder verificationCode = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int randomIndex = random.nextInt(characters.length());
            verificationCode.append(characters.charAt(randomIndex));
        }
        return verificationCode.toString();
    }

}

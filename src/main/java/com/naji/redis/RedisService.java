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

        verificationTemplate.opsForValue().set("verification:" +  email, code, 5, TimeUnit.MINUTES);

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

    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final int MAX_LOGIN_FAILURES = 10;

    public boolean tryConsumeDailyGame(Long playerId, int dailyLimit) {
        if (dailyLimit <= 0) {
            return true;
        }
        String key = "gamesToday:" + playerId + ":" + java.time.LocalDate.now();
        Long started = verificationTemplate.opsForValue().increment(key);
        if (started != null && started == 1) {
            verificationTemplate.expire(key, 26, TimeUnit.HOURS);
        }
        return started != null && started <= dailyLimit;
    }

    public boolean isLoginLocked(String userName) {
        String failures = verificationTemplate.opsForValue().get("loginFails:" + userName);
        return failures != null && Integer.parseInt(failures) >= MAX_LOGIN_FAILURES;
    }

    public void recordLoginFailure(String userName) {
        String key = "loginFails:" + userName;
        Long failures = verificationTemplate.opsForValue().increment(key);
        if (failures != null && failures == 1) {
            verificationTemplate.expire(key, 10, TimeUnit.MINUTES);
        }
    }

    public void clearLoginFailures(String userName) {
        verificationTemplate.delete("loginFails:" + userName);
    }

    public void savePendingUpdate(Long playerId, PlayerRequest playerRequest) {
        playerRequestTemplate.opsForValue().set("pendingUpdate:" + playerId, playerRequest, 10, TimeUnit.MINUTES);
    }

    public PlayerRequest getPendingUpdate(Long playerId) {
        return playerRequestTemplate.opsForValue().get("pendingUpdate:" + playerId);
    }

    public void saveUpdateStage(Long playerId, String stage) {
        verificationTemplate.opsForValue().set("updateStage:" + playerId, stage, 10, TimeUnit.MINUTES);
    }

    public String getUpdateStage(Long playerId) {
        return verificationTemplate.opsForValue().get("updateStage:" + playerId);
    }

    public void clearPendingUpdate(Long playerId) {
        playerRequestTemplate.delete("pendingUpdate:" + playerId);
        verificationTemplate.delete("updateStage:" + playerId);
    }

    public void deleteVerificationCode(String email) {
        verificationTemplate.delete("verification:" + email);
    }

    public PlayerRequest getAccountData(String email) {
        return playerRequestTemplate.opsForValue().get("account:" + email);
    }

    public String getVerificationCode(String email) {
        String code = verificationTemplate.opsForValue().get("verification:" + email);
        return code;
    }

    public boolean validateVerificationCode(String email, String code) {
        String storedCode = getVerificationCode(email);
        String attemptsKey = "codeAttempts:" + email;

        if (Objects.nonNull(storedCode) && storedCode.equals(code)) {
            verificationTemplate.delete(attemptsKey);
            return true;
        }

        Long attempts = verificationTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            verificationTemplate.expire(attemptsKey, 10, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts >= MAX_CODE_ATTEMPTS) {
            verificationTemplate.delete("verification:" + email);
            verificationTemplate.delete(attemptsKey);
        }
        return false;
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

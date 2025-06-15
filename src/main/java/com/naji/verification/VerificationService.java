package com.naji.verification;

import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.FieldsMisMatchException;
import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.exception.exceptions.TokenNotValidException;
import com.naji.player.*;
import com.naji.redis.RedisService;
import com.naji.security.jwt.JWTUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    private final RedisService redisService;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final PlayerServiceImpl playerServiceImpl;

    @Transactional
    public boolean verifyAndSaveUser(String email, String verificationCode) {
        boolean isValid = redisService.validateVerificationCode(email, verificationCode);
        if (!isValid){
            logger.error("expired code for email: {}", email);
            return false;
        }

        PlayerRequest playerRequest = redisService.getAccountData(email);
        if (Objects.nonNull(playerRequest)) {
            Player player = PlayerMapper.toEntity(playerRequest);
            String password = player.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            player.setPassword(encodedPassword);
            playerRepository.save(player);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean updateAndVerify(String email, String verificationCode,  String token){
       boolean validToken =  jwtUtils.validateJwtToken(token);
       if(!validToken)
           throw new TokenNotValidException("you token is either expired or with wrong format");

        boolean isValid = redisService.validateVerificationCode(email, verificationCode);
        if(!isValid){
            logger.error("expired code for email: {}", email);
            return false;
        }

        Long playerId = jwtUtils.getPlayerIdFromToken(token);
        PlayerRequest playerRequest = redisService.getAccountData(email);
        Player player = playerServiceImpl.getPlayerByIdOrThrowException(playerId);
        updatePlayerDetails(player, playerRequest);
        playerRepository.save(player);
        return true;
    }

    public boolean verifyAndSavePassword(String email, String code){
        boolean isValid = redisService.validateVerificationCode(email, code);
        if (!isValid){
            logger.error("expired code for email: {}", email);
            return false;
        }

        Player player = playerServiceImpl.getPlayerByEmailOrThrowException(email);

        String password = redisService.getPassword(email);
        String encodedPassword = passwordEncoder.encode(password);
        player.setPassword(encodedPassword);
        playerRepository.save(player);
        return true;
    }

    private void updatePlayerDetails(Player existingPlayer, PlayerRequest playerRequest) {
        if (Objects.nonNull(playerRequest.getUserName())) {
            existingPlayer.setUserName(playerRequest.getUserName());
        }
        if (Objects.nonNull(playerRequest.getEmail())) {
            existingPlayer.setEmail(playerRequest.getEmail());
        }
        if (Objects.nonNull(playerRequest.getPassword())) {
            existingPlayer.setPassword(
                    passwordEncoder.encode(playerRequest.getPassword())
            );
        }
    }
}

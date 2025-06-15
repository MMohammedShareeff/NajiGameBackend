package com.naji.player;

import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.FieldsMisMatchException;
import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.exception.exceptions.TokenNotValidException;
import com.naji.exception.exceptions.ValueViolationsException;
import com.naji.redis.RedisService;
import com.naji.room.Room;
import com.naji.room.RoomRepository;
import com.naji.room.RoomServiceImpl;
import com.naji.security.jwt.JWTUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;
    private final JWTUtils jwtUtils;
    private final RoomServiceImpl roomServiceImpl;
    private final RedisService redisService;
    private static final Pattern STARTS_WITH_LETTER = Pattern.compile("^[A-Za-z].*");
    private static final Pattern VALID_PASSWORD = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\W).{8,}$");


    private static final Logger logger = LoggerFactory.getLogger(PlayerServiceImpl.class);

    @Autowired
    public PlayerServiceImpl(@Lazy RoomServiceImpl roomServiceImpl,
                             PlayerRepository playerRepository,
                             RoomRepository roomRepository,
                             JWTUtils jwtUtils,
                             RedisService redisService
    ) {
        this.roomServiceImpl = roomServiceImpl;
        this.playerRepository = playerRepository;
        this.roomRepository = roomRepository;
        this.jwtUtils = jwtUtils;
        this.redisService = redisService;
    }

    @Transactional
    @Override
    public void registerPlayer(PlayerRequest playerRequest) {

        String userName = playerRequest.getUserName();
        String email = playerRequest.getEmail();
        String password = playerRequest.getPassword();
        logger.debug(String.format("Request name: %s\n Request email: %s", userName, email));

        checkDuplicateFieldsValues(userName, email, -1L);
        checkPatterns(userName, password);

        redisService.saveVerificationCode(email);
        redisService.saveAccountData(playerRequest);

        logger.info("email verification required, check your email: " + email);
    }

    @Transactional
    @Override
    public void updatePlayer(PlayerRequest playerRequest, String token) {
        boolean isTokenValid = jwtUtils.validateJwtToken(token);
        if (!isTokenValid)
            throw new TokenNotValidException("you token is either expired or with wrong format");

        String userName = playerRequest.getUserName();
        String email = playerRequest.getEmail();
        String password = playerRequest.getPassword();
        Long id = jwtUtils.getPlayerIdFromToken(token);

        checkPatterns(userName, password);
        checkDuplicateFieldsValues(userName, email, id);
        logger.debug(String.format("Request name: %s\n Request email: %s", userName, email));

        Player player = getPlayerByIdOrThrowException(id);

        redisService.saveAccountData(playerRequest);
        redisService.saveVerificationCode(email);

        logger.info("email verification required, check your email: " + email);
    }

    @Override
    public void resetPassword(ResetPasswordRequest resetRequest){
        String password1 = resetRequest.getNewPassword();
        String password2 =  resetRequest.getNewPasswordAgain();
        String email = resetRequest.getEmail();

        Player player = getPlayerByEmailOrThrowException(email);

        if(!password1.equals(password2)){
            throw new FieldsMisMatchException(ExceptionsMessages.getPasswordsMisMatchMessage());
        }

        if(!VALID_PASSWORD.matcher(password1).matches()){
            throw new DataIntegrityViolationException("password: password must have at least a capital letter," +
                    " a letter, and a special symbol.");
        }

        redisService.savePassword(email, password1);
        redisService.saveVerificationCode(email);

        logger.info("email verification required, check your email: " + email);
    }


    @Transactional
    @Override
    public void joinRoom(String passCode, String token) {
        boolean isTokenValid = jwtUtils.validateJwtToken(token);
        if (!isTokenValid) {
            throw new TokenNotValidException("you token is either expired or with wrong format");
        }
        Room room = roomServiceImpl.getRoomByPassCodeOrThrowException(passCode);

        Long playerId = jwtUtils.getPlayerIdFromToken(token);
        logger.debug(String.format("id extracted from token: %s", playerId));
        Player player = getPlayerByIdOrThrowException(playerId);

        room.getPlayers().add(player);
        roomRepository.save(room);
    }


    @Transactional
    @Override
    public void deletePlayer(Long id, String token) {
        boolean isTokenValid = jwtUtils.validateJwtToken(token);
        if (!isTokenValid)
            throw new TokenNotValidException("you token is either expired or with wrong format");

        Player player = getPlayerByIdOrThrowException(id);
        playerRepository.delete(player);
    }

    public Player getPlayerByIdOrThrowException(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Player.class)
                        )
                );
    }

    public Player getPlayerByUserNameOrThrowException(String userName) {
        return playerRepository.findByUserName(userName)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Player.class)
                        )
                );
    }

    public Player getPlayerByEmailOrThrowException(String email){
        return playerRepository.findByEmail(email)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Player.class)
                        )
                );
    }

    private void checkDuplicateFieldsValues(String userName, String email, Long playerId) {
        Optional<Player> playerWithSameUserName = playerRepository.findByUserName(userName);
        Optional<Player> playerWithSameEmail = playerRepository.findByEmail(email);

        boolean emailExists = playerWithSameEmail.isPresent() && !playerWithSameEmail.get().getId().equals(playerId);
        boolean userNameExist = playerWithSameUserName.isPresent() && !playerWithSameUserName.get().getId().equals(playerId);

        if (emailExists && userNameExist) {
            throw new ValueViolationsException("the following fields are already in use: userName & email");
        }

        if (userNameExist) {
            throw new ValueViolationsException("the userName you entered is already in use");
        }

        if (emailExists) {
            throw new ValueViolationsException("the email you entered is already in use");
        }
    }

    private void checkPatterns(String userName, String password){
        if (Objects.nonNull(userName)  && !STARTS_WITH_LETTER.matcher(userName).matches()
                && Objects.nonNull(password) && !VALID_PASSWORD.matcher(password).matches()) {
            throw new DataIntegrityViolationException("userName: name must start with a letter.\n" +
                    "password: password must have at least a capital letter, a letter, and a special symbol");
        }
        if(Objects.nonNull(userName)  && !STARTS_WITH_LETTER.matcher(userName).matches() ){
            throw new DataIntegrityViolationException("userName: name must start with a letter.");
        }
        if(Objects.nonNull(password) && !VALID_PASSWORD.matcher(password).matches()){
            throw new DataIntegrityViolationException("password: password must have at least a capital letter, a letter, and a special symbol.");
        }
    }
}

package com.naji.room;

import com.naji.player.PlayerMapper;
import com.naji.player.PlayerResponse;
import com.naji.response.ApiResponse;
import com.naji.security.jwt.JWTUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor

@RestController
@RequestMapping("/room")
public class RoomController {

    private final RoomServiceImpl roomServiceImpl;
    private final JWTUtils jwtUtils;

    @GetMapping("/get-players")
    public ApiResponse<List<PlayerResponse>> getPlayersInRoom(@RequestParam String passCode){
        List<PlayerResponse> players = roomServiceImpl.getPlayersInRoom(passCode);
        return new ApiResponse<>(players, HttpStatus.OK);
    }

    @GetMapping("/room-id")
    public ApiResponse<Long> getRoomId(@RequestParam String passCode) {
        Room room = roomServiceImpl.getRoomByPassCodeOrThrowException(passCode);
        return new ApiResponse<>(room.getId(), HttpStatus.OK);
    }

    @GetMapping("/admin")
    public ApiResponse<PlayerResponse> getRoomAdmin(@RequestParam String passCode) {
        Room room = roomServiceImpl.getRoomByPassCodeOrThrowException(passCode);
        return new ApiResponse<>(PlayerMapper.toResponse(room.getAdmin()), HttpStatus.OK);
    }

    @PostMapping("/create")
    public ApiResponse<String> createRoom(@RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        System.out.println(token);
        Room room = roomServiceImpl.createRoom(token);
        String newToken = jwtUtils.generateToken(room.getAdmin().getUserName());

        String message = "Room created successfully, your room passCode is: " + room.getPassCode()
                + ". Your new token is: " + newToken;

        return new ApiResponse<>(message, HttpStatus.CREATED);
    }

    @PostMapping("/add-player")
    public ApiResponse<String> addPlayerToRoom(@RequestParam String passCode,
                                               @RequestHeader(value = "Authorization", required = false) String authHeader){
       String token = jwtUtils.getTokenFromHeader(authHeader);
       if (token == null || !jwtUtils.validateJwtToken(token)) {
           return new ApiResponse<>("your token is either expired or with wrong format", HttpStatus.UNAUTHORIZED);
       }
       String userName = jwtUtils.getUserNameFromJwtToken(token);

       try{
           Room room = roomServiceImpl.addPlayerToRoom(passCode, userName);
           String message = "player with id " + userName + " added successfully to a room with a passCode: " + passCode;
           return new ApiResponse<>(message, HttpStatus.OK);
       }
       catch(Exception ex){
           return new ApiResponse<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
       }
    }

    @PostMapping("/leave")
    public ApiResponse<String> leaveRoom(@RequestParam String passCode,
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        if (token == null || !jwtUtils.validateJwtToken(token)) {
            return new ApiResponse<>("your token is either expired or with wrong format", HttpStatus.UNAUTHORIZED);
        }

        roomServiceImpl.leaveRoom(passCode, jwtUtils.getPlayerIdFromToken(token));
        return new ApiResponse<>("you left the room", HttpStatus.OK);
    }

    @DeleteMapping("/kick-player/{playerName}")
    public ApiResponse<String> kickPlayerFromRoom(@RequestParam String passCode, @RequestHeader("Authorization") String authHeader , @PathVariable String playerName){
        String token = jwtUtils.getTokenFromHeader(authHeader);
        Long requesterId = jwtUtils.getPlayerIdFromToken(token);
        roomServiceImpl.KickPlayerFromRoom(passCode, requesterId, playerName);
        String message = "player kicked out successfully from a room with passcode: " + passCode;
        return new ApiResponse<>(message, HttpStatus.OK);
    }

}

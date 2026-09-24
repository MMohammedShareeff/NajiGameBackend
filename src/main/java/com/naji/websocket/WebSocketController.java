package com.naji.websocket;

import com.naji.game.RoundSubmissionsMessage;
import com.naji.room.RoomPlayersMessage;
import com.naji.round.RoundResultsMessage;
import com.naji.round.RoundStartMessage;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@AllArgsConstructor
@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastGameStart(Long roomId, String message){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/updates", message);
    }

    public void broadcastRoundStarts(Long roomId, RoundStartMessage message){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/round", message);
    }
    public void broadcastLeaderboardUpdate(Long roomId, String leaderboard){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/leaderboard", leaderboard);
    }

    public void broadcastRoundResults(Long roomId, RoundResultsMessage results){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/results", results);
    }

    public void broadcastUpdate(Long roomId, String message){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/updates", message);
    }

    public void broadcastSubmissions(Long roomId, RoundSubmissionsMessage message){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/submissions", message);
    }

    public void broadcastPlayers(Long roomId, RoomPlayersMessage message){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/players", message);
    }

    public void broadcastGameEnds(Long roomId, String finalLeaderboard){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/final_leaderboard", finalLeaderboard);
        broadcastUpdate(roomId, "The game is over.");
    }
}

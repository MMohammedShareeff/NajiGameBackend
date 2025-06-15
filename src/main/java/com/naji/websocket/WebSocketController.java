package com.naji.websocket;

import com.naji.leaderboard.Leaderboard;
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

    public void broadcastRoundStarts(Long roomId, String scenario){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/round", scenario);  // Added the scenario as the message content
    }
    public void broadcastLeaderboardUpdate(Long roomId, String leaderboard){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/leaderboard", leaderboard);
    }

    // Method to broadcast the end of the game along with the final leaderboard
    public void broadcastGameEnds(Long roomId, Leaderboard finalLeaderboard){
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/final_leaderboard", finalLeaderboard);  // Send final leaderboard as message
        String message = "Congratulations to the winner! Good luck to everyone in future games.";
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/updates", message);  // Added missing forward slash
    }
}

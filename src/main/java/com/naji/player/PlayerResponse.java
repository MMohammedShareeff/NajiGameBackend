package com.naji.player;

import com.naji.dashboard.GameStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerResponse {
    private long id;
    private String userName;
    private String email;
    private GameStatus lastGameStatus;
    private String role;
}

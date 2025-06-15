package com.naji.exception;


import com.naji.room.Room;
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
public class ExceptionsMessages {

    public static String getResourceNotFoundMessage(Class<?> myClass){
        return String.format("there is no %s with the given parameter/s", myClass.getSimpleName());
    }

    public static String getInsufficientPlayersMessage(){
        return "at least two players are required to start the game";
    }

    public static String getUnauthorizedMessage(){
        return "you do not have permissions to access this resource";
    }

    public static String getRoomNotActiveMessage(Room room){
        if(room.getCurrentRound().equals(0))
            return "the room is not active yet";
        return "the room is not active anymore";
    }

    public static String getPasswordsMisMatchMessage(){
        return "passwords does not match in the two fields";
    }
}

package com.naji.exception.exceptions;

public class RoomNotActiveException extends RuntimeException{
    public RoomNotActiveException(String message){
        super(message);
    }
}

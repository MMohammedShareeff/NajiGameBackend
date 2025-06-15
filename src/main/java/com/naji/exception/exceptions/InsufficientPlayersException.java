package com.naji.exception.exceptions;

public class InsufficientPlayersException extends RuntimeException {
    public InsufficientPlayersException(String message){
        super(message);
    }
}

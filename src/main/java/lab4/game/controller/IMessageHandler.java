package lab4.game.controller;

import lab4.messages.ReceivedMessage;

public interface IMessageHandler {
    void handleMessage(ReceivedMessage message);
}

package publisher_subscriber;

import lab4.messages.ReceivedMessage;

public interface ReceiveSubscriber {
    void update(ReceivedMessage message);
}
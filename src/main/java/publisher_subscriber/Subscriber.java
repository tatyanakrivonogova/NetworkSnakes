package publisher_subscriber;

import lab4.messages.ReceivedMessage;

public interface Subscriber {
    void update(ReceivedMessage message);
}
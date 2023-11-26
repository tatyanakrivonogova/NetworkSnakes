package publisher_subscriber;

import lab4.messages.ReceivedMessage;

public interface ReceivePublisher {
    void notifyReceiveSubscribers(ReceivedMessage message);

    void addReceiveSubscriber(ReceiveSubscriber subscriber);

}
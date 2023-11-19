package publisher_subscriber;

import lab4.messages.ReceivedMessage;

public interface Publisher {
    void notifySubscribers(ReceivedMessage message);

    void addSubscriber(Subscriber subscriber);

}
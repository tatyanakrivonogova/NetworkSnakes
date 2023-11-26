package publisher_subscriber;

import java.net.InetAddress;

public interface TimeoutPublisher {
    void notifyTimeoutSubscribers(InetAddress ip, int port);

    void addTimeoutSubscriber(TimeoutSubscriber subscriber);

}
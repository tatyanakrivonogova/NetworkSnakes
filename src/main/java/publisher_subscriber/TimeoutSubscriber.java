package publisher_subscriber;

import java.net.InetAddress;

public interface TimeoutSubscriber {
    void updateTimeout(InetAddress ip, int port);
}
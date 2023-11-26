package publisher_subscriber;

import java.net.InetAddress;

public interface TimeoutSubscriber {
    void update(InetAddress ip, int port);
}
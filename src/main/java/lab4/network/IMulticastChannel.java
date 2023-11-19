package lab4.network;

import java.io.IOException;
import java.net.InetAddress;

public interface IMulticastChannel {
    void broadcast(byte[] data) throws IOException;

    void close();

    InetAddress getLocalAddress();

    int getLocalPort();
}
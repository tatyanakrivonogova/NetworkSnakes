package lab4.network;

import lab4.messages.RawMessage;

import java.io.IOException;
import java.net.InetAddress;

public interface IDatagramChannel {
    void setSocketTimeout(int timeout);
    void send(byte[] data, InetAddress receiverAddress, int receiverPort) throws IOException;
    RawMessage receive() throws IOException;
    void close();

}
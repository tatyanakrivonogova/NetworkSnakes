package lab4.network;

import lab4.messages.ReceivedMessage;
import publisher_subscriber.Publisher;
import publisher_subscriber.Subscriber;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

public class MulticastSocketWrapper implements IMulticastChannel, Publisher {
    private final MulticastSocket multicastSocket;
    private final ArrayList<Subscriber> subscribers;
    private final InetAddress multicastIp;
    private final int multicastPort;

//    private int DATAGRAM_PACKET_LEN = 4096;

    public MulticastSocketWrapper(InetAddress multicastIp, int multicastPort) throws IOException {
        this.multicastIp = multicastIp;
        this.multicastPort = multicastPort;
        multicastSocket = new MulticastSocket();
        multicastSocket.joinGroup(multicastIp);
        subscribers = new ArrayList<>();
    }

    @Override
    public void broadcast(byte[] data) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, multicastIp, multicastPort);
        multicastSocket.send(sendPacket);
    }


    @Override
    public void close() {
        multicastSocket.close();
    }

    @Override
    public void notifySubscribers(ReceivedMessage message) {
        subscribers.forEach(subscriber -> subscriber.update(message));
    }

    @Override
    public void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public InetAddress getLocalAddress() {
        return multicastSocket.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return multicastSocket.getLocalPort();
    }
}
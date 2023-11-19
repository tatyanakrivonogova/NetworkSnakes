package lab4.network;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import lab4.messages.ReceivedMessage;
import lab4.proto.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;

public class MulticastReceiver {
    private final int DATAGRAM_PACKET_LENGTH = 4096;
    private final InetAddress multicastAddress;
    private final int multicastPort;
    private final MulticastSocket socket;

    public MulticastReceiver(InetAddress multicastAddress, int multicastPort) throws IOException {

        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        socket = new MulticastSocket(multicastPort);
        socket.joinGroup(multicastAddress);
    }

    public Flowable<ReceivedMessage> getMulticastFlowable() {
        return Flowable.create(emitter -> {
            while (!socket.isClosed()) {
                byte[] data = new byte[DATAGRAM_PACKET_LENGTH];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                ByteBuffer byteBuffer = ByteBuffer.allocate(packet.getLength());
                byteBuffer.put(packet.getData(), 0, packet.getLength());
                SnakesProto.GameMessage message = SnakesProto.GameMessage.parseFrom(byteBuffer.array());
                if (message != null) {
                    emitter.onNext(new ReceivedMessage(message, packet.getAddress(), packet.getPort()));
                }
            }
        }, BackpressureStrategy.BUFFER);
    }

    public void shutdown() {
        try {
            socket.leaveGroup(multicastAddress);
        } catch (IOException ignored) {
        }
        socket.close();
    }
}
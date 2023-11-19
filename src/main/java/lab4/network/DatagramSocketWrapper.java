package lab4.network;

import lab4.messages.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public class DatagramSocketWrapper implements IDatagramChannel {
    private final static int DATAGRAM_PACKET_LENGTH = 4096;
    private final DatagramSocket socket;
    private final Logger logger = LoggerFactory.getLogger(DatagramSocketWrapper.class);

    public DatagramSocketWrapper(int timeout) throws SocketException {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            logger.error("DatagramSocketWrapper constructor:" + e);
            throw e;
        }
    }

    @Override
    public void send(byte[] data, InetAddress receiverAddress, int receiverPort) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, receiverAddress, receiverPort);
        socket.send(sendPacket);
    }

    @Override
    public RawMessage receive() throws IOException {
        byte[] data = new byte[DATAGRAM_PACKET_LENGTH];
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        try {
            socket.receive(receivePacket);
        } catch (SocketTimeoutException e) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(receivePacket.getLength());
        buffer.put(receivePacket.getData(), 0, receivePacket.getLength());
        return new RawMessage(buffer.array(), receivePacket.getAddress(), receivePacket.getPort());
    }

    @Override
    public void close() {
        socket.close();
    }
}
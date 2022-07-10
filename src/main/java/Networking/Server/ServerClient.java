package Networking.Server;

import Networking.Packet.ClientPacket;
import Networking.Packet.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;

public class ServerClient {

    private final String id;

    private final Server server;
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;

    private final byte[] receiveBuffer;
    private final Packet receiveData;

    private final Thread update;

    private boolean connected;

    public ServerClient(String id, Socket socket, Server server) throws IOException {
        this.id = id;
        this.socket = socket;
        this.server = server;

        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());

        receiveBuffer = new byte[4096];
        receiveData = new Packet();

        update = new Thread(() -> {
            while (connected) {
                update();
            }
        });

        connected = true;
        update.start();
    }

    public void disconnect(boolean sendPacket) throws IOException {
        if (sendPacket) ServerSend.forceDisconnect(this);

        update.stop();

        input.close();
        output.close();
        socket.close();

        connected = false;
    }

    private void update() {
        try {
            input.read(receiveBuffer, 0, receiveBuffer.length);
            receiveData.reset(handlePacket(receiveBuffer));
        } catch (Exception e) {
            if (socket.isClosed()) return;
            e.printStackTrace();
        }
    }

    public void send(Packet packet) throws IOException {
        packet.writeLength();
        output.write(packet.toArray(), 0, packet.length());
        output.flush();
    }

    private boolean handlePacket(byte[] data) throws IOException {
        int packetLength = 0;

        receiveData.setBytes(data);
        if (receiveData.unreadLength() >= 4) {
            packetLength = receiveData.readInt();
            if (packetLength <= 0) {
                return true;
            }
        }

        while (packetLength > 0 && packetLength <= receiveData.unreadLength()) {
            byte[] packetBytes = receiveData.readBytes(packetLength);

            Packet newPacket = new Packet(packetBytes);
            int packetID = newPacket.readInt();
            handlePacketCallback(newPacket, packetID);

            packetLength = 0;
            if (receiveData.unreadLength() >= 4) {
                packetLength = receiveData.readInt();
                if (packetLength <= 0) return true;
            }
        }

        return packetLength <= 1;
    }

    private void handlePacketCallback(Packet packet, int id) throws IOException {
        if (packet.isType(ClientPacket.Disconnect, id)) {
            server.removeClient(this.id);
            disconnect(false);
        }
    }

    public String getIp() {
        return socket.getRemoteSocketAddress().toString();
    }

    public String getId() {
        return id;
    }

    public Socket getSocket() {
        return socket;
    }

}

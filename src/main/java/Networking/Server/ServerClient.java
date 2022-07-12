package Networking.Server;

import Networking.DisconnectReason;
import Networking.Packet.ClientPacket;
import Networking.Packet.Packet;
import Networking.TransferProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ServerClient {

    private final String id;

    private final Server server;
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final DatagramSocket udpSocket;

    private final InetAddress address;

    private final byte[] receiveBuffer;
    private final Packet receiveData;

    private final Thread update;

    private boolean connected;
    private String name;

    public boolean initializedUdp = false;
    public int udpPort;

    public ServerClient(String id, Socket socket, Server server) throws Exception {
        this.id = id;
        this.socket = socket;
        this.server = server;
        this.address = socket.getInetAddress();

        name = "Client" + hashCode();

        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
        udpSocket = new DatagramSocket();

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

    public void disconnect() throws Exception {
        disconnect(true, DisconnectReason.Unspecified);
    }

    public void disconnect(DisconnectReason reason) throws Exception {
        disconnect(true, reason);
    }

    public void disconnect(boolean sendPacket, DisconnectReason reason) throws Exception {
        ServerSend.clientDisconnect(server, this);

        server.call(c -> c.onClientDisconnect(this, reason));
        if (sendPacket) ServerSend.forceDisconnect(this, reason);

        update.stop();

        input.close();
        output.close();
        socket.close();

        connected = false;
        server.removeClient(id);
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

    public void send(Packet packet, TransferProtocol protocol) throws Exception {
        if (protocol == TransferProtocol.TCP) {
            sendTcp(packet);
        } else {
            sendUdp(packet);
        }
    }

    public void sendTcp(Packet packet) throws Exception {
        packet.writeLength();
        output.write(packet.toArray(), 0, packet.length());
        output.flush();
    }

    public void sendUdp(Packet packet) throws Exception {
        if (!initializedUdp) return;

        packet.writeLength();
        DatagramPacket dp = new DatagramPacket(packet.toArray(), packet.length(), address, udpPort);
        udpSocket.send(dp);
    }

    private boolean handlePacket(byte[] data) throws Exception {
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

    private void handlePacketCallback(Packet packet, int id) throws Exception {
        if (packet.isType(ClientPacket.Disconnect, id)) {
            server.removeClient(this.id);
            disconnect(false, DisconnectReason.ClientDisconnect);
        } else if (packet.isType(ClientPacket.ChangeName, id)) {
            name = packet.readString();
        } else if (packet.isType(ClientPacket.NetworkSync, id)) {
            String user = packet.readString();
            String property = packet.readString();
            Object data = packet.readObject();
            ServerSend.networkSync(server, user, property,data, TransferProtocol.TCP);
        } else {
            server.call((c) -> c.onPacket(this, packet, id, TransferProtocol.TCP));
        }
    }

    public String getIp() {
        return socket.getRemoteSocketAddress().toString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Socket getSocket() {
        return socket;
    }

}

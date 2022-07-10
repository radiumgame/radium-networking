package Networking.Server;

import Networking.Callbacks.ServerCallback;
import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.TransferProtocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;

public class Server {

    private ServerSocket tcpSocket;
    private DatagramSocket udpSocket;
    private final HashMap<String, ServerClient> clients = new HashMap<>();

    private final int MAX_CLIENTS;

    private final int port;
    private boolean open;

    private Thread acceptThread;
    private Thread udpReceive;

    private byte[] udpReceiveBuffer;
    private Packet udpReceivePacket;

    private final List<ServerCallback> callbacks = new ArrayList<>();

    public Server(int port, int maxClients) throws Exception {
        this.port = port;
        this.MAX_CLIENTS = maxClients;

        create();
    }

    private void create() throws Exception {
        tcpSocket = new ServerSocket(port);
        udpSocket = new DatagramSocket(port);

        acceptThread = new Thread(() -> {
            try {
                while (open) {
                    acceptClients();
                }
            } catch (Exception e) {
                if (open) e.printStackTrace();
            }
        }, "SERVER-ACCEPT-" + hashCode());
        udpReceive = new Thread(() -> {
            try {
                while (open) {
                    receiveUdp();
                }
            } catch (Exception e) {
                if (open) e.printStackTrace();
            }
        }, "SERVER-ACCEPT-" + hashCode());

        udpReceiveBuffer = new byte[65535];
        udpReceivePacket = new Packet();

        open = true;
        acceptThread.start();
        udpReceive.start();
    }

    public void close() throws Exception {
        for (ServerClient client : getClients()) {
            client.disconnect(true, DisconnectReason.ServerClose);
        }

        open = false;
        acceptThread.stop();
        udpReceive.stop();
        tcpSocket.close();
        udpSocket.close();
    }

    public void registerCallback(ServerCallback callback) {
        callbacks.add(callback);
    }

    private void acceptClients() throws Exception {
        Socket newClient = tcpSocket.accept();

        String id = generateId();
        ServerClient client = new ServerClient(id, newClient, this);
        if (clients.size() >= MAX_CLIENTS) {
            client.disconnect(true, DisconnectReason.ServerFull);
            return;
        }

        ServerSend.assignData(client);
        clients.put(id, client);

        call((c) -> c.onClientConnect(client));
    }

    private void receiveUdp() throws Exception {
        DatagramPacket receive = new DatagramPacket(udpReceiveBuffer, udpReceiveBuffer.length);
        udpSocket.receive(receive);
        byte[] data = receive.getData();
        udpReceivePacket.reset(handlePacket(data));
    }

    private boolean handlePacket(byte[] data) throws Exception {
        int packetLength = 0;

        udpReceivePacket.setBytes(data);
        if (udpReceivePacket.unreadLength() >= 4) {
            packetLength = udpReceivePacket.readInt();
            if (packetLength <= 0) {
                return true;
            }
        }

        while (packetLength > 0 && packetLength <= udpReceivePacket.unreadLength()) {
            byte[] packetBytes = udpReceivePacket.readBytes(packetLength);

            Packet newPacket = new Packet(packetBytes);
            String clientId = newPacket.readString();
            int packetID = newPacket.readInt();
            handlePacketCallback(newPacket, packetID, clientId);

            packetLength = 0;
            if (udpReceivePacket.unreadLength() >= 4) {
                packetLength = udpReceivePacket.readInt();
                if (packetLength <= 0) return true;
            }
        }

        return packetLength <= 1;
    }

    private void handlePacketCallback(Packet packet, int packetID, String userId) throws Exception {
        ServerClient client = clients.get(userId);
        if (client == null) return;

        call((c) -> c.onPacket(client, packet, packetID, TransferProtocol.UDP));
    }

    private String generateId() {
        String id = UUID.randomUUID().toString();
        while (clients.containsKey(id)) {
            id = UUID.randomUUID().toString();
        }

        return id;
    }

    public void call(Consumer<ServerCallback> callback) {
        for (ServerCallback c : callbacks) {
            callback.accept(c);
        }
    }

    public Collection<ServerClient> getClients() {
        return clients.values();
    }

    public void removeClient(String id) {
        clients.remove(id);
    }

    public int getMaxClients() {
        return MAX_CLIENTS;
    }

    public int getPort() {
        return port;
    }

    public boolean isOpen() {
        return open;
    }

}

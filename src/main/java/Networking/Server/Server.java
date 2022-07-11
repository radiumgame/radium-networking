package Networking.Server;

import Networking.Callbacks.ServerCallback;
import Networking.DisconnectReason;
import Networking.Packet.ClientPacket;
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

        ServerSend.assignData(this, client);
        clients.put(id, client);
        ServerSend.newClient(this, client);
        call((c) -> c.onClientConnect(client));
    }

    public void sendToAll(Packet packet, TransferProtocol protocol) throws Exception {
        for (ServerClient client : getClients()) {
            client.send(packet, protocol);
        }
    }

    public void sendToAll(Packet packet, TransferProtocol protocol, String except) throws Exception {
        for (ServerClient client : getClients()) {
            if (client.getId().equals(except)) continue;
            client.send(packet, protocol);
        }
    }

    private void receiveUdp() throws Exception {
        DatagramPacket receive = new DatagramPacket(udpReceiveBuffer, udpReceiveBuffer.length);
        udpSocket.receive(receive);
        byte[] data = receive.getData();
        udpReceivePacket.reset(handlePacket(data, receive));
    }

    private boolean handlePacket(byte[] data, DatagramPacket dgp) throws Exception {
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
            handlePacketCallback(newPacket, packetID, clientId, dgp);

            packetLength = 0;
            if (udpReceivePacket.unreadLength() >= 4) {
                packetLength = udpReceivePacket.readInt();
                if (packetLength <= 0) return true;
            }
        }

        return packetLength <= 1;
    }

    private void handlePacketCallback(Packet packet, int packetID, String userId, DatagramPacket dgp) throws Exception {
        ServerClient client = clients.get(userId);
        if (client == null) return;

        if (packet.isType(ClientPacket.UdpInitialize, packetID)) {
            client.initializedUdp = true;
            client.udpPort = dgp.getPort();
        } else {
            call((c) -> c.onPacket(client, packet, packetID, TransferProtocol.UDP));
        }
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

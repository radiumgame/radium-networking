package Networking.Client;

import Networking.Callbacks.ClientCallback;
import Networking.DisconnectReason;
import Networking.Packet.ClientPacket;
import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;
import Networking.Ping.PingStatus;
import Networking.Ping.Ping;
import Networking.Sync.NetworkSync;
import Networking.TransferProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Client {

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private DatagramSocket udpSocket;

    private final String server;
    private final int port;
    private InetAddress address;
    private String id;
    private String name;

    private float ping;
    private PingStatus pingStatus = PingStatus.Disconnected;

    private boolean connected;
    private boolean initialized;

    private Thread update;
    private Thread udpReceive;
    private Thread pingThread;

    private byte[] receiveBuffer;
    private Packet receiveData;

    private byte[] udpReceiveBuffer;

    private final List<ClientCallback> callbacks = new ArrayList<>();
    private final List<NetworkSync> syncs = new ArrayList<>();

    public Client(String server, int port) {
        this.server = server;
        this.port = port;

        try {
            this.address = InetAddress.getByName(server);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() throws Exception {
        if (connected) return;

        socket = new Socket(server, port);
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
        udpSocket = new DatagramSocket();

        receiveBuffer = new byte[4096];
        receiveData = new Packet();

        udpReceiveBuffer = new byte[65535];

        update = new Thread(() -> {
            try {
                while (connected) {
                    update();
                }
            } catch (Exception e) {
                if (connected) e.printStackTrace();
            }
        });
        udpReceive = new Thread(() -> {
            try {
                while (connected) {
                    receiveUdp();
                }
            } catch (Exception e) {
                if (connected) e.printStackTrace();
            }
        });
        pingThread = new Thread(() -> {
            try {
                while (connected) {
                    ping();
                }
            } catch (Exception e) {
                if (connected) e.printStackTrace();
            }
        });

        connected = true;
        update.start();
        udpReceive.start();
        pingThread.start();
    }

    public void disconnect() throws Exception {
        disconnect(true, DisconnectReason.ClientDisconnect);
    }

    private void disconnect(boolean sendPacket, DisconnectReason reason) throws Exception {
        call((callback) -> callback.onDisconnect(reason));

        if (sendPacket) ClientSend.disconnect(this);
        connected = false;
        initialized = false;
        ping = 0;
        pingStatus = PingStatus.Disconnected;

        update.stop();
        udpReceive.stop();
        pingThread.stop();

        input.close();
        output.close();
        socket.close();
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
        packet.writeId(id);
        packet.writeLength();
        DatagramPacket dp = new DatagramPacket(packet.toArray(), packet.length(), address, port);
        udpSocket.send(dp);
    }

    public void updateSyncProperty(String property, Object data, TransferProtocol protocol) throws Exception {
        ClientSend.networkSync(this, property, data, protocol);
    }

    public void registerCallback(ClientCallback callback) {
        callbacks.add(callback);
    }

    public void registerSync(NetworkSync sync) {
        syncs.add(sync);
    }

    private void update() throws Exception {
        input.read(receiveBuffer, 0, receiveBuffer.length);
        receiveData.reset(handlePacket(receiveBuffer, TransferProtocol.TCP));
    }

    private void receiveUdp() throws Exception {
        DatagramPacket receive = new DatagramPacket(udpReceiveBuffer, udpReceiveBuffer.length);
        udpSocket.receive(receive);
        byte[] data = receive.getData();
        receiveData.reset(handlePacket(data, TransferProtocol.UDP));
    }

    private boolean handlePacket(byte[] data, TransferProtocol protocol) throws Exception {
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
            handlePacketCallback(newPacket, packetID, protocol);

            packetLength = 0;
            if (receiveData.unreadLength() >= 4) {
                packetLength = receiveData.readInt();
                if (packetLength <= 0) return true;
            }
        }

        return packetLength <= 1;
    }

    private void handlePacketCallback(Packet packet, int id, TransferProtocol protocol) throws Exception {
        if (packet.isType(ServerPacket.ForceDisconnect, id)) {
            DisconnectReason reason = (DisconnectReason)packet.readObject();
            disconnect(false, reason);
        } else if (packet.isType(ServerPacket.AssignData, id)) {
            ClientHandle.assignData(this, packet);
            int currentClients = packet.readInt();
            List<String> clientIDs = new ArrayList<>();
            for (int i = 0; i < currentClients; i++) {
                clientIDs.add(packet.readString());
            }

            Packet init = new Packet(ClientPacket.UdpInitialize);
            sendUdp(init);

            initialized = true;
            call((c) -> c.onConnect(clientIDs));
        } else if (packet.isType(ServerPacket.NewClient, id)) {
            call((callback) -> callback.onNewClient(packet.readString()));
        } else if (packet.isType(ServerPacket.ClientDisconnect, id)) {
            call((callback) -> callback.onClientDisconnect(packet.readString()));
        } else if (packet.isType(ServerPacket.NetworkSync, id)) {
            String clientId = packet.readString();
            String name = packet.readString();
            Object data = packet.readObject();
            callSync((ns) -> {
                ns.updateProperty(clientId, name, data);
                ns.getCallback().onReceive(clientId, name, data);
            });
        } else {
            call((callback) -> callback.onPacket(packet, id, protocol));
        }
    }

    private void ping() throws Exception {
        ping = Ping.ping(server);
        pingStatus = Ping.getConnectionStatus(ping);

        if (pingStatus == PingStatus.TimedOut) {
            disconnect(true, DisconnectReason.TimedOut);
            return;
        }

        float wait = 500 - ping;
        if (wait > 0) {
            Thread.sleep((long)wait);
        }
    }

    private void call(Consumer<ClientCallback> callback) {
        callbacks.forEach(callback);
    }

    private void callSync(Consumer<NetworkSync> callback) {
        syncs.forEach(callback);
    }

    public void setName(String name) throws Exception {
        setName(name, true);
    }

    public void setName(String name, boolean sendPacket) throws Exception {
        if (!connected) return;

        for (char c : name.toCharArray()) {
            if (Character.isSpaceChar(c) || Character.isWhitespace(c)) {
                System.err.println("Name may not contain spaces or whitespace.");
                return;
            }
        }

        if (sendPacket) ClientSend.changeName(this, name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return server;
    }

    public int getPort() {
        return port;
    }

    public float getPing() {
        return ping;
    }

    public PingStatus getPingStatus() {
        return pingStatus;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Socket getSocket() {
        return socket;
    }

}

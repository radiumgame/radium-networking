package Networking.Client;

import Networking.Callbacks.ClientCallback;
import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Client {

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    private final String server;
    private final int port;
    private String id;
    private String name;

    private boolean connected;

    private Thread update;

    private byte[] receiveBuffer;
    private Packet receiveData;

    private final List<ClientCallback> callbacks = new ArrayList<>();

    public Client(String server, int port) {
        this.server = server;
        this.port = port;
    }

    public void connect() throws Exception {
        if (connected) return;

        socket = new Socket(server, port);
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());

        receiveBuffer = new byte[4096];
        receiveData = new Packet();

        update = new Thread(() -> {
            try {
                while (connected) {
                    update();
                }
            } catch (Exception e) {
                if (connected) e.printStackTrace();
            }
        });

        connected = true;
        update.start();

        call(ClientCallback::onConnect);
    }

    public void disconnect() throws Exception {
        disconnect(true, DisconnectReason.ClientDisconnect);
    }

    private void disconnect(boolean sendPacket, DisconnectReason reason) throws Exception {
        call((callback) -> callback.onDisconnect(reason));

        if (sendPacket) ClientSend.disconnect(this);
        connected = false;
        update.stop();

        input.close();
        output.close();
        socket.close();
    }

    public void send(Packet packet) throws Exception {
        packet.writeLength();
        output.write(packet.toArray(), 0, packet.length());
        output.flush();
    }

    public void registerCallback(ClientCallback callback) {
        callbacks.add(callback);
    }

    private void update() throws Exception {
        input.read(receiveBuffer, 0, receiveBuffer.length);
        receiveData.reset(handlePacket(receiveBuffer));
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
        if (packet.isType(ServerPacket.ForceDisconnect, id)) {
            DisconnectReason reason = (DisconnectReason)packet.readObject();
            disconnect(false, reason);
        } else if (packet.isType(ServerPacket.AssignData, id)) {
            ClientHandle.assignData(this, packet);
        } else {
            call((callback) -> callback.onPacket(packet, id));
        }
    }

    private void call(Consumer<ClientCallback> callback) {
        callbacks.forEach(callback);
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

    public String getHost() {
        return server;
    }

    public int getPort() {
        return port;
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

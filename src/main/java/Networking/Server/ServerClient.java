package Networking.Server;

import Networking.Packet.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;

public class ServerClient {

    private String id;

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    public ServerClient(String id, Socket socket) throws IOException {
        this.id = id;
        this.socket = socket;

        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
    }

    public void send(Packet packet) throws IOException {
        packet.writeLength();
        output.write(packet.toArray(), 0, packet.length());
        output.flush();
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

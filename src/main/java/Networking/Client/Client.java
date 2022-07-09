package Networking.Client;

import Networking.Packet.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;

public class Client {

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    private String server;
    private int port;
    private String id;

    private boolean connected;

    private Thread update;

    private byte[] receiveBuffer;
    private Packet receiveData;

    public Client(String server, int port) {
        this.server = server;
        this.port = port;
    }

    public void connect() throws IOException {
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        connected = true;
        update.start();
    }

    public void disconnect() throws IOException {
        connected = false;
        update.stop();

        input.close();
        output.close();
        socket.close();
    }

    private void update() throws IOException {
        input.read(receiveBuffer, 0, receiveBuffer.length);
        int byteLength = receiveBuffer.length;

        receiveData.reset(handlePacket(receiveBuffer));
    }

    private boolean handlePacket(byte[] data) {
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
            if (packetID == 1) {
                System.out.println(newPacket.readString());
            }

            packetLength = 0;
            if (receiveData.unreadLength() >= 4) {
                packetLength = receiveData.readInt();
                if (packetLength <= 0) return true;
            }
        }

        if (packetLength <= 1) return true;

        return false;
    }

    public String getHost() {
        return server;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public Socket getSocket() {
        return socket;
    }

}

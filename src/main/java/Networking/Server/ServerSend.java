package Networking.Server;

import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;

import java.io.IOException;

public class ServerSend {

    protected ServerSend() {}

    public static void forceDisconnect(ServerClient client) throws IOException {
        Packet packet = new Packet(ServerPacket.Close);
        client.send(packet);
    }

    public static void assignId(ServerClient client) throws IOException {
        Packet packet = new Packet(ServerPacket.AssignID);
        packet.write(client.getId());
        client.send(packet);
    }

}

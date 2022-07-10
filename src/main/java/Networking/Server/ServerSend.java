package Networking.Server;

import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;

public class ServerSend {

    protected ServerSend() {}

    public static void forceDisconnect(ServerClient client, DisconnectReason reason) throws Exception {
        Packet packet = new Packet(ServerPacket.ForceDisconnect);
        packet.write(reason);
        client.send(packet);
    }

    public static void assignData(ServerClient client) throws Exception {
        Packet packet = new Packet(ServerPacket.AssignData);
        packet.write(client.getId());
        packet.write(client.getName());
        client.send(packet);
    }

}

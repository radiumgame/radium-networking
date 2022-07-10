package Networking.Client;

import Networking.Packet.ClientPacket;
import Networking.Packet.Packet;

public class ClientSend {

    protected ClientSend() {}

    public static void disconnect(Client client) throws Exception {
        Packet packet = new Packet(ClientPacket.Disconnect);
        client.send(packet);
    }

}

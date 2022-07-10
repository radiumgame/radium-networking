package Networking.Client;

import Networking.Packet.ClientPacket;
import Networking.Packet.Packet;

import java.io.IOException;

public class ClientSend {

    protected ClientSend() {}

    public static void disconnect(Client client) throws IOException {
        Packet packet = new Packet(ClientPacket.Disconnect);
        client.send(packet);
    }

}

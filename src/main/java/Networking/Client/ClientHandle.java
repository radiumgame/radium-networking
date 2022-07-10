package Networking.Client;

import Networking.Packet.Packet;

public class ClientHandle {

    protected ClientHandle() {}

    public static void receiveId(Client client, Packet packet) {
        String id = packet.readString();
        client.setId(id);
    }

}

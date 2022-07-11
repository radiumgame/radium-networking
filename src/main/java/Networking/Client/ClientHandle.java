package Networking.Client;

import Networking.Packet.Packet;

public class ClientHandle {

    protected ClientHandle() {}

    public static void assignData(Client client, Packet packet) throws Exception {
        String id = packet.readString();
        String name = packet.readString();

        client.setId(id);
        client.setName(name, false);
    }

}

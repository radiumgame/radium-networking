package Networking.Client;

import Networking.Packet.Packet;

import java.io.IOException;

public class ClientHandle {

    protected ClientHandle() {}

    public static void receiveId(Client client, Packet packet) {
        String id = packet.readString();
        client.setId(id);
    }

}

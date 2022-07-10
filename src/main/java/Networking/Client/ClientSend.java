package Networking.Client;

import Networking.Packet.ClientPacket;
import Networking.Packet.Packet;
import Networking.TransferProtocol;

public class ClientSend {

    protected ClientSend() {}

    public static void disconnect(Client client) throws Exception {
        Packet packet = new Packet(ClientPacket.Disconnect);
        client.send(packet, TransferProtocol.TCP);
    }

    public static void changeName(Client client, String name) throws Exception {
        Packet packet = new Packet(ClientPacket.ChangeName);
        packet.write(name);
        client.send(packet, TransferProtocol.TCP);
    }

}

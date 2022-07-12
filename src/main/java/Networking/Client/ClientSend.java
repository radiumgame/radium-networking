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

    public static void networkSync(Client client, String property, Object data, TransferProtocol protocol) throws Exception {
        Packet packet = new Packet(ClientPacket.NetworkSync);
        packet.write(client.getId());
        packet.write(property);
        packet.write(data);
        client.send(packet, protocol);
    }

}

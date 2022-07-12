package Networking.Server;

import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;
import Networking.TransferProtocol;

import java.util.Collection;

public class ServerSend {

    protected ServerSend() {}

    public static void forceDisconnect(ServerClient client, DisconnectReason reason) throws Exception {
        Packet packet = new Packet(ServerPacket.ForceDisconnect);
        packet.write(reason);
        client.send(packet, TransferProtocol.TCP);
    }

    public static void assignData(Server server, ServerClient client) throws Exception {
        Packet packet = new Packet(ServerPacket.AssignData);
        packet.write(client.getId());
        packet.write(client.getName());

        Collection<ServerClient> clients = server.getClients();
        packet.write(clients.size());
        for (ServerClient c : clients) {
            packet.write(c.getId());
        }

        client.send(packet, TransferProtocol.TCP);
    }

    public static void newClient(Server server, ServerClient client) throws Exception {
        Packet packet = new Packet(ServerPacket.NewClient);
        packet.write(client.getId());
        server.sendToAll(packet, TransferProtocol.TCP, client.getId());
    }

    public static void clientDisconnect(Server server, ServerClient client) throws Exception {
        Packet packet = new Packet(ServerPacket.ClientDisconnect);
        packet.write(client.getId());
        server.sendToAll(packet, TransferProtocol.TCP, client.getId());
    }

    public static void networkSync(Server server, String client, String property, Object data, TransferProtocol protocol) throws Exception {
        Packet packet = new Packet(ServerPacket.NetworkSync);
        packet.write(client);
        packet.write(property);
        packet.write(data);
        server.sendToAll(packet, protocol, client);
    }

}

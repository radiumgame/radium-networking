package Networking.Callbacks;

import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Server.ServerClient;

public interface ServerCallback {

    void onClientConnect(ServerClient client);
    void onClientDisconnect(ServerClient client, DisconnectReason reason);
    void onPacket(ServerClient client, Packet packet, int type);

}
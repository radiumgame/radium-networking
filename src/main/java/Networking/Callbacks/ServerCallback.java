package Networking.Callbacks;

import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Server.ServerClient;
import Networking.TransferProtocol;

public abstract class ServerCallback {

    public void onClientConnect(ServerClient client) {}
    public void onClientDisconnect(ServerClient client, DisconnectReason reason) {}
    public void onPacket(ServerClient client, Packet packet, int type, TransferProtocol protocol) {}

}

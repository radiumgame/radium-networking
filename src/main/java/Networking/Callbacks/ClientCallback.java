package Networking.Callbacks;

import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;
import Networking.TransferProtocol;

public abstract class ClientCallback {

    public void onConnect() {}
    public void onDisconnect(DisconnectReason reason) {}
    public void onPacket(Packet packet, int type, TransferProtocol protocol) {}
    public void onNewClient(String clientId) {}
    public void onClientDisconnect(String clientId) {}

}

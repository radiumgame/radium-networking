package Networking.Callbacks;

import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;
import Networking.TransferProtocol;

import java.util.List;

public abstract class ClientCallback {

    public void onConnect(List<String> clients) {}
    public void onDisconnect(DisconnectReason reason) {}
    public void onPacket(Packet packet, int type, TransferProtocol protocol) {}
    public void onNewClient(String clientId) {}
    public void onClientDisconnect(String clientId) {}

}

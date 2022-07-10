package Networking.Callbacks;

import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;
import Networking.TransferProtocol;

public interface ClientCallback {

    void onConnect();
    void onDisconnect(DisconnectReason reason);
    void onPacket(Packet packet, int type, TransferProtocol protocol);

}

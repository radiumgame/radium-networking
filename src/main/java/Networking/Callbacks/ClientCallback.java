package Networking.Callbacks;

import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;

public interface ClientCallback {

    void onConnect();
    void onDisconnect(DisconnectReason reason);
    void onPacket(Packet packet, int type);

}

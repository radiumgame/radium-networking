import Networking.Client.Client;
import Networking.Server.Server;
import Networking.Server.ServerClient;

import java.util.Scanner;

public class Main {

    private static Server server;

    public static void main(String[] args) {
        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void start() throws Exception {
        server = new Server(8080, 10);

        String line;
        System.out.print("CMD: ");
        Scanner scanner = new Scanner(System.in);
        while (!(line = scanner.next()).equals("exit")) {
            if (line.equals("new")) {
                Client client = new Client("127.0.0.1", server.getPort());
                client.connect();
            }
            if (line.equals("clients")) {
                for (ServerClient client : server.getClients()) {
                    System.out.println(client.getIp());
                }
            }

            System.out.print("CMD: ");
        }

        server.close();
    }

}

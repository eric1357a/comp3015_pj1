package COMP3015_Project_1.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Scanner;

class Server {

    public static void main(String[] args) {
        ServerSocket ss;
        try {
            Scanner sc = new Scanner(System.in);
            System.out.print("Port? ");
            int port = sc.nextInt();
            ss = new ServerSocket(port);
            System.out.println("Listening at: " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
            sc.close();
            while (true) {
                new AcceptedServerConnection(ss.accept()).start();
            }
        } catch (IOException e) {
            System.out.println("Problems on create socket");
            e.printStackTrace();
        }
    }
}

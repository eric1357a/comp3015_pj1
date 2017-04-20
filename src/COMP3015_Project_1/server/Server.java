package COMP3015_Project_1.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Scanner;

class Server {

    public static void main(String[] args) {
        ServerSocket ss;
        String check;
        int port = -1;
        try {
            Scanner sc = new Scanner(System.in);
            System.out.print("Port? (Default:1234) ");
            try {
                check = sc.nextLine();
                if(check.equals("")){
                    port = 1234;
                }
                port = Integer.parseInt(check);
            }catch(java.lang.NumberFormatException ex){
                System.out.println("Only number is allow!" +
                        "\nWill run on default port");
            }
            if(port == -1)
                port = 1234;

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

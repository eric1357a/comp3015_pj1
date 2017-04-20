package COMP3015_Project_1.client;

import COMP3015_Project_1.Common.DataChannelPool;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AcceptedClientConnection {
    private Socket s;
    private Scanner sc;

    private DataChannelPool dcPool = new DataChannelPool();

    private InputStream in;
    private OutputStream out;
    private BufferedReader br;
    private BufferedWriter bw;
    private DataInputStream din;
    private DataOutputStream dout;

    boolean closing = false;


    AcceptedClientConnection(Socket socket, Scanner scanner) throws IOException {
        s = socket;
        sc = scanner;
        br = new BufferedReader(new InputStreamReader(in = s.getInputStream()));
        bw = new BufferedWriter(new OutputStreamWriter(out = s.getOutputStream()));
        din = new DataInputStream(in);
        dout = new DataOutputStream(out);
    }

    public void work() throws InterruptedException {
        String password = "";
        if (System.console() == null) {
            System.out.print("Password: ");
            password = sc.nextLine();
        } else
            password = String.valueOf(System.console().readPassword("Password: "));
        try {
            bw.write("200 " + password + "\r\n");
            bw.flush();

            if (!br.readLine().equals("200")) {
                System.out.println("Incorrect password");
                return;
            } else {
                System.out.println("COMP3015 Project 1\n" +
                        "Login success");
            }
            while (!closing) {
                System.out.print("COMMAND>>> ");
                String cmd = "";
                String command = sc.nextLine();
                command = command.trim();
                // Command
                if (command.toLowerCase().equalsIgnoreCase("ls")) {
                    cmd = "100";
                } else if (command.toLowerCase().contains("cd")) {
                    cmd = "201";
                    if (command.length() > 3) {
                        cmd += " " + command.toLowerCase().substring(3, command.length());
                    }
                } else if (command.toLowerCase().contains("get")) {
                    cmd = "300" + " " + command.toLowerCase().substring(3, command.length());

                } else if (command.toLowerCase().equalsIgnoreCase("help")) {
                    cmd = "400";
                } else if (command.toLowerCase().equalsIgnoreCase("logout")) {
                    cmd = "500";
                    Thread.sleep(250);

                } else if (command.toLowerCase().equalsIgnoreCase("ping")) {
                    cmd = "600";
                } else {
                    cmd = "404";
                }
                bw.write(predict(cmd) + "\r\n");
                bw.flush();
                String rec = new String(receiveAndUnpack(din));
                System.out.println(rec);
                parseAndDoBackground(rec);
                if (rec.equalsIgnoreCase("Logged out from the server.")) {
                    closing = true;
                }
            }
            if (dcPool.size() > 0) {
                dcPool.getAll().stream().forEach((dc) -> {
                    try {
                        //noinspection AccessStaticViaInstance
                        if (Thread.currentThread().interrupted())
                            dc.interrupt();
                        else
                            dc.join();
                    } catch (InterruptedException e) {
                        System.out.println("Detected interrupted at \"%s\".");
                    }
                });
            }
        } catch (
                IOException e)

        {
            System.out.println("I/O Problem");
        }

    }

    private String predict(String content) {
        if (content.equals("logout"))
            closing = true;

        return content;
    }

    private static byte[] receiveAndUnpack(DataInputStream in) throws IOException {
        byte[] ret = new byte[in.readInt()];

        in.read(ret);

        return ret;
    }

    private void parseAndDoBackground(String rec) {
        {
            Pattern regex = Pattern.compile("(?<=Transferring: \\().+@[\\d\\.:]+@\\d+(?=\\))");
            Matcher regexMatcher = regex.matcher(rec);

            List<String[]> found = new LinkedList<>();
            while (regexMatcher.find()) {
                found.add(regexMatcher.group().split("@"));
            }

            if (found.size() > 0) for (String[] f : found) {
                final String fileName = f[0];
                InetAddress ip;
                try {
                    ip = InetAddress.getByName(f[1]);
                } catch (UnknownHostException ignored) {
                    continue;
                }
                int port = Integer.valueOf(f[2]);

                File Directory = new File("C:/" + "DownloadedFiles");
                if (!Directory.exists()) {
                    System.out.println("Directory not found, creating " + Directory.getName() + " into C:/");
                    try {
                        Directory.mkdir();
                    } catch (SecurityException se) {
                        se.printStackTrace();
                    }
                }

                Path writeDest = Paths.get(Directory.getAbsolutePath().toString(), fileName);
                if (writeDest.toFile().exists()) {
                    System.out.printf("Overwrite existing file \"%s\" ? (Y/N) ", fileName);
                    if (sc.nextLine().toLowerCase().equals("n"))
                        continue;
                }

                FileOutputStream fOut = null;
                try {
                    fOut = new FileOutputStream(writeDest.toAbsolutePath().toString());
                } catch (FileNotFoundException ignored) {
                }

                Socket s;
                try {
                    s = new Socket(ip, port);
                } catch (IOException e) {
                    System.out.println("Socket creation error.");
                    continue;
                }

                dcPool.putNew(fOut, s, r -> System.out.printf(" \"%s\" transferred %s.\r\n> ", fileName, r.getException() == null ? "success" : "failed"));
            }
        }
    }

}

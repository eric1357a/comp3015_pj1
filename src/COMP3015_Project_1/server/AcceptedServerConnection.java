package COMP3015_Project_1.server;

import COMP3015_Project_1.Common.CommandArguments;
import COMP3015_Project_1.Common.DataChannelInitializeServerSocketHouse;
import COMP3015_Project_1.Common.DataChannelPool;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

class AcceptedServerConnection extends Thread {
    private Socket s;

    private InputStream in;
    private OutputStream out;

    private DataInputStream din;
    private DataOutputStream dout;

    private BufferedReader br;
    private BufferedWriter bw;

    private File currDir;
    private File rootDir;
    private Map<String, File> currDirFiles;

    private DataChannelPool dtPool;

    private DataChannelInitializeServerSocketHouse serverSocketHouse;

    public AcceptedServerConnection(Socket s) {
        File theDir = new File("www");
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            System.out.println("creating directory: " + theDir.getName());
            try {
                theDir.mkdir();
            } catch (SecurityException se) {
                //handle it
            }
        }
        this.s = s;
        try {
            in = s.getInputStream();
            out = s.getOutputStream();

            din = new DataInputStream(in);
            dout = new DataOutputStream(out);

            br = new BufferedReader(new InputStreamReader(in));
            bw = new BufferedWriter(new OutputStreamWriter(out));
        } catch (IOException e) {
            System.out.println("Cannot create i/o streams");
        }


        //currDir = Paths.get(System.getProperty("user.dir")).toFile();
        currDir = theDir;
        rootDir = currDir;
        dtPool = new DataChannelPool();
        serverSocketHouse = new DataChannelInitializeServerSocketHouse();
    }

    @Override
    public void run() {
        System.out.printf("Established connection from (%s:%d)\r\n", s.getInetAddress(), s.getPort());

        try {
            String PSCmd = br.readLine();

            if (PSCmd.substring(0, 3).equalsIgnoreCase("200")) { // 200 = checking pw
                if (!"12345678".equals(PSCmd.substring(4, PSCmd.length()))) {// password
                    bw.write("Failed.\r\n");
                    //return;
                } else {
                    bw.write("200\r\n");
                }
            }

            bw.flush();
            currDirFiles = getFileList(currDir.toPath());
            boolean exiting = false;
            while (!exiting) {

                String raw = br.readLine();
                if (!raw.equals("500")) {
                    System.out.printf("Command %s from %s:%d\r\n", raw, s.getInetAddress().getHostAddress(), s.getPort());
                }else if(raw.equals("500")) System.out.printf(" %s:%d disconnected\r\n", s.getInetAddress().getHostAddress(), s.getPort());
                CommandArguments ca = CommandArguments.CreateWithSpliting(raw);
                if (ca.getCommand().equals("100")) { // 100 = ls
                    currDirFiles = getFileList(currDir.toPath());
                    String r = String.join("\r\n", currDirFiles.values().stream().map(AcceptedServerConnection::formatFileProperties).toArray(String[]::new));
                    packetAndSend(r.getBytes(), r.getBytes().length);
                } else if (ca.getCommand().equals("200")) { // 200 = cd
                    String arg = ca.get(0);
                    if (arg.equals("/")) {
                        currDir = rootDir;
                        currDirFiles = getFileList(currDir.toPath());
                    } else if (arg.equals("..") && !(currDir.toPath().equals(rootDir.toPath())) && currDir.exists()) {
                        currDir = currDir.getParentFile();
                        currDirFiles = getFileList(currDir.toPath());

                    } else if (arg.length() > 0 && !arg.equals(".")) {
                        File target = currDirFiles.get(arg);

                        try {
                            if (target.isDirectory()) {
                                currDir = target;
                                currDirFiles = getFileList(currDir.toPath());
                            }
                        } catch (Exception ex) {
                            currDir = rootDir;
                            currDirFiles = getFileList(currDir.toPath());
                        }
                    }

                    String vPath = currDir.getAbsolutePath().substring(rootDir.getAbsoluteFile().toString().length()).replace('\\', '/');
                    if (vPath.length() <= 0) vPath = "/";

                    packetAndSend("Changed directory to \"%s\"", vPath);
                } else if (ca.getCommand().equals("300")) { // 300 = get
                    currDirFiles = getFileList(currDir.toPath());
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < ca.length(); i++) {
                        String fn = ca.get(i);
                        File f = Paths.get(currDir.getAbsolutePath(), fn).toFile();
                        if (!f.exists() || !f.isFile()) {
                            sb.append("\r\n\"");
                            sb.append(fn);
                            sb.append("\" does not exist");
                        } else {
                            FileInputStream fs = new FileInputStream(f);

                            int portNumber = generateNewFreePortNumber();

                            InetAddress remoteAddress = s.getInetAddress();

                            sb.append(String.format("\r\nTransferring file: %s", f.getName()));

                            System.out.printf("File %s transferring to %s:%d \r\n", f.getName(), s.getInetAddress().getHostAddress(), s.getPort());
                            serverSocketHouse.putNewSingleServerSocket(
                                    portNumber,
                                    remoteAddress, s -> dtPool.putNew(fs, s,
                                            r -> System.out.printf("File %s transfer to %s:%d complete\r\n",
                                                    f.getName(),
                                                    s.getInetAddress().getHostAddress(),
                                                    s.getPort())
                                    )
                            );
                        }
                    }

                    String output = sb.toString();
                    if (output.length() > 4) output = output.substring(2);
                    packetAndSend(output);
                } else if (ca.getCommand().equals("500")) { // 500 = quit
                    packetAndSend("Logged out from the system.");
                    exiting = true;
                } else if (ca.getCommand().equals("400")) { // 400 = help
                    String content =
                            "ls              By using this ls function, the program will list out the contents of the current directory.\r\n" +
                                    "cd [dir]        If a directory was inputted, the program will change to\n" +
                                    "                corresponding directory.\r\n" +
                                    "cd  [/]         If [/] was inputted, the program will go back to the root\n" +
                                    "                folder.\r\n" +
                                    "cd  [..]        If [..] was inputted, the program will go back to the\n" +
                                    "                previous folder.\r\n" +
                                    "get [file]      By using this get function, the user will able to download the file..\r\n" +
                                    "help            By using this help function, all the available commands will show\n" +
                                    "                with the description.\r\n" +
                                    "logout          By using this logout function, the user will log out from the\n" +
                                    "system.\r\n" +
                                    "ping            By using this ping function, the user will able to check the time of the server.";
                    packetAndSend(content);
                } else if (ca.getCommand().equals("600")) { // 600 = ping
                    String content = "Server time = " + new java.util.Date();
                    packetAndSend(content);
                } else {
                    packetAndSend("Command Not Found"); // 404 / other = not found
                }
            }

            if (dtPool.size() > 0) {
                dtPool.getAll().stream().forEach((dc) -> {
                    try {
                        if (interrupted())
                            dc.interrupt();
                        else
                            dc.join();
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted on waiting data channel \"%s\" to be completed. Will kill all remainings.");
                    }
                });
            }
        } catch (IOException e) {
            System.out.printf("(%s:%d) IO error: %s\r\n", s.getInetAddress().getHostAddress(), s.getPort(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String formatFileProperties(File file) {

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd  yyyy", Locale.ENGLISH);
        return String.format("%s%s%s%s %4d %s %s",
                file.isDirectory() ? "d" : "-",
                (file.canRead() ? "r" : "-") + (file.canWrite() ? "w" : "-") + (file.canExecute() ? "x" : "-"),
                (file.canRead() ? "r" : "-") + "-" + (file.canExecute() ? "x" : "-"),
                (file.canRead() ? "r" : "-") + "-" + (file.canExecute() ? "x" : "-"),
                file.length(),
                sdf.format(file.lastModified()),
                file.getName()
        );
    }

    private static Map<String, File> getFileList(Path path) throws IOException {
        return Files.walk(
                Paths.get(path.toAbsolutePath().toString()), 1)
                .filter(e -> {
                    try {
                        return (Files.isRegularFile(e) || Files.isDirectory(e)) && !Files.isHidden(e);
                    } catch (IOException ex) {
                        System.out.printf("Error when traversing through a file: %s", e.toAbsolutePath());
                        return false;
                    }
                })
                .map(Path::toFile)
                .collect(Collectors.toMap(File::getName, e -> e));
    }

    private void packetAndSend(byte[] content, int len) throws IOException {
        dout.writeInt(len);
        out.write(content, 0, len);
        out.flush();
    }

    private void packetAndSend(String content) throws IOException {
        byte[] c = content.getBytes();
        packetAndSend(c, c.length);
    }

    private void packetAndSend(String format, Object... args) throws IOException {
        packetAndSend(String.format(format, args));
    }

    private int generateNewFreePortNumber() {
        int port;
        while (true) {
            int port1 = (int) (Math.random() * 60 + 195);
            int port2 = (int) (Math.random() * 256);
            port = port1 * 256 + port2;


            return port;
        }
    }
}

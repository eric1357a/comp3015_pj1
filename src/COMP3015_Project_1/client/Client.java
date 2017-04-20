package COMP3015_Project_1.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

class Client extends JFrame implements ActionListener {
    private static Scanner sc = new Scanner(System.in);
    private JTextField txtServerIP;
    private JTextField txtPort;
    JFrame frame = new JFrame();

    JButton btnLogin;

    public static void main(String[] args) {
        new Client();


    }

    private static void clean() {
        if (System.console() == null)
            System.out.println("----------");
        else {
            try {
                if (System.getProperty("os.name").contains("Windows"))
                    Runtime.getRuntime().exec("cls");
                else
                    Runtime.getRuntime().exec("clear");
            } catch (IOException ignored) {
            }
        }
    }

    public Client() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 120);
        setTitle("Client Login");
        getContentPane().setLayout(null);

        txtServerIP = new JTextField();
        txtServerIP.setFont(new Font("Tahoma", Font.PLAIN, 11));
        txtServerIP.setBounds(76, 11, 196, 20);
        getContentPane().add(txtServerIP);
        txtServerIP.setColumns(10);

        JLabel lblNewLabel = new JLabel("Server IP");
        lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        lblNewLabel.setBounds(10, 14, 56, 14);
        getContentPane().add(lblNewLabel);

        lblNewLabel = new JLabel("Server Port");
        lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        lblNewLabel.setBounds(10, 44, 56, 14);
        getContentPane().add(lblNewLabel);

        txtPort = new JTextField();
        txtPort.setFont(new Font("Tahoma", Font.PLAIN, 11));
        txtPort.setBounds(76, 42, 56, 20);
        getContentPane().add(txtPort);
        txtPort.setColumns(10);

        btnLogin = new JButton("Log In");
        btnLogin.setFont(new Font("Tahoma", Font.PLAIN, 11));
        btnLogin.setBounds(180, 42, 89, 23);
        btnLogin.addActionListener(this);
        getContentPane().add(btnLogin);


        setVisible(true);
    }

    public Client(String host, int port) throws InterruptedException {

        while (true) {
            Socket s;
            boolean check = true;

            AcceptedClientConnection accepted = null;
            try {
                s = new Socket(host, port);
                accepted = new AcceptedClientConnection(s, sc);
                accepted.work();
            } catch (IOException e) {
                System.out.println("Socket init failed, retry (Y/N)?");
                if (sc.nextLine().toLowerCase().equals("n")) {
                    System.exit(-1);
                } else {
                    check = false;
                }

            }
            if (check) {
                System.out.println("Logout ? (Y/N) ");
                if (sc.nextLine().toLowerCase().equals("y"))
                    System.exit(-1);
            }

            clean();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        JButton button = (JButton) e.getSource();
        if (button == btnLogin) {
            if (!(txtServerIP.getText().equals("")) && !(txtPort.getText().equals(""))) {
                try {
                    setVisible(false);
                    new Client(txtServerIP.getText(), Integer.parseInt(txtPort.getText()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Please fill in all the fields.",
                        "Error",
                        JOptionPane.WARNING_MESSAGE);
            }
        } else {
            System.out.println("Why you are here?");
        }
    }
}

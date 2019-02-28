package com.kickr;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter writerOut;
    private BufferedReader readerIn;

    public ChatClient(String serverName, int serverPort) {
        this.host = serverName;
        this.port = serverPort;
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient("localhost", 8818);
        client.start();
    }

    private void start() throws IOException {
        if (connect()) {
            System.err.println("connection OK");
            Scanner sc = new Scanner(System.in);
            String line;
            while (true) {
                line = sc.nextLine();
                if ("quit".equalsIgnoreCase(line)) {
                    sendMsg(line);
                    break;
                }
                sendMsg(line);
            }

        }
        else
            System.err.println("connection failed");

        writerOut.close();
        readerIn.close();
        socket.close();
    }


    private void sendMsg(String line) {
        writerOut.println(line);
        writerOut.flush();
    }

    private boolean connect() {
        try {
            this.socket = new Socket(host, port);
            writerOut = new PrintWriter(socket.getOutputStream());
            readerIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Client port is " + socket.getLocalPort());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}

package com.kickr;

import com.kickr.engine.ResponseListener;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private final String host;
    private final int port;
    private Socket socket;
    private OutputStream outputStream;

    public ChatClient(String serverName, int serverPort) {
        this.host = serverName;
        this.port = serverPort;
    }

    public static void main(String[] args) throws IOException {
        String serverHost = "localhost";
        int serverPort = 8818;
        ChatClient client = new ChatClient(serverHost, serverPort);
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
        outputStream.close();
        socket.close();
    }

    private boolean connect() {
        try {
            this.socket = new Socket(host, port);
            System.out.println("Client port is " + socket.getLocalPort());
            outputStream = socket.getOutputStream();
            Thread inputListener = new Thread(new ResponseListener(socket));
            inputListener.start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void sendMsg(String line) throws IOException {
        outputStream.write((line + System.lineSeparator()).getBytes());
        outputStream.flush();
    }
}

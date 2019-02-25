package com.kickr;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread {
    private final int serverPort;

    private ArrayList<ServerWorker> workersList = new ArrayList<>();

    public Server(int serverPort) {
        this.serverPort = serverPort;
    }

    public ArrayList<ServerWorker> getWorkersList() {
        return workersList;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while (true) {
                System.out.println("About to accept client connection.." + System.lineSeparator());
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted from " + clientSocket + System.lineSeparator());
                ServerWorker worker = new ServerWorker(this, clientSocket);
                workersList.add(worker);
                worker.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

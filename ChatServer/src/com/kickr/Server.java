package com.kickr;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

public class Server extends Thread {
    private final int serverPort;
    private int currentUsers;

    private HashSet<ServerWorker> workersList = new HashSet<>();
    private ArrayList<String> topicsList = new ArrayList<>();

    Server(int serverPort) {
        this.serverPort = serverPort;
    }

    HashSet<ServerWorker> getWorkersList() {
        return workersList;
    }

    ArrayList<String> getTopicsList() {
        return topicsList;
    }

    int getCurrentUsers() {
        return currentUsers;
    }

    void setCurrentUsers(int currentUsers) {
        this.currentUsers = currentUsers;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while (true) {
                System.out.println("About to accept client connection");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted from " + clientSocket);
                ServerWorker worker = new ServerWorker(this, clientSocket);
                workersList.add(worker);
                worker.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

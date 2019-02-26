package com.kickr;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread {
    private final int serverPort;
    private final String welcomeMsg = "Welcome to ChatServer!" + System.lineSeparator() + "Basic commands: " + System.lineSeparator()
            + "\t login <username> <password> \t(logging in)" + System.lineSeparator()
            + "\t msg <username> <message> \t(direct messages)" + System.lineSeparator()
            + "\t msg <#topicname> <message> \t(direct messages)" + System.lineSeparator()
            + "\t #topics \t\t\t(get the list of all topics)" + System.lineSeparator()
            + "\t #newtopic <topicname> \t\t(create a new topic, without #)" + System.lineSeparator()
            + "\t #join <topicname> \t\t(join to topic, with #)" + System.lineSeparator()
            + "\t #leave <topicname> \t\t(leave this topic, with #)" + System.lineSeparator()
            + "\t logout/quit" + System.lineSeparator() + System.lineSeparator();


    private ArrayList<ServerWorker> workersList = new ArrayList<>();
    private ArrayList<String> topicsList = new ArrayList<>();

    public Server(int serverPort) {
        this.serverPort = serverPort;
    }

    public ArrayList<ServerWorker> getWorkersList() {
        return workersList;
    }

    public ArrayList<String> getTopicsList() {
        return topicsList;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while (true) {
                System.out.println("About to accept client connection.." + System.lineSeparator());
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted from " + clientSocket + System.lineSeparator());
                showWelcomeMessage(clientSocket);
                ServerWorker worker = new ServerWorker(this, clientSocket);
                workersList.add(worker);
                worker.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showWelcomeMessage(Socket clientSocket) {
        try {
            clientSocket.getOutputStream().write(welcomeMsg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

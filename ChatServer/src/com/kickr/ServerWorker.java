package com.kickr;

import java.io.*;
import java.net.Socket;
import java.util.LinkedHashSet;

public class ServerWorker extends Thread {

    private final String welcomeMsg = "Welcome to ChatServer!" + System.lineSeparator() + "Basic commands: " + System.lineSeparator()
            + "\t login <username> <password> \t(logging in)" + System.lineSeparator()
            + "\t msg <username> <message> \t(direct messages)" + System.lineSeparator()
            + "\t msg <#topicname> <message> \t(direct messages)" + System.lineSeparator()
            + "\t #topics \t\t\t(get the list of all topics)" + System.lineSeparator()
            + "\t #newtopic <topicname> \t\t(create a new topic, without #)" + System.lineSeparator()
            + "\t #join <topicname> \t\t(join to topic, with #)" + System.lineSeparator()
            + "\t #leave <topicname> \t\t(leave this topic, with #)" + System.lineSeparator()
            + "\t logout" + System.lineSeparator()
            + "\t quit";

    private static final String MSG_LINE_PREFIX = "- ";
    private static final String SYS_LINE_PREFIX = "<=> ";
    private static final String LIST_LINE_PREFIX = " - ";

    private final Socket clientSocket;
    private final Server server;
    private InputStream inputStream;
    private OutputStream outputStream;

    private String login;
    private boolean isLoggedIn = false;
    private LinkedHashSet<String> userTopics = new LinkedHashSet<>();

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public String getLogin() {
        return login;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public boolean isMemberOfTopic(String topicName) {
        return userTopics.contains(topicName);
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException {
        inputStream = clientSocket.getInputStream();
        outputStream = clientSocket.getOutputStream();
        this.send(MsgTypes.NONE, welcomeMsg);

        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if (!"quit".equalsIgnoreCase(line))  {
                String[] tokens = line.split(" ");
                String cmd = tokens[0].trim().toLowerCase();
                switch (cmd) {
                    default:
                        if (this.isLoggedIn)
                            handleCommonMessage(line);
                        else
                            this.send(MsgTypes.SYSTEM, "ERROR! You need to log in first!");
                        break;
                    case "login":
                        handleLogin(tokens);
                        break;
                    case "msg":
                        if (this.isLoggedIn)
                            handleSpecialMessage(tokens);
                        else
                            this.send(MsgTypes.SYSTEM, "ERROR! You need to log in first!");
                        break;
                    case "#topics":
                        handleGetTopicsList();
                        break;
                    case "#newtopic":
                        if (this.isLoggedIn)
                            handleNewTopic(tokens);
                        else
                            this.send(MsgTypes.SYSTEM, "ERROR! You need to log in first!");
                        break;
                    case "#join":
                        if (this.isLoggedIn)
                            handleJoinTopic(tokens);
                        else
                            this.send(MsgTypes.SYSTEM, "ERROR! You need to log in first!");
                        break;
                    case "#leave":
                        if (this.isLoggedIn)
                            handleLeaveTopic(tokens);
                        else
                            this.send(MsgTypes.SYSTEM, "ERROR! You need to log in first!");
                        break;
                }
            } else {
                handleQuit();
                break;
            }
        }
        inputStream.close();
        outputStream.close();
        clientSocket.close();
        System.out.println("closing");
    }

    private void handleLogin(String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String login = tokens[1].trim();
            String password = tokens[2].trim();

            //refactor with users data ?db?
            if (login.equalsIgnoreCase("guest") && password.equalsIgnoreCase("guest")
                    || login.equalsIgnoreCase("alex") && password.equalsIgnoreCase("123")) {
                this.send(MsgTypes.SYSTEM, "successful login");
                this.login = login;
                System.out.println(login + " has logged in!");

                //send current user list of all online users
                //send all users current user's login message
                this.send(MsgTypes.SYSTEM, "Online users: " + server.getCurrentUsers());
                for (ServerWorker worker : server.getWorkersList()) {
                    if (worker.getLogin() != null)
                        if (!login.equalsIgnoreCase(worker.getLogin())) {
                            this.send(MsgTypes.LIST, worker.getLogin());
                            worker.send(MsgTypes.SYSTEM, login + " goes online");
                        }
                }
            } else {
                this.send(MsgTypes.SYSTEM, "Login error");
            }
            server.setCurrentUsers(server.getCurrentUsers() + 1);
            this.isLoggedIn = true;
        } else {
            this.send(MsgTypes.SYSTEM, "ERROR! Not a valid username/password");
        }
    }

    private void handleQuit() throws IOException {
        server.getWorkersList().remove(this);
        //send all users current user's logout message
        for (ServerWorker worker : server.getWorkersList())
            if (!login.equalsIgnoreCase(worker.getLogin()) && worker.getLogin() != null)
                worker.send(MsgTypes.SYSTEM, this.login + " went offline");

        server.setCurrentUsers(server.getCurrentUsers() - 1);
        this.isLoggedIn = false;
    }

    private void handleGetTopicsList() throws IOException {
        if (!server.getTopicsList().isEmpty())
            for (String topic : server.getTopicsList())
                this.send(MsgTypes.LIST, topic);
        else
            this.send(MsgTypes.SYSTEM, "There are no topics on the server");
    }

    private void handleNewTopic(String[] tokens) throws IOException {
        if (tokens.length > 1) {
            StringBuilder topicNameBuilder = new StringBuilder();
            topicNameBuilder.append("#");
            for (int i = 1; i < tokens.length; i++)
                topicNameBuilder.append(tokens[i]).append(" ");
            String convertedTopicName = topicNameBuilder.toString().trim().replaceAll(" ", "_");
            if (server.getTopicsList().contains(convertedTopicName))
                this.send(MsgTypes.SYSTEM, "This topic is already exists");
            else {
                server.getTopicsList().add(convertedTopicName);
                this.send(MsgTypes.SYSTEM, "Topic " + convertedTopicName + " was successfully created");
            }
        } else {
            this.send(MsgTypes.SYSTEM, "Error! Please write a topic name");
        }
    }

    private void handleJoinTopic(String[] tokens) throws IOException {
        if (tokens.length == 2) {
            String topicName = tokens[1];
            if (server.getTopicsList().contains(topicName)) {
                if (!this.isMemberOfTopic(topicName)) {
                    userTopics.add(topicName);
                    this.send(MsgTypes.SYSTEM, "You have successfully join to " + topicName);
                } else
                    this.send(MsgTypes.SYSTEM, "You have already joined to " + topicName);

            } else {
                this.send(MsgTypes.SYSTEM, "There is no topic \"" + topicName + "\" on this server");
            }
        } else {
            this.send(MsgTypes.SYSTEM, "ERROR! Please enter valid topic name");
        }
    }

    private void handleLeaveTopic(String[] tokens) throws IOException {
        if (tokens.length == 2) {
            String topicName = tokens[1];
            if (server.getTopicsList().contains(topicName)) {
                if (userTopics.contains(topicName)) {
                    userTopics.remove(topicName);
                    this.send(MsgTypes.SYSTEM, "You have successfully leave from " + topicName);
                } else
                    this.send(MsgTypes.SYSTEM, "You do not participate this topic");
            } else
                this.send(MsgTypes.SYSTEM, "There is no topic \"" + topicName + "\" on this server");
        } else {
            this.send(MsgTypes.SYSTEM, "ERROR! Please enter valid topic name");
        }
    }

    private void handleCommonMessage(String msg) throws IOException {
        for (ServerWorker worker : server.getWorkersList())
            if (worker.isLoggedIn())
                worker.send(MsgTypes.COMMON, this.login + ": " + msg);
    }

    private void handleSpecialMessage(String[] tokens) throws IOException {
        if (tokens.length > 2) {
            String modifier = tokens[1];

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 2; i < tokens.length; i++)
                messageBuilder.append(tokens[i]).append(" ");

            String convertedMessage = messageBuilder.toString().trim();
            boolean isTopic = modifier.trim().charAt(0) == '#';
            for (ServerWorker worker : server.getWorkersList()) {
                if (isTopic) {
                    if (worker.isMemberOfTopic(modifier))
                        worker.sendToTopic(getLogin(), convertedMessage, modifier);
                } else {
                    if (modifier.equalsIgnoreCase(worker.getLogin())) {
                        worker.send(MsgTypes.DIRECT, "(" + getLogin() + " -> you): " + convertedMessage);
                        this.send(MsgTypes.DIRECT, "(" + "you" + " -> " + worker.getLogin() + "): " + convertedMessage);
                    }
                }
            }
        } else
            this.send(MsgTypes.SYSTEM, "ERROR! Invalid command");
    }

    private void send(MsgTypes msgType, String msg) throws IOException {
        switch (msgType) {
            default:
                break;
            case NONE:
                outputStream.write((msg + System.lineSeparator()).getBytes());
                outputStream.flush();
                break;
            case SYSTEM:
                outputStream.write((SYS_LINE_PREFIX + msg + System.lineSeparator()).getBytes());
                outputStream.flush();
                break;
            case COMMON:
                outputStream.write((MSG_LINE_PREFIX + msg + System.lineSeparator()).getBytes());
                outputStream.flush();
                break;
            case DIRECT:
                outputStream.write((MSG_LINE_PREFIX + msg + System.lineSeparator()).getBytes());
                outputStream.flush();
                break;
            case LIST:
                outputStream.write((LIST_LINE_PREFIX + msg + System.lineSeparator()).getBytes());
                outputStream.flush();
                break;
        }
    }

    private void sendToTopic(String fromUser, String msg, String topicName) throws IOException {
        outputStream.write(("- " + topicName + "|" + fromUser + ": " + msg + System.lineSeparator()).getBytes());
        outputStream.flush();
    }
}
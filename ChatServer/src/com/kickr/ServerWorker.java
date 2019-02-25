package com.kickr;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ServerWorker extends Thread {
    private final Socket clientSocket;
    private final Server server;

    private String login;
    private OutputStream outputStream;

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public String getLogin() {
        return login;
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
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ( (line = reader.readLine()) != null ) {
            String[] tokens = StringUtils.split(line);
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("quit".equalsIgnoreCase(cmd) || "logoff".equalsIgnoreCase(cmd)) {
                    handleLogoff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                } else if("msg".equalsIgnoreCase(cmd)) {
                    handleDirectMessage(tokens);
                } else {
                    String msg = "unknown cmd: " + cmd + System.lineSeparator();
                    outputStream.write(msg.getBytes());
                }
            }
        }
        clientSocket.close();
    }

    private void handleDirectMessage(String[] tokens) {
        
    }


    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];

            if (login.equalsIgnoreCase("guest") && password.equalsIgnoreCase("guest") || login.equalsIgnoreCase("alex") && password.equalsIgnoreCase("123") || login.equalsIgnoreCase("guest2") && password.equalsIgnoreCase("123")) {
                String msg = "successful login" + System.lineSeparator();
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println(login + " has logged in!");

                ArrayList<ServerWorker> workersList = server.getWorkersList();

                //send current user list of all online users
                for (ServerWorker worker : workersList) {
                    if (worker.getLogin() != null) {
                        if (!login.equalsIgnoreCase(worker.getLogin())) {
                            String alreadyOnlMsg = worker.getLogin() + " is already online" + System.lineSeparator();
                            send(alreadyOnlMsg);
                        }
                    }
                }

                String onlineMsg = login + " is now online" + System.lineSeparator();
                //send all users current user's login message
                for (ServerWorker worker : workersList) {
                    if (!login.equalsIgnoreCase(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }
            } else {
                String msg = "error login" + System.lineSeparator();
                outputStream.write(msg.getBytes());
            }
        }
    }

    private void handleLogoff() throws IOException {
        server.getWorkersList().remove(this);
        String exitMsg = login + " is went offline" + System.lineSeparator();
        //send all users current user's login message
        for (ServerWorker worker : server.getWorkersList()) {
            worker.send(exitMsg);
        }
        clientSocket.close();
    }

    private void send(String msg) throws IOException {
        if (login != null)
            outputStream.write(msg.getBytes());
    }
}

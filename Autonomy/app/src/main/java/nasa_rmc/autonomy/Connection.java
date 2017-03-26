package nasa_rmc.autonomy;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Kyle on 2/13/2017.
 */
public class Connection {
    private enum ClientStatus {
        HANDSHAKE_INITIALIZED("Handshake initialized."),
        HANDSHAKE_SUCCESSFUL("Handshake successful.");
        String status;

        ClientStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }

    private enum ServerStatus {
        LISTENING("Server listening."),
        SERVER_SHUTDOWN("Server shutdown.");
        String status;

        ServerStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }

    int serverPortNumber;
    String clientIPAddress;
    int clientPortNumber;
    int bufferSize;

    Queue<String> serverQueue;

    ClientStatus clientStatus;
    ServerStatus serverStatus;
    Thread serverThread;
    Thread handShakeThread;
    ServerSocket serverSocket;

    public Connection(int serverPortNumber, String clientIPAddress,
                      int clientPortNumber, int bufferSize) {
        this.serverPortNumber = serverPortNumber;
        this.clientIPAddress = clientIPAddress;
        this.clientPortNumber = clientPortNumber;
        this.bufferSize = bufferSize;
        this.serverQueue = new ConcurrentLinkedQueue<>();

        try {
            serverThread = new server();
            handShakeThread = new handShake();

            serverThread.start();
            handShakeThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class handShake extends Thread {
        @Override
        public void run() {
            clientStatus = ClientStatus.HANDSHAKE_INITIALIZED;
            System.out.println(clientStatus);
            while (!this.isInterrupted()) {
                try {
                    Socket client = new Socket(clientIPAddress, clientPortNumber);

                    BufferedReader in =
                            new BufferedReader(
                                    new InputStreamReader(client.getInputStream()));

                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);

                    out.println("SYN");

                    if (in.readLine().equals("ACK")) {
                        out.println("SYN-ACK");

                        client.close();
                        in.close();
                        out.close();

                        clientStatus = ClientStatus.HANDSHAKE_SUCCESSFUL;
                        System.out.println(clientStatus);
                        break;
                    }
                } catch (ConnectException e) {
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class server extends Thread {
        private server() throws IOException {
            serverSocket = new ServerSocket(serverPortNumber);
        }

        @Override
        public void run() {
            serverStatus = ServerStatus.LISTENING;
            System.out.println(serverStatus);
            while (!this.isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();

                    BufferedReader in =
                            new BufferedReader(
                                    new InputStreamReader(socket.getInputStream()));

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    String message = in.readLine();
                    if (message.equals("SYN")) {
                        out.println("ACK");
                    } else {
                        System.out.println("Server received: " + message);
                        serverQueue.add(message);
                    }
                    socket.close();
                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void closeServerSocket() {
        try {
            serverSocket.close();
            serverThread.join();
            serverThread = null;
            handShakeThread.join();
            handShakeThread = null;
            serverStatus = ServerStatus.SERVER_SHUTDOWN;
            System.out.println(serverStatus);
        } catch (IOException e) {
            return;
        } catch (InterruptedException e) {
            return;
        }
    }

    public void sendMessage(String message) {
        try {
            Socket socket = new Socket(clientIPAddress, clientPortNumber);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
            System.out.println("Client sent:" + message);
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Connection main() {
        String CLIENT_IP_ADDRESS = "0.0.0.0";
        int CLIENT_PORT_NUMBER = 1123;

        String CONTROLLER_IP_ADDRESS = "192.168.1.3";
        int CONTROLLER_TO_CLIENT_PORT_NUMBER = 5813;
        int CONTROLLER_TO_TANGO_PORT_NUMBER = 2134;

        String TANGO_IP_ADDRESS = "192.168.1.4";
        int TANGO_PORT_NUMBER = 5589;

        int DEFAULT_BUFFER_SIZE = 1024;

        return new Connection(TANGO_PORT_NUMBER, CONTROLLER_IP_ADDRESS,
                CONTROLLER_TO_TANGO_PORT_NUMBER, DEFAULT_BUFFER_SIZE);
    }
}


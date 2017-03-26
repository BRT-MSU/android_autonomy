package nasa_rmc.autonomy;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

/**
 * Created by Alex on DAY=f/MONTH=?/YEAR=this
 *
 * Represents a connection to the Windows client.
 */
public class Network implements Runnable{
    public static final String NETWORK_TAG = Network.class.getSimpleName() + " (Network)";

    // Declare Needed Class Variables
    private DatagramSocket socket;
    private Handler autonomyActivityHandler;

    //Make sure this enum matches the one in the ArduinoConnection class
    private enum NetworkData {
        LEFT,  //Left hand wheel motors
        RIGHT, //Right hand wheel motors
        DIG,   // Dig Actuators
        DIG_LIFT,   // Raise/Lower Dig Linear Actuator
        DUMP,  // Dump Actuator
        AUTONOMY_ACTIVE
    }

    /*
     * Constructor for Network Object
     */
    public Network(int portNumber, Handler handler) {
        Log.v(NETWORK_TAG, "Network");

        // Connect Socket
        networkConnect(portNumber);

        // Initialize Robot Handler to Pass Commands
        this.autonomyActivityHandler = handler;
    }

    /*
     * Method to Connect to Control Client over UDP Socket
     */
    private void networkConnect(int portNumber) {
        Log.v(NETWORK_TAG, "networkConnect");
        // Initialize Socket
        try {
            socket = new DatagramSocket(portNumber);
        } catch (SocketException ex) {
            System.out.println("Cannot Create Socket!");
        }
    }

    @Override
    public void run() {
        while(true) {
            // Get Data from Socket
            byte[] remoteBuffer = recieveBufferCommands();

            // Create Bundle of size 2
            Bundle sendData = new Bundle(2);

            // Add Data to Bundle
            boolean autonomy = parseAutonomyState(remoteBuffer);
            sendData.putBoolean("AUTONOMY_ACTIVE", autonomy);

            sendData.putByteArray("BUFFER", parseBuffer(remoteBuffer));
            Log.d(NETWORK_TAG, "A: " + autonomy + " B: " + remoteBuffer[0]);

            Message commandMessage = autonomyActivityHandler.obtainMessage();
            commandMessage.obj = sendData;
            commandMessage.sendToTarget();
        }
    }

    private byte[] recieveBufferCommands() {
        byte[] packetBuffer = new byte[128];
        DatagramPacket packet = new DatagramPacket(packetBuffer, packetBuffer.length);

        Log.d(NETWORK_TAG, "About to receive.");
        // Try-Catch to Receive Data over Socket
        try {
            // Will pause until it receives something
            socket.receive(packet);
            Log.d(NETWORK_TAG, "Received Data: " + Arrays.toString(packet.getData()));
        } catch (Exception e) {
            Log.d(NETWORK_TAG, "Error Receiving Data.");
        }
        Log.d(NETWORK_TAG, "No data received");

        byte[] received;
        received = packet.getData();

        return received;
    }

    private boolean parseAutonomyState(byte[] networkBuffer) {
        return (networkBuffer[NetworkData.AUTONOMY_ACTIVE.ordinal()] != (byte) 0);
    }

    private byte[] parseBuffer(byte[] networkBuffer) {
        return Arrays.copyOfRange(networkBuffer, NetworkData.LEFT.ordinal(), NetworkData.DUMP.ordinal() + 1);
    }

    private String networkReceive() {
        byte[] packetBuffer = new byte[128];
        DatagramPacket packet = new DatagramPacket(packetBuffer, packetBuffer.length);

        String data = "";

        Log.d(NETWORK_TAG, "About to receive.");
        // Try-Catch to Receive Data over Socket
        try {
            // Will pause until it receives something
            socket.receive(packet);
            Log.d(NETWORK_TAG, "Received Data: " + Arrays.toString(packet.getData()));
        } catch (Exception e) {
            data += "No Data Received over UDP Socket.";
            Log.d(NETWORK_TAG, "Error Receiving Data.");
        }

        byte[] received;
        received = packet.getData();

        data += new String(received);

        return data;
    }
}

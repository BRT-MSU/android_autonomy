package com.example.angustomlinson.tangoautonomy20;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import com.felhr.usbserial.*; //https://github.com/felHR85/UsbSerial


/**
 * Created by Alex on 2/3/2016.
 * This class is represents a connection between the Tango and Arduino
 */
public class ArduinoConnection {

    private UsbSerialDevice serialConnection;

    //Represents the commands that will be pushed to the controller when update is called
    private byte[] buffer;
    private boolean foundArduino;

    private StringBuilder probeStatus;

    public String getReceivedData() {
        return receivedData;
    }

    //Declare an enum to manage buffer indices
    //Note: Since we are referring to the ordinal values, DO NOT CHANGE THE ORDERING OF THESE ENUMS
    public enum Motors {
        LEFT,      // Left hand wheel motors
        RIGHT,     // Right hand wheel motors
        DIG,       // Dig Steppers
        DIG_LIFT,  // Raise/Lower Dig Linear Actuator
        DUMP,      // Dump Steppers
        IR,
    }

    // Update the buffer's values for the given input
    void setLeftForward(byte value) {
        buffer[Motors.LEFT.ordinal()] = value;
    }

    public void setRightForward(byte value) {
        buffer[Motors.RIGHT.ordinal()] = value;
    }

    public void setDig(byte value) {
        buffer[Motors.DIG.ordinal()] = value;
    }

    public void setDump(byte value) {
        buffer[Motors.DUMP.ordinal()] = value;
    }

    public void setActuate(byte value) {
        buffer[Motors.DIG_LIFT.ordinal()] = value;
    }

    void setIR(byte value) {
        buffer[Motors.IR.ordinal()] = value;
    }

    public void resetBuffer() {
        setLeftForward((byte) 0);
        setRightForward((byte) 0);
        setDig((byte) 0);
        setDump((byte) 0);
        setActuate((byte) 0);
        setIR((byte) 0);
    }

    public ArduinoConnection(AutonomyActivity activity) {
        UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        this.serialConnection = null;

        //Initialize Buffer
        buffer = new byte[10];
        for (int i = 0; i < 10; i++) {
            buffer[0] = 0;
        }

        // Connect to Motor Arduino
        try {
            probeStatus = new StringBuilder();
            probeStatus.append("Probed Devices ");
            HashMap devices = manager.getDeviceList();
            probeStatus.append("(").append(devices.keySet().size()).append("): ");
            for (Object o : devices.keySet()) {
                String key = (String) o;
                UsbDevice device = (UsbDevice) devices.get(key);
                UsbDeviceConnection connection = manager.openDevice(device);
                UsbSerialDevice curSerialConnection = UsbSerialDevice.createUsbSerialDevice(device, connection);

                probeStatus.append("Key:").append(device.getDeviceName());

                //TODO: find a better thing rather than device id
                /*
                    We are assuming that the device is an arduino if its id != 2002.
                    For some reason, there is always at least one device connected to the Tango,
                    whose id is 2002. Not sure what it is, but it has something to do with tangofg
                 */
                if (device.getDeviceId() != 2002) {
                    probeStatus.append("{Device: Arduino Mega");
                    this.serialConnection = curSerialConnection;
                } else {
                    probeStatus.append("{Device: Other");
                }

                probeStatus.append(", ID: ").append(device.getDeviceId());
                probeStatus.append(", Probed=").append(curSerialConnection != null).append("} ");
            }

        foundArduino = true;

            foundArduino = serialConnection != null;

        } catch (Exception ex) {
            foundArduino = false;
        }

        if(foundArduino) {
            serialConnection.open();
            serialConnection.setBaudRate(9600);
            serialConnection.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serialConnection.setParity(UsbSerialInterface.PARITY_NONE);
            serialConnection.setStopBits(1);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(serialConnection != null && readCallback != null) {
                    serialConnection.read(readCallback);
                }
            }
        }).start();
    }

    public String foundStatus() {
        return ("\n" + probeStatus.toString());
    }

    public boolean arduinoConnected() {
        return foundArduino;
    }

    public void sendCommands() {
//        Will send commands to Arduino
        try {
            for(int i = 0; i < buffer.length; i++){
                Log.v("Buffer", Byte.toString(buffer[i]));
            }
            robotMove(buffer);
        } catch (IOException e) {
            //do something
        }
    }

    public String bufferValues() {
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (byte aBuffer : buffer) {
            sb.append(aBuffer).append(" ");
        }
        sb.append("]");

        return sb.toString();
    }

    private synchronized void robotMove(byte[] robotCommands) throws IOException {

        // Convert Robot Commands to String to Send
        String sendString = "";
        for (byte robotCommand : robotCommands) {
            sendString = (sendString + robotCommand + " ");
        }

        // Add Terminating Character
        sendString = sendString + "X";

        // Encode String to Byte Array
        byte[] sendData = null;
        try {
            sendData = sendString.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Receive Send Signal from Arduino
        serialConnection.read(readCallback);

        serialConnection.write(sendData);
    }

    private String receivedData = null;
    private UsbSerialInterface.UsbReadCallback readCallback = new UsbSerialInterface.UsbReadCallback() {

        @Override
        public void onReceivedData(byte[] read) {
            // Convert Received Data to String and Remove Empty Characters
            receivedData = new String(read);
        }
    };
}
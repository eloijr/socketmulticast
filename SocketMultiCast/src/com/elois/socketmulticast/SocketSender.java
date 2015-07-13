/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elois.socketmulticast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Eloi Jr
 */
public class SocketSender {
    
    private final List<ServerDevice> devices = new ArrayList<ServerDevice>();
    
    public static void main(String[] args) {        
        SocketSender socketSender = new SocketSender();
        Thread deviceDiscovery = new Thread(new DeviceDiscovery(socketSender));
        deviceDiscovery.start();
    }

    public void add(String name, String hostName) {
        devices.add(new ServerDevice(name, hostName));
    }
    
    private void showServers() {
        System.out.println("Devices Found:");
        for (ServerDevice device : devices) {
            System.out.println(device.Name + " - " + device.hostName);
        }
    }
    
    class ServerDevice {
        String Name;
        String hostName;
        
        public ServerDevice(String name, String hostname) {
            Name = name;
            hostName = hostname;
        }
    }
    
    static class DeviceDiscovery implements Runnable {

        private SocketSender socketSender;
        
        private static final int SERVER_PORT = 1805;    
        private static final String MULTICAST_ADDRESS = "226.0.0.1";
        private static final int WAITING_TIME = 10000;

        public DeviceDiscovery(SocketSender socketSender) {
            this.socketSender = socketSender;
        }
        
        @Override
        public void run() {
            System.out.println("SocketDiscovery");
            try {
                DatagramSocket dsocket = new DatagramSocket();
                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);

                String buffer = "whoisthere";
                try {
                    DatagramPacket packet = new DatagramPacket(buffer.getBytes(), buffer.getBytes().length, group, SERVER_PORT);
                    System.out.println("Sending data via multicast...");
                    dsocket.send(packet);

                    byte[] bBuffer = new byte[50];
                    packet = new DatagramPacket(bBuffer, bBuffer.length);
                    try {
                        long end_time = System.currentTimeMillis() + WAITING_TIME;
                        long start_time_receive, stop_time_receive;
                        dsocket.setSoTimeout(WAITING_TIME);
                        while (System.currentTimeMillis() < end_time) {
                            System.out.println("Waiting answers...");
                            start_time_receive = System.currentTimeMillis();                        
                            try {
                                dsocket.receive(packet);
                            } catch (SocketTimeoutException e) {
                                System.out.println("Socket time out.");
                            }
                            stop_time_receive = System.currentTimeMillis();
                            // Decrementa o tempo final do tempo decorrido do último receive
                            end_time = end_time - (stop_time_receive - start_time_receive);
                            System.out.println("start r: "+start_time_receive+" stop r: "+stop_time_receive+" end t: "+end_time);

                            if (packet.getAddress() != null) {
                                System.out.println("Received data from: " + packet.getAddress().toString() +
                                        ":" + packet.getPort() + " with length: " +
                                        packet.getLength());            
                               socketSender.add(new String(packet.getData(), 0, packet.getData().length), 
                                                packet.getAddress().toString()); 
                            }
                            Thread.sleep(500); // sleep for a half of second
                        }
                        System.out.println("Device searching finished!\n");
                        socketSender.showServers();
                    } finally {
                        dsocket.close();
                    }
                } finally {
                    dsocket.close();
                }

            } catch (Exception e) {
               e.printStackTrace();
            }        
        }

    }

}
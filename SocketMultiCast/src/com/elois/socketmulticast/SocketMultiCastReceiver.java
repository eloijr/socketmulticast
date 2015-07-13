/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elois.socketmulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 *
 * @author Eloi Jr
 */
public class SocketMultiCastReceiver {

    private static final int SERVER_PORT = 1805;
    private static final String MULTICAST_ADDRESS = "226.0.0.1";
    
    public static void main(String[] args) {
        System.out.println("SocketReceiver");
        try {
            MulticastSocket msocket = new MulticastSocket(SERVER_PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            msocket.joinGroup(group);
            
            DatagramSocket dsocket = new DatagramSocket();
            
            byte[] bBuffer = new byte[50];
            try {
                DatagramPacket packet = new DatagramPacket(bBuffer, bBuffer.length);
                System.out.println("Waiting incoming connection...");
                msocket.receive(packet);

                System.out.println("Received data from: " + packet.getAddress().toString() +
                            ":" + packet.getPort() + " with length: " +
                            packet.getLength() + " and data: "+new String(packet.getData(), 0, packet.getData().length));            

                String sBuffer = "LL-S";
                InetAddress ipclient = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(sBuffer.getBytes(), sBuffer.getBytes().length, ipclient, port);
                System.out.println("Sending back to the client " + packet.getAddress().toString().substring(1)+":"+port);
                dsocket.send(packet);            
            } finally {
                dsocket.close();
                msocket.leaveGroup(group);
                msocket.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }        
    }
    
}

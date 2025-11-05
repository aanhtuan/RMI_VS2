package org.raina;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ChatServerImpl extends UnicastRemoteObject implements ChatServer {
    private final Map<String, ChatClientCallback> clients = Collections.synchronizedMap(new HashMap<>());


    protected ChatServerImpl() throws RemoteException {
        super();
    }


    @Override
    public void register(String username, ChatClientCallback client) throws RemoteException {
        System.out.println("Registering: " + username);
        clients.put(username, client);
        broadcastSystem(username + " joined the chat");
    }


    @Override
    public void unregister(String username) throws RemoteException {
        System.out.println("Unregistering: " + username);
        clients.remove(username);
        broadcastSystem(username + " left the chat");
    }


    @Override
    public void sendMessage(String username, String message) throws RemoteException {
        System.out.println(username + ": " + message);
        synchronized (clients) {
            for (Map.Entry<String, ChatClientCallback> e : clients.entrySet()) {
                try {
                    e.getValue().deliver(username, message);
                } catch (RemoteException ex) {
// remove unreachable client
                    System.err.println("Removing unreachable client: " + e.getKey());
                    clients.remove(e.getKey());
                }
            }
        }
    }


    private void broadcastSystem(String msg) {
        synchronized (clients) {
            for (Map.Entry<String, ChatClientCallback> e : clients.entrySet()) {
                try {
                    e.getValue().deliver("<system>", msg);
                } catch (RemoteException ex) {
                    System.err.println("Removing unreachable client: " + e.getKey());
                    clients.remove(e.getKey());
                }
            }
        }
    }


    public static void main(String[] args) {
        try {
// optionally allow custom registry port
            int port = 1099;
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }


            ChatServerImpl server = new ChatServerImpl();
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("ChatServer", server);
            System.out.println("ChatServer bound in registry on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
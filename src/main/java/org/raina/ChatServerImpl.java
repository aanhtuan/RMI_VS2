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


    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", "192.168.1.175");
        int registryPort = 1099;
        Registry registry = LocateRegistry.createRegistry(registryPort);

        // tạo và bind trực tiếp; không export lại
        ChatServerImpl server = new ChatServerImpl(); // đã export bởi super()
        registry.rebind("ChatServer", server);

        System.out.println("ChatServer bound on " + System.getProperty("java.rmi.server.hostname") + ":" + registryPort);
    }

}
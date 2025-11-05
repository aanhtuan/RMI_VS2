package org.raina;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatServer extends Remote {
    void register(String username, ChatClientCallback client) throws RemoteException;
    void unregister(String username) throws RemoteException;
    void sendMessage(String username, String message) throws RemoteException;
}

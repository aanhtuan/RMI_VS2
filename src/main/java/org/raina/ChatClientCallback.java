package org.raina;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ChatClientCallback extends Remote {
    void deliver(String from, String message) throws RemoteException;
}
package com.noktuska.bot.noktuskabot_revamped.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.Main;
import com.noktuska.main.IRemoteObject;
import com.noktuska.main.Pair;
import com.noktuska.main.Reference;

public class RmiServer extends UnicastRemoteObject implements IRemoteObject {
	
	private static final long serialVersionUID = 547225129204399254L;
	
	private Main main;
	
	public RmiServer(Main main) throws RemoteException {
		this.main = main;
	}
	
	protected RmiServer() throws RemoteException {
		super();
	}
	
	public void register() throws RemoteException {
		try {
			Registry r = LocateRegistry.createRegistry(1099);
			r.bind(Reference.BINDING_NAME, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<Pair> getLogs() throws RemoteException {
		List<Pair> result = new ArrayList<Pair>(main.console.getLogs());
		main.console.clearNewLogs();
		return result;
	}

	@Override
	public void writeLog(String log) throws RemoteException {
		main.console.logWithoutResend(log);
	}

	@Override
	public List<Pair> receive() throws RemoteException {
		List<Pair> result = new ArrayList<Pair>(main.console.getNewLogs());
		main.console.clearNewLogs();
		return result;
	}
	
	@Override
	public void sendCommand(String cmd) throws RemoteException {
		main.console.compile(cmd);
	}
	
}

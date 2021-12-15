package com.haxzz.tl.objects.wrapper;

import java.util.ArrayList;
import java.util.List;

public class ConnectedClients {
	
	public int x = 0;
	public List<WebClientMini> clients = new ArrayList<WebClientMini>();
	
	public ConnectedClients() {}
	
	public ConnectedClients(List<WebClientMini> clients){
		this.clients = clients;
	}
	
	public void setNames(List<WebClientMini> clients) {
		this.clients = clients;
	}
	
	public List<WebClientMini> getClients(){
		return clients;
	}
}

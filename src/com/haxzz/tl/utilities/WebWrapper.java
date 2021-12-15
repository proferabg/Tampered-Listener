package com.haxzz.tl.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.objects.wrapper.ChatMessageInOut;
import com.haxzz.tl.objects.wrapper.ConnectedClients;
import com.haxzz.tl.objects.wrapper.MessageObject;
import com.haxzz.tl.objects.wrapper.WebClient;
import com.haxzz.tl.objects.wrapper.WebClientMini;
import com.haxzz.tl.objects.wrapper.WrapperLoginRequest;

public class WebWrapper {
	
	private SocketIOServer ioserver;
	private static List<WebClient> clients = new ArrayList<WebClient>();
	private static List<MessageObject> chatlog = new ArrayList<MessageObject>();
	private Timer runner1, runner2;
	private boolean started = false;

	public WebWrapper(String host, int port, String Origin){
		//start config
		Configuration config = new Configuration();
	    config.setHostname(host);
	    config.setPort(port);
	    config.setOrigin(Origin);
	    //start needed socket config
	    SocketConfig sc = config.getSocketConfig();
	    sc.setReuseAddress(true);
	    config.setSocketConfig(sc);
	    //start server
	    ioserver = new SocketIOServer(config);
	    //register those damn methods
	    RegisterMethods();
	    //start auth timer
	    runner1 = new Timer();
	    runner1.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				//Logger.logWithTime("Looping. Clients: "+clients.size());
				List<WebClient> tmp = new ArrayList<WebClient>(clients);
	        	for(WebClient wc : tmp){
	        		DateTime now = new DateTime();
	        		if(!wc.hasChallenged() && now.isAfter(wc.getJoinTime().plusSeconds(3))){
	        			wc.getSockClient().disconnect();
	        			Logger.logWithTime("%br_Wrapper: NO AUTH! Client Disconnected: "+wc.getIP(), "Wrapper", "", "");
	        			if(clients.contains(wc))clients.remove(wc);
	        		}
	        	}
			}
	    }, 0, 5000);
	    
	    runner2 = new Timer();
	    runner2.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				try {
					while(TamperedListener.getDB() == null) Thread.sleep(500);
					while(Utilities.isDbConnected() == false) Thread.sleep(500);
				} catch (InterruptedException e){ }
				if(Utilities.isDbConnected()){
					Logger.logWithTime("%bg_Wrapper: Last 10 Messages Restored.\r\n", "Wrapper", "", "");
					chatlog.addAll(Utilities.getChat(10));
					runner2.cancel();
				}
			}
	    }, 0, 1000);
	    
	    
	    //chatlog.addAll(Utilities.getChat(10));
	    
	    ioserver.start();
	    Logger.logWithTime("%bg_Web Wrapper Listening on "+ host + ":" + port + "\r\n", "Global", "", "");
	    started = true;
	}
	
	public void RegisterMethods(){
		ioserver.addConnectListener(new ConnectListener(){
			@Override
			public void onConnect(SocketIOClient client){
				WebClient wc = new WebClient(client);
				clients.add(wc);
				//Logger.logWithTime("%bc_Web Socket Connection From: "+wc.getIP()+" \r\n");
			}
		});
		
		ioserver.addEventListener("credentials", WrapperLoginRequest.class, new DataListener<WrapperLoginRequest>(){
			@Override
			public void onData(SocketIOClient client, WrapperLoginRequest request, AckRequest ackRequest){
	        	//prevent concurrent modification
	        	List<WebClient> tmp = new ArrayList<WebClient>(clients);
	        	WebClient wc = null;
				for(WebClient wc1 : tmp){
					if(wc1.getSockClient() == client){
						wc = wc1;
						break;
					}
				}
				
				if(wc == null){
					client.disconnect();
        			Logger.logWithTime("%br_Wrapper: FAILED AUTH! Client Disconnected: "+client.getRemoteAddress().toString().replaceAll("/", "").split(":")[0]+" \r\n", "Wrapper", "", "");
        			return;
				}
				
				if(request.getName().equals("") || request.getUid() < 1 || request.getParam1().equals("") || request.getParam2().equals("")){
        			wc.getSockClient().disconnect();
        			Logger.logWithTime("%br_Wrapper: FAILED AUTH! Client Disconnected: "+wc.getIP()+" \r\n", "Wrapper", "", "");
        			return;
				}
				
				wc.setName(request.getName());
				wc.setUID(request.getUid());
				wc.setBrowserAgent(request.getParam1());
				wc.setLoginString(request.getParam2());
				wc.PoolData();
				
				if(!wc.challenge()){
					wc.getSockClient().disconnect();
        			Logger.logWithTime("%br_Wrapper: "+wc.getName()+" Failed Authentication: " + wc.getIP()+" \r\n", "Wrapper", "", "");
        			return;
				} 
				
				Logger.logWithTime("%bg_Wrapper: "+wc.getName()+" Authenticated ("+wc.getIP()+") \r\n", "Wrapper", "", "");
				CloseOtherPages(wc);
				wc.getSockClient().sendEvent("chat", new ChatMessageInOut(chatlog));
				BroadcastClients();
			}
		});
		
		ioserver.addEventListener("chat", ChatMessageInOut.class, new DataListener<ChatMessageInOut>() {
	    	@Override
	    	public void onData(SocketIOClient client, ChatMessageInOut data, AckRequest ackRequest) {
	    		//prevent concurrent modification
	        	List<WebClient> tmp = new ArrayList<WebClient>(clients);
	        	WebClient wc = null;
				for(WebClient wc1 : tmp){
					if(wc1.getSockClient() == client){
						wc = wc1;
						break;
					}
				}
				
				if(wc == null || !wc.hasChallenged()){
					client.disconnect();
        			Logger.logWithTime("%br_Wrapper: FAILED AUTH! Client Disconnected: "+client.getRemoteAddress().toString().replaceAll("/", "").split(":")[0]+" \r\n", "Wrapper", "", "");
        			return;
				}
				
				String message = data.getMessage().replaceAll("([^A-Za-z0-9-./!:, ])", "");
				if(message.length() > 128) message = message.substring(0, 128);
				
				/*if (message.equalsIgnoreCase("/restart")){
					TamperedListener.setServerOn(false);
					new Thread(new Runnable(){
						@Override
						public void run() {
							try {
								TamperedListener.getSocketServer().close();
								Thread.sleep(1000);
								TamperedListener.StartServerThread();
							} catch (InterruptedException | IOException e) {
								Logger.error(e, "WebWrapper.Chat()");
							}
						}
					}).start();
					Logger.logWithTime("%br_Wrapper: Name: "+ wc.getName() + " restarted the server. IP: "+wc.getIP());
					SaveToChat(wc.getName(), "*** Restarted the server ***", CHATTYPE.RESTART);
	        	} else if (message.equalsIgnoreCase("/stop")){
	        		if (TamperedListener.isServerOn()){
						TamperedListener.setServerOn(false);
						try {
							TamperedListener.getSocketServer().close();
						} catch (IOException e){
							Logger.error(e, "WebWrapper.Chat()");
						}
			        	Logger.logWithTime("%br_Wrapper: Name: "+ wc.getName() + " stopped the server. IP: "+wc.getIP());
			        	SaveToChat(wc.getName(), "*** Stopped the server ***", CHATTYPE.STOP);
					}
	        	} else if (message.equalsIgnoreCase("/start")){
	        		if (!TamperedListener.isServerOn()){
	        			TamperedListener.setServerOn(true);
	        			TamperedListener.StartServerThread();
			        	Logger.logWithTime("%br_Wrapper: Name: "+ wc.getName() + " started the server. IP: "+wc.getIP());
		        		SaveToChat(wc.getName(), "*** Started the server ***", CHATTYPE.START);
					}
	        	} else */ if (message.equalsIgnoreCase("/cc")){
	        		chatlog = new ArrayList<MessageObject>();
		        	Logger.logWithTime("%br_Wrapper: Name: "+ wc.getName() + " cleared the chat. IP: "+wc.getIP(), "Wrapper", "", "");
	        		SaveToChat(wc, "*** Cleared the chat ***", CHATTYPE.CLEARCHAT);
	        	} else {
	        		if (message.equalsIgnoreCase("")) return;
	        		Logger.logWithTime("%bg_Wrapper: Chat - "+wc.getName()+" said %by_\""+ message + "\" %bg_IP: "+wc.getIP(), "Wrapper", "", "");
	        		SaveToChat(wc, message, CHATTYPE.GENERAL);
	        	}
	        }
	    });
		
		
		ioserver.addDisconnectListener(new DisconnectListener() {
	        @Override
	        public void onDisconnect(SocketIOClient client) {
	        	//prevent concurrent modification
	        	List<WebClient> tmp = new ArrayList<WebClient>(clients);
	        	for(WebClient wc : tmp){
	        		if(wc.getSockClient() == client){
	        			if(clients.contains(wc))clients.remove(wc);
	    	        	BroadcastClients();
	        			return;
	        		}
	        	}
	        }
	    });
	}
	
	public void BroadcastClients(){
		List<WebClient> tmp = new ArrayList<WebClient>(clients);
    	List<WebClientMini> clis = new ArrayList<WebClientMini>();
		for(WebClient wc : tmp){
			if(wc.hasChallenged()){
				clis.add(new WebClientMini(wc.getUID(), wc.getName(), wc.getIP(), wc.getAvatar(), wc.getLevel()));
			}
		}
		for(WebClient wc : tmp){
			if(!wc.hasChallenged()) continue;
			wc.getSockClient().sendEvent("connectedclients", new ConnectedClients(clis));
		}
	}

	public SocketIOServer getIOServer() {
		return ioserver;
	}

	public boolean isStarted() {
		return started && ioserver != null;
	}

	public List<WebClient> getClients() {
		return clients;
	}
	
	public void SaveToChat(WebClient wc, String message, CHATTYPE type){
		Utilities.SaveChatToDB(wc.getUID(), ((type == CHATTYPE.CLEARCHAT) ? "*0XCCX0*" : message));
		DateTimeFormatter fmt = DateTimeFormat.forPattern("hh:mm a").withZone(DateTimeZone.forID("America/New_York"));
		DateTime now = DateTime.now(DateTimeZone.UTC);
		//create message object
		MessageObject mo = new MessageObject(wc.getUID(), wc.getName(), wc.getAvatar(), message, fmt.print(now));
		//add message
		chatlog.add(mo);
		if(chatlog.size() > 151){
			List<MessageObject> newlog = new ArrayList<MessageObject>();
			for (int i = 1; i < 152; i++){
				newlog.add(chatlog.get(i));
			}
			chatlog = newlog;
		}
		List<WebClient> tmp = new ArrayList<WebClient>(clients);
		for(WebClient wc1 : tmp){
			if(!wc1.hasChallenged()) continue;
			wc1.getSockClient().sendEvent("chat", new ChatMessageInOut(chatlog));
		}
	}
	
	public void CloseOtherPages(WebClient wc){
		List<WebClient> tmp = new ArrayList<WebClient>(clients);
		for (WebClient wc1 : tmp){
			if(wc1.getUID() == wc.getUID() && wc1 != wc){
				wc1.getSockClient().sendEvent("closepage");
			}
		}
	}
	
}

enum CHATTYPE {
	GENERAL,
	CLEARCHAT,
	RESTART,
	STOP,
	START;
}

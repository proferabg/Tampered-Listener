package com.haxzz.tl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.haxzz.tl.commands.Handler;
import com.haxzz.tl.mysql.MariaDB;
import com.haxzz.tl.objects.config.API;
import com.haxzz.tl.objects.config.Config;
import com.haxzz.tl.objects.config.MariaDBC;
import com.haxzz.tl.objects.config.Patch;
import com.haxzz.tl.objects.config.SocketIO;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Metrics;
import com.haxzz.tl.utilities.Utilities;
import com.haxzz.tl.utilities.WebWrapper;
import com.haxzz.tl.kvchecker.KVChecker;

public class TamperedListener {
	
	//Single Config
	private static Config config;
	
	/* THESE ARE JUST DEFAULT VALUES BELOW */
	
	//XEXName
	private static String XEXName = "Tampered.Live";
	
	//Socket Port
	private static List<Integer> SockPorts = new ArrayList<Integer>(Arrays.asList(9685));
	
	//APIKey
	private static String APIURL = "http://178.32.56.60/API/api.php";
	private static String APIKey = "C87E783444809B1A8E3B8957251DFE";
	private static String APIXEX = "http://178.32.56.60/API/Tampered.Live";

	//socket io shit
	private static String IOIP = "199.19.225.11";
	private static int IOPort = 9687;
	private static String IOOrigin = "http://199.19.225.11:9683";
	
	//for disabling connection
	private static boolean ServerOn = true;
	
	//RC4 Stuff
	private static Cipher RC4Cipher;
	private static SecretKeySpec RC4KeyStore;
	private static byte[] Key = { 0x3f, (byte) 0xb1, 0x56, (byte) 0xeb, (byte) 0xf6, 0x04, 0x36, 0x7d, 0x0e, 0x35, 0x10, (byte) 0xe1, (byte) 0x8e, (byte) 0xaf, 0x66, 0x43 };
	
	//Socket Server
	private static List<ServerSocket> SockServers = new ArrayList<ServerSocket>();
	
	//SocketIOServer
	private static WebWrapper Webwrapper;
	
	//Metrics Tick
	private static Metrics metrics;

	//MYSQL Stuff
	private static MariaDB MariaDB;
	
	//MYSQL Credentials 
	private static String Host = "localhost", Port = "3306", Database = "TamperedLive", User = "TamperedLive", Password = "sAewaVVxzfuIPWr1";//qyt7JzDHG4NzvvLKUP6L2MWjkAsEbaAu
	
	//Patches
	private static List<Patch> Patches = new ArrayList<Patch>();
	
	//KVChecker Stuff
	private static KVChecker KVChecker;
	
	//debug
	private static boolean WritePackets = false;
	
	//developer mode
	private static boolean Developer = false;
	
	//blacklisted ips
	private static List<String> Blacklist = new ArrayList<String>();
	
	
	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack" , "true"); //very important as TUC liked ipv6
		ReadConfig();
		Webwrapper = new WebWrapper(IOIP, IOPort, IOOrigin);
		metrics = new Metrics();
		SetupRC4();
		InitializeDB();
		Utilities.PoolTitles();
		KVChecker = new KVChecker();
		StartServerThread();
	}
	
	private static void ReadConfig(){
		try {
			String configtxt = new String(Files.readAllBytes(Paths.get("config.json")));
			Gson gson = new Gson();
			config = gson.fromJson(configtxt, Config.class);
			//set values below
			XEXName = config.getXEXName();
			SockPorts.clear();
			for (int Port : config.getPorts()){
				SockPorts.add(Port);
			} 
			WritePackets = config.shouldWritePackets();
			Developer = config.isDeveloper();
			//api
			API api = config.getAPI();
			APIURL = api.getURL();
			APIKey = api.getKey();
			APIXEX = api.getXEXUrl();
			//Socket IO
			SocketIO socketio = config.getSocketIO();
			IOPort = socketio.getPort();
			IOIP = socketio.getIP();
			IOOrigin = socketio.getOrigin();
			//MariaDB
			MariaDBC mariadbc = config.getMariaDB();
			Host = mariadbc.getHost();
			Port = mariadbc.getPort();
			User = mariadbc.getUsername();
			Password = mariadbc.getPassword();
			Database = mariadbc.getDatabase();
			//patches
			int x = 0;
			for(Patch p : config.getPatches()){
				Patches.add(p);
				if(p.isEnabled()) x++;
			}
			//blacklist
			Blacklist = config.getBlacklist();
			Logger.logWithTime("%bg_Read Settings from Config.json.\r\n", "Global", "", "");
			if(Developer) Logger.logWithTime("%by_Warning: Running in Developer Mode.\r\n", "Global", "", "");
			if(WritePackets) Logger.logWithTime("%by_Warning: Writing Packets is Enabled.\r\n", "Global", "", "");
			Logger.logWithTime("%bc_"+Patches.size()+" Patches Loaded. "+x+" Enabled.\r\n", "Global", "", "");
			Logger.logWithTime("%bc_"+Blacklist.size()+" Blacklisted IPs.\r\n", "Global\r\n", "", "");
		} catch (IOException e){
			Logger.logWithTime("%br_Couldn't Read Settings from Config.json.\r\n", "Global", "", "");
		}
	}
	
	private static boolean SetupRC4(){
		try {
			RC4Cipher = Cipher.getInstance("RC4");
			RC4KeyStore = new SecretKeySpec(Key, "RC4");
			RC4Cipher.init(Cipher.ENCRYPT_MODE, RC4KeyStore);
			return true;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e){
			Logger.error(e, "TamperedListener.SetupRC4()");
			return false;
		}
	}
	
	public static boolean InitializeDB() {
		MariaDB = new MariaDB(Host, Port, Database, User, Password);
		if(Utilities.isDbConnected()){
			Logger.logWithTime("%bg_Connected to Database.\r\n", "Global", "", "");
			return true;
		}
		Logger.logWithTime("%br_Couldn't Connect to Database.\r\n", "Global", "", "");
		return false;
	}
	
	public static boolean StartServerThread() {
		try {
			ServerOn = true;
			//initialize server
			String ports = "";
			for(int Port : SockPorts){
				ports += Port + ", ";
				ServerSocket SockServer = new ServerSocket(Port);
				//reuse addr and port
				SockServer.setReuseAddress(true);
				//start server on new thread
				new Thread(new ServerThread(SockServer, Port)).start();
				//add to ilst
				SockServers.add(SockServer);
				
			}
			Logger.logWithTime("%by_Socket Server Listening on Port(s) " +ports.substring(0,ports.length() - 2)+ ".\r\n", "Global", "", "");
			return true;
		} catch (IOException e){
			e.printStackTrace();
			Logger.error(e, "TamperedListener.StartServerThread()");
			return false;
		}
	}

	
	//getters for referencing out of class
	public static boolean isServerOn() {
		return ServerOn;
	}
	
	public static void setServerOn(boolean serverOn) {
		ServerOn = serverOn;
	}

	public static Cipher getRC4Cipher() {
		return RC4Cipher;
	}

	public static MariaDB getDB() {
		return MariaDB;
	}
	
	public static String getXEXName() {
		return XEXName;
	}

	public static String getAPIURL() {
		return APIURL;
	}

	public static String getAPIKey() {
		return APIKey;
	}
	
	public static String getXEXUrl(){
		return APIXEX;
	}

	public static WebWrapper getWebWrapper() {
		return Webwrapper;
	}

	public static Metrics getMetrics() {
		return metrics;
	}

	public static List<Patch> getPatches() {
		return Patches;
	}
	
	public static List<ServerSocket> getSocketServers(){
		return SockServers;
	}
	
	public static KVChecker getKVChecker(){
		return KVChecker;
	}
	
	public static boolean getWritePackets(){
		return WritePackets;
	}

	public static String getIOOrigin() {
		return IOOrigin;
	}
	
	public static boolean isDeveloper(){
		return Developer;
	}
	
	public static List<String> getBlacklist(){
		return Blacklist;
	}
	
	
}


//runnable so it runs on separate thread and can have multiple instances (2nd part not wanted)
class ServerThread implements Runnable {
	
	ServerSocket SockServer;
	int Port;
	
	public ServerThread(ServerSocket SockServer, int Port){
		this.SockServer = SockServer;
		this.Port = Port;
	}

	@Override
	public void run() {
		try {
			Logger.logWithTime("%bg_*** Server Started on Port " + Port + " ***\r\n", "Global", "", "");
			while(TamperedListener.isServerOn()){
				Socket socket = SockServer.accept();
				//Logger.logWithTime("%by_*** Incoming Connection ***\r\n");
				new Thread(new Handler(socket)).start();
			}
		} catch (IOException e){
			if(TamperedListener.isServerOn() == false) {
				Logger.logWithTime("%br_*** Server Stopped on Port " + Port + " ***\r\n", "Global", "", "");
				return;
			}
			Logger.error(e, "TamperedListener.ServerThread.run()");
		}
	}
	

}

package com.haxzz.tl.objects.wrapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.joda.time.DateTime;

import com.corundumstudio.socketio.SocketIOClient;
import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class WebClient {
	
	private int UID = -1, Level = 0;
	private String IP = "", Name = "", LoginString = "", BrowserAgent = "", Password = "", Avatar = "";
	private boolean Challenged = false;
	private DateTime JoinTime;
	private long LoginTime = 0;
	private SocketIOClient SockClient;
	
	public WebClient(){
		JoinTime = new DateTime();
	}
	
	public WebClient(int UID){
		this.UID = UID;
	}
	
	public WebClient(SocketIOClient SockClient){
		this.SockClient = SockClient;
		JoinTime = new DateTime();
		this.IP = SockClient.getRemoteAddress().toString().replaceAll("/", "").split(":")[0];
	}
	
	public WebClient(int UID, String Name, String LoginString, String BrowserAgent, SocketIOClient SockClient){
		this.UID = UID;
		this.Name = Name;
		this.LoginString = LoginString;
		this.BrowserAgent = BrowserAgent;
		this.SockClient = SockClient;
		JoinTime = new DateTime();
		this.IP = SockClient.getRemoteAddress().toString().replaceAll("/", "").split(":")[0];
		PoolData();
	}

	public boolean hasChallenged() {
		return Challenged;
	}

	public void setChallenged(boolean challenged) {
		Challenged = challenged;
	}

	public int getUID() {
		return UID;
	}

	public String getName() {
		return Name;
	}

	public String getLoginString() {
		return LoginString;
	}

	public String getBrowserAgent() {
		return BrowserAgent;
	}

	public String getPassword() {
		return Password;
	}

	public DateTime getJoinTime() {
		return JoinTime;
	}
	
	public String getIP() {
		return IP;
	}

	public void setIP(String iP) {
		IP = iP;
	}

	public void setUID(int uID) {
		UID = uID;
	}

	public void setName(String name) {
		Name = name;
	}

	public void setLoginString(String loginString) {
		LoginString = loginString;
	}

	public void setBrowserAgent(String browserAgent) {
		BrowserAgent = browserAgent;
	}

	public SocketIOClient getSockClient() {
		return SockClient;
	}

	public void setSockClient(SocketIOClient sockClient) {
		SockClient = sockClient;
	}
	
	public int getLevel(){
		return Level;
	}
	
	public void setLevel(int Level){
		this.Level = Level;
	}

	public String getAvatar() {
		return Avatar;
	}

	public void PoolData(){
		if(UID == -1) return;
		try {
			if(Utilities.isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT `level`, `password`, `avatar`, `username`, `login_time` FROM `logins` WHERE `id` = '" + UID + "' LIMIT 1");
				if(res.next()) {
					Password = res.getString("password");
					Level = res.getInt("level");
					Avatar = res.getString("avatar");
					Name = res.getString("username");
					LoginTime = res.getLong("login_time");
				}
				res.close(); statement.close(); conn.close();
			}
		} catch(SQLException e){
			Logger.error(e, "WebClient.PoolData()");
		}
	}
	
	public boolean challenge(){
		try {
			String data = Password + BrowserAgent + LoginTime;
			MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
			String check = Utilities.bytesToHex(sha512.digest(data.getBytes()));
			//Logger.logWithTime("Check: "+check);
			//Logger.logWithTime("LogString: "+ LoginString);
			if(Level < 1) return false;
			if(check.equalsIgnoreCase(LoginString)){
				Challenged = true;
				return true;
			}
			return false;
		} catch (NoSuchAlgorithmException e){
			Logger.error(e, "WebClient.challenge()");
			return false;
		}
	}
}

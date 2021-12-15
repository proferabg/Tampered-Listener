package com.haxzz.tl.objects.wrapper;

public class WebClientMini {
	
	private int UID = 0, Level = 0;
	private String IP = "", Name = "", Avatar = "";
	
	public WebClientMini(int UID, String Name, String IP, String Avatar, int Level){
		this.UID = UID;
		this.Name = Name;
		this.IP = IP;
		this.Avatar = Avatar;
		this.Level = Level;
	}

	public int getUID() {
		return UID;
	}

	public void setUID(int uID) {
		UID = uID;
	}

	public String getIP() {
		return IP;
	}

	public void setIP(String iP) {
		IP = iP;
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public String getAvatar() {
		return Avatar;
	}

	public void setAvatar(String avatar) {
		Avatar = avatar;
	}

	public int getLevel() {
		return Level;
	}

	public void setLevel(int level) {
		Level = level;
	}
	
}

package com.haxzz.tl.objects.wrapper;

public class MessageObject {
	
	private int ID = 0;
	private String Name= "Server", Avatar = "img/default-avatar.png", Message = "-", Date = "00:00";
	
	public MessageObject(){}
	
	public MessageObject(int ID, String Name, String Avatar, String Message, String Date){
		this.ID = ID;
		this.Name = Name;
		this.Avatar = Avatar;
		this.Message = Message;
		this.Date = Date;
	}

	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
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

	public String getMessage() {
		return Message;
	}

	public void setMessage(String message) {
		Message = message;
	}

	public String getDate() {
		return Date;
	}

	public void setDate(String date) {
		Date = date;
	}
	
	

}

package com.haxzz.tl.objects.wrapper;

public class LogObject {
	
	private String Name = "Global";
	private String CPUKey = "-";
	private String Message = "";
	private String IP = "";
	
	public LogObject(String Name, String CPUKey, String IP, String Message){
		this.Name = Name;
		this.CPUKey = CPUKey;
		this.IP = IP;
		this.Message = Message;
	}

	public String getName() {
		return Name;
	}

	public void setName(String Name) {
		this.Name = Name;
	}

	public String getCPUKey() {
		return CPUKey;
	}

	public void setCPUKey(String CPUKey) {
		this.CPUKey = CPUKey;
	}

	public String getIP() {
		return IP;
	}

	public void setIP(String IP) {
		this.IP = IP;
	}
	
	public String getMessage() {
		return Message;
	}

	public void setMessage(String Message) {
		this.Message = Message;
	}
}

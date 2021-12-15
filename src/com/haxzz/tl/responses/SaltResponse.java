package com.haxzz.tl.responses;

import java.nio.ByteBuffer;

import com.haxzz.tl.enums.AUTH_TYPE;
import com.haxzz.tl.enums.STATUS_TYPE;
import com.haxzz.tl.utilities.Utilities;

public class SaltResponse {
	
	//default values
	private STATUS_TYPE Status = STATUS_TYPE.NO_AUTH;
	private AUTH_TYPE AuthStatus = AUTH_TYPE.NOT_AUTHED;
	private byte[] Session = new byte[8];
	private String Name = "";
	private int Days = 0;
	private long UnixExpire = 0;
	private byte[] Padding;
	private short KvUsedOn = 0;
	
	public SaltResponse() {
		this.Padding = Utilities.randomPadding();
	}

	public SaltResponse(STATUS_TYPE Status, AUTH_TYPE AuthStatus, byte[] Session, String Name, int Days, long UnixExpire){
		this.Status = Status;
		this.AuthStatus = AuthStatus;
		this.Session = Session;
		this.Name = Name;
		this.Days = Days;
		this.UnixExpire = UnixExpire;
		this.Padding = Utilities.randomPadding();
	}
	
	
	
	public void setStatus(STATUS_TYPE status) {
		Status = status;
	}

	public void setAuthStatus(AUTH_TYPE authStatus) {
		AuthStatus = authStatus;
	}

	public void setSession(byte[] session) {
		Session = session;
	}

	public void setName(String name) {
		Name = name;
	}

	public void setDays(int days) {
		Days = days;
	}

	public void setUnixExpire(long unixExpire) {
		UnixExpire = unixExpire;
	}

	public STATUS_TYPE getStatus() {
		return Status;
	}

	public AUTH_TYPE getAuthStatus() {
		return AuthStatus;
	}

	public byte[] getSession() {
		return Session;
	}

	public short getKvUsedOn() {
		return KvUsedOn;
	}

	public void setKvUsedOn(short kvUsedOn) {
		KvUsedOn = kvUsedOn;
	}

	public byte[] getBuffer(){
		ByteBuffer buffer = ByteBuffer.allocate(54);
		
		buffer.position(0);
		buffer.putShort(Status.getValue());
		
		buffer.position(2);
		buffer.putShort(AuthStatus.getValue());
		
		buffer.position(4);
		buffer.put(Session);
		
		buffer.position(12);
		buffer.put(Name.getBytes());
		
		buffer.position(32);
		buffer.putInt(Days);
		
		buffer.position(36);
		buffer.putLong(UnixExpire);
		
		buffer.position(44);
		buffer.putShort(KvUsedOn);
		
		buffer.position(46);
		buffer.put(Padding);
		
		return buffer.array();
	}
	
}

package com.haxzz.tl.responses;

import java.nio.ByteBuffer;

import com.haxzz.tl.utilities.Utilities;

public class TokenResponse {
	
	private int Days = 0;
	private byte[] Padding;
	
	public TokenResponse(){
		this.Padding = Utilities.randomPadding();
	}
	
	public TokenResponse(int Days){
		this.Days = Days;
		this.Padding = Utilities.randomPadding();
	}
	
	public void setDays(int Days){
		this.Days = Days;
	}
	
	public int getDays(){
		return Days;
	}
	
	public byte[] getBuffer(){
		ByteBuffer buffer = ByteBuffer.allocate(12);
		
		buffer.position(0);
		buffer.putInt(Days);
		
		buffer.position(4);
		buffer.put(Padding);
		
		return buffer.array();
	}

}

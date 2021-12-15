package com.haxzz.tl.responses;

import java.nio.ByteBuffer;

public class UpdateResponse {

	private int Length = 0;
	
	public UpdateResponse() {}

	public UpdateResponse(int Length){
		this.Length = Length;
	}
	
	public void setLength(int Length){
		this.Length = Length;
	}
	
	public byte[] getBuffer(){
		ByteBuffer buffer = ByteBuffer.allocate(4);
		
		buffer.position(0);
		buffer.putInt(Length);
		
		return buffer.array();
	}
	
}

package com.haxzz.tl.responses;

import java.nio.ByteBuffer;

public class PatchResponse {
	
	private int Length = 0;
	private byte[] Data = new byte[256];
	
	public PatchResponse() {}

	public PatchResponse(int Length, byte[] Data){
		this.Length = Length;
		this.Data = Data;
	}
	
	public void setLength(int Length){
		this.Length = Length;
	}
	
	public int getLength(){
		return Length;
	}
	
	public byte[] getData() {
		return Data;
	}

	public void setData(byte[] Data) {
		this.Data = Data;
	}

	public byte[] getBuffer(){
		ByteBuffer buffer = ByteBuffer.allocate(260);
		
		buffer.position(0);
		buffer.putInt(Length);
		
		buffer.position(4);
		buffer.put(Data);
		
		return buffer.array();
	}

}

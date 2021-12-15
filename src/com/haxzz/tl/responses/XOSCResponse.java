package com.haxzz.tl.responses;

import java.nio.ByteBuffer;

import com.haxzz.tl.enums.STATUS_TYPE;

public class XOSCResponse {
	
	private STATUS_TYPE Status = STATUS_TYPE.NO_AUTH;
	private byte[] Buffer = new byte[1024];

	
	public XOSCResponse(){}
	
	public XOSCResponse(STATUS_TYPE Status, byte[] Buffer){
		this.Status = Status;
		this.Buffer = Buffer;
	}

	public STATUS_TYPE getStatus() {
		return Status;
	}

	public void setStatus(STATUS_TYPE status) {
		Status = status;
	}

	public void setBuffer(byte[] buffer) {
		Buffer = buffer;
	}
	
	public byte[] getBuffer(){
		ByteBuffer buffer = ByteBuffer.allocate(1026);
		
		buffer.position(0);
		buffer.putShort(Status.getValue());
		
		buffer.position(2);
		buffer.put(Buffer);
		
		return buffer.array();
	}
	
	
}

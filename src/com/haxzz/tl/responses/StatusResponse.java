package com.haxzz.tl.responses;

import java.nio.ByteBuffer;

import com.haxzz.tl.enums.AUTH_TYPE;
import com.haxzz.tl.enums.CLI_CMD_TYPE;
import com.haxzz.tl.enums.STATUS_TYPE;
import com.haxzz.tl.utilities.Utilities;

public class StatusResponse {
	
	private STATUS_TYPE Status = STATUS_TYPE.NO_AUTH;
	private AUTH_TYPE AuthStatus = AUTH_TYPE.NOT_AUTHED;
	private CLI_CMD_TYPE ClientCommand = CLI_CMD_TYPE.NONE;
	private byte[] Padding;
	
	public StatusResponse(){
		this.Padding = Utilities.randomPadding();
	}
	
	public StatusResponse(STATUS_TYPE Status, AUTH_TYPE AuthStatus, CLI_CMD_TYPE ClientCommand){
		this.Status = Status;
		this.AuthStatus = AuthStatus;
		this.ClientCommand = ClientCommand;
		this.Padding = Utilities.randomPadding();
	}
	
	public void setStatus(STATUS_TYPE Status){
		this.Status = Status;
	}
	
	public void setAuthStatus(AUTH_TYPE AuthStatus){
		this.AuthStatus = AuthStatus;
	}
	
	public void setCommand(CLI_CMD_TYPE ClientCommand){
		this.ClientCommand = ClientCommand;
	}
	
	public STATUS_TYPE getStatus() {
		return Status;
	}

	public AUTH_TYPE getAuthStatus() {
		return AuthStatus;
	}

	public CLI_CMD_TYPE getClientCommand() {
		return ClientCommand;
	}

	public byte[] getBuffer(){
		ByteBuffer buffer = ByteBuffer.allocate(14);
		
		buffer.position(0);
		buffer.putShort(Status.getValue());
		
		buffer.position(2);
		buffer.putShort(AuthStatus.getValue());
		
		buffer.position(4);
		buffer.putShort(ClientCommand.getValue());
		
		buffer.position(6);
		buffer.put(Padding);
		
		//return buffer to send
		return buffer.array();
	}
	
}

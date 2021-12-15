package com.haxzz.tl.responses;

import java.nio.ByteBuffer;

import com.haxzz.tl.enums.CLI_CMD_TYPE;
import com.haxzz.tl.utilities.Utilities;

public class CommandResponse {
	
	private CLI_CMD_TYPE ClientCommand = CLI_CMD_TYPE.NONE;
	private String Message = "";
	private String Button = "";
	private byte[] Padding;
	
	public CommandResponse(){
		this.Padding = Utilities.randomPadding();
	}
	
	public CommandResponse(CLI_CMD_TYPE ClientCommand, String Message, String Button){
		this.ClientCommand = ClientCommand;
		this.Message = Message;
		this.Button = Button;
		this.Padding = Utilities.randomPadding();
	}
	
	
	
	public CLI_CMD_TYPE getClientCommand() {
		return ClientCommand;
	}

	public void setClientCommand(CLI_CMD_TYPE clientCommand) {
		ClientCommand = clientCommand;
	}

	public void setMessage(String message) {
		Message = message;
	}

	public void setButton(String button) {
		Button = button;
	}

	public String getMessage() {
		return Message;
	}

	public String getButton() {
		return Button;
	}

	public byte[] getBuffer(){
		ByteBuffer buffer = ByteBuffer.allocate(554);
		
		buffer.position(0);
		buffer.putShort(ClientCommand.getValue());
		
		buffer.position(2);
		buffer.put(Message.getBytes());
		
		buffer.position(514);
		buffer.put(Button.getBytes());
		
		buffer.position(546);
		buffer.put(Padding);
		
		return buffer.array();
	}
}

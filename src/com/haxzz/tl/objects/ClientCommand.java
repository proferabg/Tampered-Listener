package com.haxzz.tl.objects;

import com.haxzz.tl.enums.CLI_CMD_TYPE;

public class ClientCommand {
	
	private int Id = -1;
	private CLI_CMD_TYPE Type = CLI_CMD_TYPE.NONE;
	private String Message = "", Button = "", CPUKey = "";
	
	public ClientCommand(){}

	public ClientCommand(int Id, CLI_CMD_TYPE Type, String CPUKey){
		this.Id = Id;
		this.Type = Type;
		this.CPUKey = CPUKey;
	}
	
	public ClientCommand(int Id, CLI_CMD_TYPE Type, String Message, String Button, String CPUKey){
		this.Id = Id;
		this.Type = Type;
		this.Message = Message;
		this.Button = Button;
		this.CPUKey = CPUKey;
	}

	public int getId() {
		return Id;
	}

	public void setId(int Id) {
		this.Id = Id;
	}

	public CLI_CMD_TYPE getType() {
		return Type;
	}

	public void setType(CLI_CMD_TYPE Type) {
		this.Type = Type;
	}

	public String getMessage() {
		return Message;
	}

	public void setMessage(String Message) {
		this.Message = Message;
	}

	public String getButton() {
		return Button;
	}

	public void setButton(String Button) {
		this.Button = Button;
	}

	public String getCPUKey() {
		return CPUKey;
	}

	public void setCPUKey(String CPUKey) {
		this.CPUKey = CPUKey;
	}
}

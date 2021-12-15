package com.haxzz.tl.enums;

public enum CLI_CMD_TYPE {

	NONE(0),
	REBOOT(1),
	UPDATE(2),
	XNOTIFY(3),
	MSGBOX(4),
	DASHBOARD(5);
	
	private int value;
	
	private CLI_CMD_TYPE(int value) {
		this.value = value;
	}
	
	public short getValue() {
        return (short)this.value;
    }
	
	public static CLI_CMD_TYPE getValue(int i){
		for(CLI_CMD_TYPE c : CLI_CMD_TYPE.values()){
			if(c.getValue() == i) return c;
		}
		return NONE;
	}
	
}

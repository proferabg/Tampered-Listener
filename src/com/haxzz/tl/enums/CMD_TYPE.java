package com.haxzz.tl.enums;

public enum CMD_TYPE {
	
	NONE(0),
	SALT(1),
	STATUS(2),
	UPDATE(3),
	RDM_TOKEN(4),
	RCV_PATCH(5),
	RCV_CMD(6),
	XKE_CHAL(7),
	XOSC_CHAL(8);
	
	private int value;
	
	private CMD_TYPE(int value) {
		this.value = value;
	}
	
	public short getValue() {
        return (short)this.value;
    }

}

package com.haxzz.tl.enums;

public enum STATUS_TYPE {

	NO_AUTH(0),
	BLACKLIST(1),
	EXPIRED(2),
	ERROR(3),
	TAMPER(4),
	AUTHED(5),
	UPDATE(6),
	INVALID_SESSION(7);
	
	private int value;
	
	private STATUS_TYPE(int value) {
		this.value = value;
	}
	
	public short getValue() {
        return (short)this.value;
    }
	
}

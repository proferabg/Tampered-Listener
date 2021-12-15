package com.haxzz.tl.enums;

public enum AUTH_TYPE {
	
	GENERAL(0),
	LIFETIME(1),
	FREETIME(2),
	NOT_AUTHED(3),
	CHANGED_DAYS(4);
	
	
	private int value;
	
	private AUTH_TYPE(int value) {
		this.value = value;
	}
	
	public short getValue() {
        return (short)this.value;
    }

}

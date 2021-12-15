package com.haxzz.tl.enums;

public enum KV_CHECK {
	
	ERROR(0),
	BANNED(1),
	UNBANNED(2),
	INVALID(3);
	
	private int value;
	
	private KV_CHECK(int value) {
		this.value = value;
	}
	
	public int getValue() {
        return this.value;
    }

}

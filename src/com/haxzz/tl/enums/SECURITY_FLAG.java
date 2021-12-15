package com.haxzz.tl.enums;

public enum SECURITY_FLAG {
	PASSED(0),
	HASH_FAILED(1),
	SESSION_FAILED(2),
	BLACKLISTED(3),
	UPDATE_AVAIL(4),
	GENEALOGY_FAILED(5);
	
	private int value;
	
	private SECURITY_FLAG(int value){
		this.value = value;
	}
	
	public int getValue(){
		return value;
	}

}

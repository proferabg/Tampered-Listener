package com.haxzz.tl.enums;

public enum TOKEN_STATUS {
	
	NOT_EXIST(0xFFFFFFFF),
	ALRDY_RDM(0xFFFFFFFE),
	REDEEMED(0xFFFFFFFD),
	ERROR(0xFFFFFFFC),
	EXISTS(0xFFFFFFFB),
	BAD_FORMAT(0xFFFFFFFA),
	TRIAL_ALRDY_RDM(0xFFFFFFF9);
	
	private int value;
	
	private TOKEN_STATUS(int value) {
		this.value = value;
	}
	
	public short getValue() {
        return (short)this.value;
    }
	
}

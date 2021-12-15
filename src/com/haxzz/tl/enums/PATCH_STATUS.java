package com.haxzz.tl.enums;

public enum PATCH_STATUS {
	
	SRV_DISABLED(0xFFFFFFFF),
	CLI_DISABLED(0XFFFFFFFE);
	
	private int value;
	
	private PATCH_STATUS(int value) {
		this.value = value;
	}
	
	public short getValue() {
        return (short)this.value;
    }

}

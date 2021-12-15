package com.haxzz.tl.enums;

public enum CONSOLE_TYPE {
	
	XENON(0, "Xenon", 0x10B0524, 0x00000227, "0224EEA61E898BA155B5AF74AA78AD0B", "1231010600"),
	ZEPHYR(1, "Zephyr", 0x10C0AD0, 0x10000227, "0224EEA61E898BA155B5AF74AA78AD0B", "1221010A00"),
	FALCON(2, "Falcon", 0x10C0AD8, 0x20000227, "4EEAA3323D9F40AA90C00EFC5AD5B000", "1231010600"),
	JASPER(3, "Jasper", 0x10C0FFB, 0x30000227, "FF239990ED61D154B23135990D90BDBC", "1241020300"),
	TRINITY(4, "Trinity", 0x304000D, 0x40000227, "DBE6358778CBFC2F52A3BAF892458D65", "1251030100"),
	CORONA(5, "Corona", 0x304000E, 0x50000227, "D132FB439B4847E39FE54646F0A99EB1", "1262020500");
	
	private int value, cb, hw_flags;
	private String name, fuses, smc;
	
	private CONSOLE_TYPE(int value, String name, int cb, int hw_flags, String fuses, String smc) {
		this.value = value;
		this.name = name;
		this.cb = cb;
		this.hw_flags = hw_flags;
		this.fuses = fuses;
		this.smc = smc;
	}
	
	public int getValue() {
        return this.value;
    }
	
	public String getName() {
		return this.name;
	}
	
	public int getCBVersion() {
		return this.cb;
	}
	
	public int getHardwareFlags() {
		return this.hw_flags;
	}
	
	public String getFuseDigest() {
		return this.fuses;
	}
	
	public String getSMCVersion() {
		return this.smc;
	}

}

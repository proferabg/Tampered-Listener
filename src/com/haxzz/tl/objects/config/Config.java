package com.haxzz.tl.objects.config;

import java.util.List;

public class Config {
	
	private int[] Ports = { 0 };
	private String XEXName = "";
	private boolean Developer = false;
	private boolean WritePackets = false;
	private SocketIO SocketIO;
	private MariaDBC MariaDB;
	private API API;
	private Patch[] Patches;
	private List<String> Blacklist;
	
	
	public int[] getPorts() {
		return Ports;
	}
	
	public SocketIO getSocketIO() {
		return SocketIO;
	}
	
	public MariaDBC getMariaDB() {
		return MariaDB;
	}
	
	public API getAPI() {
		return API;
	}

	public Patch[] getPatches() {
		return Patches;
	}

	public String getXEXName() {
		return XEXName;
	}

	public boolean isDeveloper() {
		return Developer;
	}

	public boolean shouldWritePackets() {
		return WritePackets;
	}
	
	public List<String> getBlacklist(){
		return Blacklist;
	}
	
	
}

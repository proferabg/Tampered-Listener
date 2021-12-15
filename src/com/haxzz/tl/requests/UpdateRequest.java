package com.haxzz.tl.requests;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class UpdateRequest {
	
	private double Version;
	private String CPUKey;
	private String XEXHash;
	private String Session;
	private String GenealogyHash;

	private byte[] byte_20 = new byte[20];
	private byte[] byte_16 = new byte[16];
	private byte[] byte_8 = new byte[8];
	
	public UpdateRequest(byte[] buffer) {
		try {
			//define bytebuffer to read from
			DataInputStream input = new DataInputStream(new ByteArrayInputStream(buffer));
			
			//parse version from 2 shorts
			int maj = input.readShort();
			int min = input.readShort();
			Version = Double.parseDouble(maj + "." + min);
			
			//read in cpukey
			input.read(byte_16);
			CPUKey = Utilities.bytesToHex(byte_16);
			
			//read in executable hash
			input.read(byte_16);
			XEXHash = Utilities.bytesToHex(byte_16);
			
			//read in session key
			input.read(byte_8);
			Session = Utilities.bytesToHex(byte_8);

			//read genealogy
			input.read(byte_20);
			GenealogyHash = Utilities.bytesToHex(byte_20);
			
			//Close buffer
			input.close();

			//write packet
			Utilities.WritePacketBinary("UpdateRequest.bin", CPUKey, buffer);
			
		} catch (IOException e) {
			Logger.error(e, "UpdateRequest");
		}
		
	}
	

	public double getVersion() {
		return Version;
	}

	public String getCPUKey() {
		return CPUKey;
	}

	public String getXEXHash() {
		return XEXHash;
	}

	public String getSession() {
		return Session;
	}
	
	public String getGenealogyHash() {
		return GenealogyHash;
	}

}

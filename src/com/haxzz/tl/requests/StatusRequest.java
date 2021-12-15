package com.haxzz.tl.requests;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.haxzz.tl.enums.XSTATUS;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class StatusRequest {
	
	private double Version;
	private int TitleID;
	private XSTATUS XStatus;
	private String Gamertag;
	private String CPUKey;
	private String XEXHash;
	private String Session;
	private String GenealogyHash;

	private byte[] byte_20 = new byte[20];
	private byte[] byte_16 = new byte[16];
	private byte[] byte_8 = new byte[8];
	
	public StatusRequest(byte[] buffer) {
		try {
			if(buffer.length == 16){
				File file = new File("status.bin");
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(buffer);
				fos.flush();
				fos.close();
			}
			
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
			
			//titleid
			TitleID = input.readInt();
			
			//read 16 bytes then format to string
			input.read(byte_16);
			Gamertag = new String(Utilities.trimZeros(byte_16), StandardCharsets.UTF_8);
			if (!Gamertag.matches("[a-zA-Z0-9 ]+")) Gamertag = "N/A";
			
			//xstatus
			XStatus = XSTATUS.parseValue(input.readInt());
			
			//Close buffer
			input.close();

			//write packet
			Utilities.WritePacketBinary("StatusRequest.bin", CPUKey, buffer);
			
		} catch (IOException e) {
			Logger.error(e, "StatusRequest");
		}
		
	}
	

	public double getVersion() {
		return Version;
	}

	public int getTitleID() {
		return TitleID;
	}

	public String getGamertag() {
		return Gamertag;
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
	
	public XSTATUS getXStatus() {
		return XStatus;
	}
	
}

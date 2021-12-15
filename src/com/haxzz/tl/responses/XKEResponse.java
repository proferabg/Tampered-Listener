package com.haxzz.tl.responses;

import java.nio.ByteBuffer;

import com.haxzz.tl.enums.STATUS_TYPE;

public class XKEResponse {
	
	private STATUS_TYPE Status = STATUS_TYPE.NO_AUTH;
	private short BLDR = 0;
	private int HVFlags = 0;
	private int CBVersion = 0;
	private byte[] ECCDigest = new byte[20];
	private byte[] ChallSignature = new byte[128];
	private byte[] HVHash = new byte[6];
	private byte[] APIChecksum = new byte[20];
	
	
	public XKEResponse(){}
	
	public XKEResponse(STATUS_TYPE Status, short BLDR, int HVFlags, byte[] ECCDigest, byte[] ChallSignature, byte[] HVHash, byte[] APIChecksum){
		this.Status = Status;
		this.BLDR = BLDR;
		this.HVFlags = HVFlags;
		this.ECCDigest = ECCDigest;
		this.ChallSignature = ChallSignature;
		this.HVHash = HVHash;
		this.APIChecksum = APIChecksum;
	}

	public STATUS_TYPE getStatus() {
		return Status;
	}

	public void setStatus(STATUS_TYPE status) {
		Status = status;
	}

	public byte[] getECCDigest() {
		return ECCDigest;
	}

	public void setECCDigest(byte[] eCCDigest) {
		ECCDigest = eCCDigest;
	}

	public byte[] getHVHash() {
		return HVHash;
	}

	public void setHVHash(byte[] hVHash) {
		HVHash = hVHash;
	}

	public void setBLDR(short bLDR) {
		BLDR = bLDR;
	}


	public void setHVFlags(int hVFlags) {
		HVFlags = hVFlags;
	}

	public void setChallSignature(byte[] challSignature) {
		ChallSignature = challSignature;
	}
	
	public void setCBVersion(int cBVersion) {
		CBVersion = cBVersion;
	}
	
	public int getCBVersion(){
		return CBVersion;
	}

	public byte[] getAPIChecksum() {
		return APIChecksum;
	}

	public void setAPIChecksum(byte[] aPIChecksum) {
		APIChecksum = aPIChecksum;
	}

	public byte[] getBuffer(){
		ByteBuffer buffer = ByteBuffer.allocate(186);
		
		buffer.position(0);
		buffer.putShort(Status.getValue());
		
		buffer.position(2);
		buffer.putShort(BLDR);
		
		buffer.position(4);
		buffer.putInt(HVFlags);
		
		buffer.position(8);
		buffer.putInt(CBVersion);
		
		buffer.position(12);
		buffer.put(ECCDigest);
		
		buffer.position(32);
		buffer.put(ChallSignature);
		
		buffer.position(160);
		buffer.put(HVHash);
		
		buffer.position(166);
		buffer.put(APIChecksum);
		
		return buffer.array();
	}
	
	
}

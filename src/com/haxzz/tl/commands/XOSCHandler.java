package com.haxzz.tl.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.joda.time.DateTime;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.CONSOLE_TYPE;
import com.haxzz.tl.enums.SECURITY_FLAG;
import com.haxzz.tl.enums.STATUS_TYPE;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.requests.XOSCRequest;
import com.haxzz.tl.responses.XOSCResponse;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class XOSCHandler {
	
	Socket socket;
	ClientData client;
	XOSCRequest request;
	XOSCResponse response;
	String ZeroXSixty;
	String FuseDigest;
	CONSOLE_TYPE ConsoleType;
	boolean APISuccess = true;
	byte[] KV;
	
	public XOSCHandler(Socket socket, byte[] Buffer){
		this.socket = socket;
		request = new XOSCRequest(Buffer);
		client = new ClientData(request.getCPUKey());
		response = new XOSCResponse();
		DoResponse();
	}
	
	public void DoResponse(){
		client.setIP(socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]);
		client.setVersion(request.getVersion());
		client.setXEXHash(request.getXEXHash());
		client.setLastPing((DateTime.now().getMillis()/1000L));
		client.setCRL(true);
		
		SECURITY_FLAG flag = Utilities.checkFlags(client, request.getSession(), request.getGenealogyHash(), true);

		response.setStatus(Utilities.getStatusFromFlag(flag));

		if (Utilities.isBadFlag(flag)){
			WriteOutput();
			client.updateData();
			Log();
			return;
		}

		
		//set status
		if(!client.getIsInDB() && !Utilities.isFreeTime()){
			response.setStatus(STATUS_TYPE.NO_AUTH);//reboot
			WriteOutput();
			client.updateData();
			Log();
			return;
		} else if(client.hasTime() || Utilities.isFreeTime()){
			response.setStatus(STATUS_TYPE.AUTHED);//continue challenge
		} else {
			response.setStatus(STATUS_TYPE.EXPIRED);//reboot
			WriteOutput();
			client.updateData();
			Log();
			return;
		}
		
		KV = client.getKV();
		boolean type1kv = Utilities.isType1KV(Arrays.copyOfRange(KV, 0x1DF8, 0x1DF8 + 0x100));
		
		ConsoleType = Utilities.getConsoleType(Arrays.copyOfRange(KV, 0x9D1, 0x9D1 + 0x2), type1kv);

		ByteBuffer XOSCBuffer = ByteBuffer.wrap(request.getBuffer());

		//hv flags
		XOSCBuffer.position(0x158);
		XOSCBuffer.putInt(Utilities.getHVFlags(client.getCRL(), Utilities.getFCRT(Arrays.copyOfRange(KV, 0x1C, 0x1C + 0x2))));

		//bldr 
		XOSCBuffer.position(0x146);
		XOSCBuffer.putShort(Utilities.getBLDR(type1kv));
		
		//drive data 1
		XOSCBuffer.position(0xF0);
		XOSCBuffer.put(Arrays.copyOfRange(KV, 0xC8A, 0xC8A + 0x24));

		//drive data 2
		XOSCBuffer.position(0x114);
		XOSCBuffer.put(Arrays.copyOfRange(KV, 0xC8A, 0xC8A + 0x24));

		//xam region
		XOSCBuffer.position(0x148);
		XOSCBuffer.put(Arrays.copyOfRange(KV, 0xC8, 0xC8 + 0x2));

		//xam odd
		XOSCBuffer.position(0x14A);
		XOSCBuffer.put(Arrays.copyOfRange(KV, 0x1C, 0x1C + 0x2));

		//policy flash size
		XOSCBuffer.position(0x150);
		XOSCBuffer.put(Arrays.copyOfRange(KV, 0x24, 0x24 + 0x4));

		//hardware flags
		XOSCBuffer.position(0x1D0);
		XOSCBuffer.putInt(ConsoleType.getHardwareFlags());

		//ioctl_req
		XOSCBuffer.position(0x10);
		XOSCBuffer.putInt(0);

		//execution result
		XOSCBuffer.position(0x18);
		XOSCBuffer.putInt(0);

		//sizeMuSfc & sizeMuUsb
		XOSCBuffer.position(0x2B0);
		XOSCBuffer.putLong(0);

		//media dae
		XOSCBuffer.position(0x84);
		XOSCBuffer.putLong(0);

		//HV Protected Flags
		XOSCBuffer.position(0x198);
		XOSCBuffer.putLong(1);

		//execution id: media id
		XOSCBuffer.position(0x38);
		XOSCBuffer.putInt(0);

		//execution id: version
		XOSCBuffer.position(0x3C);
		XOSCBuffer.putInt(0x20446700);

		//execution id: base version
		XOSCBuffer.position(0x40);
		XOSCBuffer.putInt(0x20446700);

		//execution id: titleid
		XOSCBuffer.position(0x44);
		XOSCBuffer.putInt(0xFFFE07D1);

		//execution id: shit
		XOSCBuffer.position(0x48);
		XOSCBuffer.putLong(0);
		
		//start 0x60 + 0x70
		String SMC = ConsoleType.getSMCVersion();
		FuseDigest = ConsoleType.getFuseDigest();
		
		ZeroXSixty = CallAPI(TamperedListener.getAPIURL(), TamperedListener.getAPIKey(), request.getMacAddress(), SMC, request.getKVDigest(), FuseDigest);
		
		if(ZeroXSixty == null ||  ZeroXSixty.length() < 1 ||  ZeroXSixty.contains("0xC0000040") || ZeroXSixty.contains("Please make") || ZeroXSixty.contains("This API Key")){
			//api call failed lets retry
			Logger.logWithTime("%br_API Call Returned Error. Retrying...", client.getName(), client.getCPUKey(), client.getIP());
			ZeroXSixty = CallAPI(TamperedListener.getAPIURL(), TamperedListener.getAPIKey(), request.getMacAddress(), SMC, request.getKVDigest(), FuseDigest);
			//api call failed again.....
			if(ZeroXSixty == null ||  ZeroXSixty.length() < 1 ||  ZeroXSixty.contains("0xC0000040") || ZeroXSixty.contains("Please make") || ZeroXSixty.contains("This API Key")){
				Logger.logWithTime("%br_API Call Failed.", client.getName(), client.getCPUKey(), client.getIP());
				APISuccess = false;
			} 
		}
		
		if(APISuccess) {
			//0x60
			XOSCBuffer.position(0x60);
			XOSCBuffer.put(Utilities.hexToBytes(ZeroXSixty));
				
			//0x70
			XOSCBuffer.position(0x70);
			XOSCBuffer.put(Utilities.hexToBytes(FuseDigest));
		}
		
		//write buffer to packet
		response.setBuffer(XOSCBuffer.array());
		
		WriteOutput();
		client.updateData();
		Log();
	}
	
	public String CallAPI(String APIURL, String APIKey, String Mac, String SMC, String KVDigest, String FuseDigest){
		try {
			String tmp = Utilities.readURLFirefox(APIURL + "?action=getModuleDigest&apikey=" + APIKey + "&mac=" + Mac + "&smc=" + SMC + "&kvDigest=" + KVDigest + "&fuseDigest=" + FuseDigest);
			Utilities.WritePacketBinary("XOSC_API.txt", client.getCPUKey(), tmp.getBytes());
			return tmp.replaceAll("\\r\\n|\\r|\\n", "").replace(" ", "");
		} catch (Exception e) {
			Logger.error(e, "XOSCHandler.CallAPI()");
			return null;
		}
	}

	public void WriteOutput() {
		try {
			//write packet
			Utilities.WritePacketBinary("XOSCResponse.bin", client.getCPUKey(), response.getBuffer());
			
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			byte[] EncryptedBuffer = TamperedListener.getRC4Cipher().doFinal(response.getBuffer());
			output.write(EncryptedBuffer);
			output.flush();
			output.close();
		} catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
			Logger.error(e, "XOSCHandler.WriteOutput()");
		}
		
	}
	
	public void Log(){
		String print = "%c_\n\n-----------------"+Utilities.getTimestamp() + "-----------------\n\n" +
					   "%c_   XOSC Command:" + "\n" +
					   "%c_      - IP: %w_" + client.getIP() + "\n" +
				       "%c_      - CPUKey: %w_"+ client.getCPUKey() + "\n" + 
				       "%c_      - Name: %w_"+ client.getName() + "\n" + 
					   "%c_      - Hash: %w_"+ client.getXEXHash()+"\n" + 
					   "%c_      - Session: %w_"+request.getSession()+"\n" + 
					   "%c_      - Genealogy: %w_"+Utilities.getGeneologyHash(Utilities.hexToBytes(request.getGenealogyHash()), client.getCPUKey())+"\n" + 
				       "%c_      - Version: %w_"+ client.getVersion() + "\n" +
				       "%c_      - Status: %w_" + response.getStatus() + "\n" + 
				       "%c_      - Module Digest: %w_" + ZeroXSixty + "\n" + 
				       "%c_      - Fuse Digest: %w_" + FuseDigest + "\n" + 
				       "%c_      - KV Console Type: %w_" + ConsoleType.getName() + "\n" + 
				       "%c_\n-----------------"+Utilities.getTimestamp() + "-----------------\n";
		Logger.log(print, client.getName(), client.getCPUKey(), client.getIP());
	}
	
}

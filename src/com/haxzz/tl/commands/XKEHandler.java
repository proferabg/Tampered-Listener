package com.haxzz.tl.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.joda.time.DateTime;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.CONSOLE_TYPE;
import com.haxzz.tl.enums.SECURITY_FLAG;
import com.haxzz.tl.enums.STATUS_TYPE;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.requests.XKERequest;
import com.haxzz.tl.responses.XKEResponse;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class XKEHandler {
	
	Socket socket;
	ClientData client;
	XKERequest request;
	XKEResponse response;
	CONSOLE_TYPE ConsoleType;
	
	public XKEHandler(Socket socket, byte[] Buffer){
		this.socket = socket;
		request = new XKERequest(Buffer);
		client = new ClientData(request.getCPUKey());
		response = new XKEResponse();
		DoResponse();
	}
	
	public void DoResponse(){
		client.setIP(socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]);
		client.setVersion(request.getVersion());
		client.setXEXHash(request.getXEXHash());
		client.setLastPing((DateTime.now().getMillis()/1000L));
		
		SECURITY_FLAG flag = Utilities.checkFlags(client, request.getSession(), request.getGenealogyHash(), true);
		
		response.setStatus(Utilities.getStatusFromFlag(flag));

		if (Utilities.isBadFlag(flag)){
			response.setStatus(STATUS_TYPE.ERROR);
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
		
		boolean type1kv = Utilities.isType1KV(Arrays.copyOfRange(client.getKV(), 0x1DF8, 0x1EF8));
		
		response.setBLDR(Utilities.getBLDR(type1kv));
		
		response.setHVFlags(Utilities.getHVFlags(client.getCRL(), Utilities.getFCRT(Arrays.copyOfRange(client.getKV(), 0x1C, 0x1E))));
		
		ConsoleType = Utilities.getConsoleType(Arrays.copyOfRange(client.getKV(), 0x9D1, 0x9D1 + 0x2), type1kv);
		
		response.setCBVersion(ConsoleType.getCBVersion());
		
		String[] values = CallAPI(TamperedListener.getAPIURL(), TamperedListener.getAPIKey(), client.getSession(), request.getECCSalt(), request.getHVSalt());

		if(values == null ||  values.length < 2 ||  values[0].contains("Please make") || values[0].contains("This API Key") || values[1].contains("0xC0000020") || values[1].contains("0xC0000040")){
			//api call failed lets retry
			Logger.logWithTime("%br_API Call Returned Error. Retrying...", client.getName(), client.getCPUKey(), client.getIP());
			values = CallAPI(TamperedListener.getAPIURL(), TamperedListener.getAPIKey(), client.getSession(), request.getECCSalt(), request.getHVSalt());
			//api call failed again.....
			if(values == null ||  values.length < 2 ||  values[0].contains("Please make") || values[0].contains("This API Key") || values[1].contains("0xC0000020") || values[1].contains("0xC0000040")){
				Logger.logWithTime("%br_API Call Failed.", client.getName(), client.getCPUKey(), client.getIP());
				response.setStatus(STATUS_TYPE.ERROR);//reboot
				WriteOutput();
				client.updateData();
				Log();
				return;
			}
		}

		response.setECCDigest(Utilities.hexToBytes(values[1]));
		response.setChallSignature(Utilities.hexToBytes(values[2]));
		response.setHVHash(Utilities.hexToBytes(values[3]));
		response.setAPIChecksum(Utilities.hexToBytes(values[4]));
		
		WriteOutput();
		client.updateData();
		Log();
	}

	public String[] CallAPI(String APIURL, String APIKey, String Session, String ECCSalt, String HVSalt){
		try {
			String tmp = Utilities.readURLFirefox(APIURL + "?action=getChallenge&apikey=" + APIKey + "&challSalt=" + HVSalt + "&ecc=" + ECCSalt + "&bootid=" + Session);
			Utilities.WritePacketBinary("XKE_API.txt", client.getCPUKey(), tmp.getBytes());
			return tmp.split(",");
		} catch (Exception e) {
			Logger.error(e, "XKEHandler.CallAPI()");
			return null;
		}
	}
	
	
	
	public void WriteOutput() {
		try {
			//write packet
			Utilities.WritePacketBinary("XKEResponse.bin", client.getCPUKey(), response.getBuffer());
			
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			byte[] EncryptedBuffer = TamperedListener.getRC4Cipher().doFinal(response.getBuffer());
			output.write(EncryptedBuffer);
			output.flush();
			output.close();
		} catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
			Logger.error(e, "XKEHandler.WriteOutput()");
		}
		
	}
	
	public void Log(){
		String print = "%bm_\n\n-----------------"+Utilities.getTimestamp() + "-----------------\n\n" +
					   "%bm_   XKE Command:" + "\n" +
					   "%bm_      - IP: %w_" + client.getIP() + "\n" +
				       "%bm_      - CPUKey: %w_"+ client.getCPUKey() + "\n" + 
				       "%bm_      - Name: %w_"+ client.getName() + "\n" + 
					   "%bm_      - Hash: %w_"+ client.getXEXHash()+"\n" + 
					   "%bm_      - Session: %w_"+request.getSession()+"\n" + 
					   "%bm_      - Genealogy: %w_"+Utilities.getGeneologyHash(Utilities.hexToBytes(request.getGenealogyHash()), client.getCPUKey())+"\n" + 
				       "%bm_      - Version: %w_"+ client.getVersion() + "\n" +
				       "%bm_      - Status: %w_" + response.getStatus() + "\n" + 
				       "%bm_      - HV Salt: %w_" + request.getHVSalt() + "\n" + 
				       "%bm_      - HV Digest: %w_" + Utilities.bytesToHex(response.getHVHash()) + "\n" + 
				       "%bm_      - ECC Salt: %w_" + request.getECCSalt() + "\n" +
				       "%bm_      - ECC Digest: %w_" + Utilities.bytesToHex(response.getECCDigest()) + "\n" + 
				       "%bm_      - API Checksum: %w_" + Utilities.bytesToHex(response.getAPIChecksum()) + "\n" + 
				       "%bm_      - CB Version: %w_" + Integer.toHexString(ConsoleType.getCBVersion()).toUpperCase() + " (" + ConsoleType.getName() + ")\n" + 
				       "%bm_      - CRL: %w_" + client.getCRL() + "\n" +
					   "%bm_\n-----------------"+Utilities.getTimestamp() + "-----------------\n";
		Logger.log(print, client.getName(), client.getCPUKey(), client.getIP());
	}

}

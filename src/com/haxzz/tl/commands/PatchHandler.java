package com.haxzz.tl.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.joda.time.DateTime;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.PATCH_STATUS;
import com.haxzz.tl.enums.SECURITY_FLAG;
import com.haxzz.tl.enums.STATUS_TYPE;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.objects.config.Patch;
import com.haxzz.tl.requests.PatchRequest;
import com.haxzz.tl.responses.PatchResponse;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class PatchHandler {
	
	Socket socket;
	ClientData client;
	PatchRequest request;
	PatchResponse response;
	Patch patch;
	byte[] PatchBuffer;
	boolean badflag = false;
	boolean notauthed = false;
	STATUS_TYPE status;
	
	
	public PatchHandler(Socket socket, byte[] Buffer){
		this.socket = socket;
		request = new PatchRequest(Buffer);
		client = new ClientData(request.getCPUKey());
		response = new PatchResponse();
		DoResponse();
	}
	
	public void DoResponse(){
		client.setIP(socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]);
		client.setVersion(request.getVersion());
		client.setXEXHash(request.getXEXHash());
		client.setLastPing((DateTime.now().getMillis()/1000L));
		
		SECURITY_FLAG flag = Utilities.checkFlags(client, request.getSession(), request.getGenealogyHash(), true);
		status = Utilities.getStatusFromFlag(flag);
		
		if (Utilities.isBadFlag(flag)){
			badflag = true;
			response.setLength(PATCH_STATUS.SRV_DISABLED.getValue());
			WriteOutput();
			client.updateData();
			Log();
			return;
		}
		
		//set status
		if(!client.getIsInDB() || !client.hasTime()){
			notauthed = true;
			response.setLength(PATCH_STATUS.SRV_DISABLED.getValue());
			WriteOutput();
			client.updateData();
			Log();
			return;
		}
		
		patch = Utilities.getPatch(request.getGame());
		if (patch == null){
			response.setLength(PATCH_STATUS.SRV_DISABLED.getValue());
			WriteOutput();
			client.updateData();
			Log();
			return;
		}
		
		if (!patch.isEnabled()){
			response.setLength(PATCH_STATUS.SRV_DISABLED.getValue());
		} else {
			String value = GetPatchAPI(TamperedListener.getAPIURL(), TamperedListener.getAPIKey(), client.getSession(), patch.getTitleID());
			if(value == null ||  value.length() < 1 ||  value.contains("0xC0000040") || value.contains("Please make") || value.contains("This API Key")){
				//api call failed lets retry
				Logger.logWithTime("%br_API Call Returned Error. Retrying...", client.getName(), client.getCPUKey(), client.getIP());
				value = GetPatchAPI(TamperedListener.getAPIURL(), TamperedListener.getAPIKey(), client.getSession(), patch.getTitleID());
				//api call failed again.....
				if(value == null ||  value.length() < 1 ||  value.contains("0xC0000040") || value.contains("Please make") || value.contains("This API Key")){
					Logger.logWithTime("%br_API Call Failed.", client.getName(), client.getCPUKey(), client.getIP());
					response.setLength(PATCH_STATUS.SRV_DISABLED.getValue());
					WriteOutput();
					client.updateData();
					Log();
					return;
				}
			}
			
			PatchBuffer = Utilities.hexToBytes(value);
			//http://IPHERE/API/api.php?action=getAddressPkg&gameID=41560817&sessionkey=SESSIONKEYHERE
			response.setLength(PatchBuffer.length);
			response.setData(PatchBuffer);
		}
		
		WriteOutput();
		client.updateData();
		Log();
	}
	
	public String GetPatchAPI(String APIURL, String APIKey, String Session, String TitleID){
		try {
			String tmp = Utilities.readURLFirefox(APIURL + "?action=getAddressPkg&apikey=" + APIKey + "&gameID=" + TitleID + "&sessionkey=" + Session);
			Utilities.WritePacketBinary("Patch_" + TitleID + ".txt", client.getCPUKey(), tmp.getBytes());
			return tmp.replaceAll("\\r\\n|\\r|\\n", "").replace(" ", "");
		} catch (Exception e) {
			Logger.error(e, "PatchHandler.GetPatchAPI()");
			return null;
		}
	}
	
	public void WriteOutput() {
		try {
			//write packet
			Utilities.WritePacketBinary("PatchResponse.bin", client.getCPUKey(), response.getBuffer());
			
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			byte[] EncryptedBuffer = TamperedListener.getRC4Cipher().doFinal(response.getBuffer());
			output.write(EncryptedBuffer);
			output.flush();
			output.close();
		} catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
			Logger.error(e, "PatchHandler.WriteOutput()");
		}
		
	}
	
	public void Log(){
		String print = "%bb_\n\n-----------------"+Utilities.getTimestamp() + "-----------------\n\n" +
					   "%bb_   Patch Command:" + "\n" +
					   "%bb_      - IP: %w_" + client.getIP() + "\n" +
				       "%bb_      - CPUKey: %w_"+ client.getCPUKey() + "\n" + 
				       "%bb_      - Name: %w_"+ client.getName() + "\n" + 
					   "%bb_      - Hash: %w_"+ client.getXEXHash()+"\n" + 
					   "%bb_      - Session: %w_"+request.getSession()+"\n" + 
					   "%bb_      - Genealogy: %w_"+Utilities.getGeneologyHash(Utilities.hexToBytes(request.getGenealogyHash()), client.getCPUKey())+"\n" + 
				       "%bb_      - Version: %w_"+ client.getVersion() + "\n" +
					   ((badflag) ? "%bb_      - Status: %w_"+ status+"\n" : "") +
					   ((notauthed) ? "%bb_      - Status: %w_NO_AUTH\n" : "") +
				       "%bb_      - Game: %w_" + ((patch != null) ? patch.getName() : "") + "\n" +
				       "%bb_      - Patch Length: %w_" + response.getLength() + "\n" + 
				       "%bb_\n-----------------"+Utilities.getTimestamp() + "-----------------\n";
		Logger.log(print, client.getName(), client.getCPUKey(), client.getIP());
	}

}

package com.haxzz.tl.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.joda.time.DateTime;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.AUTH_TYPE;
import com.haxzz.tl.enums.CLI_CMD_TYPE;
import com.haxzz.tl.enums.SECURITY_FLAG;
import com.haxzz.tl.enums.STATUS_TYPE;
import com.haxzz.tl.objects.ClientCommand;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.requests.StatusRequest;
import com.haxzz.tl.responses.StatusResponse;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class StatusHandler {
	
	Socket socket;
	ClientData client;
	StatusRequest request;
	StatusResponse response;
	
	public StatusHandler(Socket socket, byte[] Buffer){
		this.socket = socket;
		request = new StatusRequest(Buffer);
		client = new ClientData(request.getCPUKey());
		response = new StatusResponse();
		DoResponse();
	}
	
	public void DoResponse(){
		client.setIP(socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]);
		client.setVersion(request.getVersion());
		client.setXEXHash(request.getXEXHash());
		client.setGamertag(request.getGamertag());
		client.setTitleID(Integer.toHexString(request.getTitleID()).toUpperCase());
		client.setXStatus(request.getXStatus().getValue());
		client.setTotalTimeUsed(((DateTime.now().getMillis()/1000L) - client.getLastPing()) + client.getTotalTimeUsed()); 
		client.setLastPing((DateTime.now().getMillis()/1000L));
		
		SECURITY_FLAG flag = Utilities.checkFlags(client, request.getSession(), request.getGenealogyHash(), true);
		
		response.setStatus(Utilities.getStatusFromFlag(flag));
		
		if (Utilities.isBadFlag(flag)){
			WriteOutput();
			client.updateData();
			Log();
			return;
		}
		
		//set status
		if(!client.getIsInDB()){
			response.setStatus(STATUS_TYPE.NO_AUTH);
		} else if(client.hasTime()){
			response.setStatus(STATUS_TYPE.AUTHED);
			response.setAuthStatus(client.handleAuth(false));
		} else {
			response.setStatus(STATUS_TYPE.EXPIRED);
		}
		
		ClientCommand command = Utilities.getCommand(client.getCPUKey());
		if (command.getType() != CLI_CMD_TYPE.NONE){
			response.setCommand(command.getType());
			if(command.getType() != CLI_CMD_TYPE.MSGBOX && command.getType() != CLI_CMD_TYPE.XNOTIFY){
				Utilities.deleteCommand(command.getId());
			}
		}
		
		WriteOutput();
		client.updateData();
		Log();
	}
	
	public void WriteOutput() {
		try {
			//write packet
			Utilities.WritePacketBinary("StatusResponse.bin", client.getCPUKey(), response.getBuffer());
			
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			byte[] EncryptedBuffer = TamperedListener.getRC4Cipher().doFinal(response.getBuffer());
			output.write(EncryptedBuffer);
			output.flush();
			output.close();
		} catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
			Logger.error(e, "StatusHandler.WriteOutput()");
		}
		
	}
	
	public void Log(){
		String print = "%bg_\n\n-----------------"+Utilities.getTimestamp() + "-----------------\n\n" +
					   "%bg_   Status Command:" + "\n" +
					   "%bg_      - IP: %w_" + client.getIP() + "\n" +
				       "%bg_      - CPUKey: %w_"+ client.getCPUKey() + "\n" + 
				       "%bg_      - Name: %w_"+ client.getName() + "\n" + 
					   "%bg_      - Hash: %w_"+ client.getXEXHash()+"\n" + 
					   "%bg_      - Session: %w_"+request.getSession()+"\n" + 
					   "%bg_      - Genealogy: %w_"+Utilities.getGeneologyHash(Utilities.hexToBytes(request.getGenealogyHash()), client.getCPUKey())+"\n" + 
				       "%bg_      - Version: %w_"+ client.getVersion() + "\n" +
				       "%bg_      - Status: %w_" + response.getStatus() + "\n" + 
				       "%bg_      - Auth: %w_" + response.getAuthStatus() + "\n" + 
				       ((client.hasTime() && response.getAuthStatus() != AUTH_TYPE.LIFETIME) ? 
				       "%bg_      - Expire Time: %w_" + Utilities.getExpireTime(client) + "\n" + 
				       "%bg_      - Days: %w_" + client.getDays() + "\n" : "") +
				       "%bg_      - Client CMD: %w_" + response.getClientCommand() + "\n" +
				       "%bg_      - Gamertag: %w_" + client.getGamertag() + "\n" + 
				       "%bg_      - TitleID: %w_" + client.getTitleID() + " (" + Utilities.GetTitle(client.getTitleID()) + ") \n" +
					   "%bg_\n-----------------"+Utilities.getTimestamp() + "-----------------\n";
		Logger.log(print, client.getName(), client.getCPUKey(), client.getIP());
		TamperedListener.getMetrics().logClient(client.getCPUKey());
	}

}

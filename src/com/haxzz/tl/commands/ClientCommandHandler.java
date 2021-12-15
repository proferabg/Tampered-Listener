package com.haxzz.tl.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.joda.time.DateTime;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.CLI_CMD_TYPE;
import com.haxzz.tl.enums.SECURITY_FLAG;
import com.haxzz.tl.objects.ClientCommand;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.requests.CommandRequest;
import com.haxzz.tl.responses.CommandResponse;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class ClientCommandHandler {
	
	Socket socket;
	ClientData client;
	CommandRequest request;
	CommandResponse response;
	
	public ClientCommandHandler(Socket socket, byte[] Buffer){
		this.socket = socket;
		request = new CommandRequest(Buffer);
		client = new ClientData(request.getCPUKey());
		response = new CommandResponse();
		DoResponse();
	}
	
	public void DoResponse(){
		client.setIP(socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]);
		client.setVersion(request.getVersion());
		client.setXEXHash(request.getXEXHash());
		client.setLastPing((DateTime.now().getMillis()/1000L));
		
		SECURITY_FLAG flag = Utilities.checkFlags(client, request.getSession(), request.getGenealogyHash(), true);
		
		if (Utilities.isBadFlag(flag)){
			response.setClientCommand(CLI_CMD_TYPE.REBOOT); //reboot if bad
			WriteOutput();
			client.updateData();
			Log();
			return;
		}
		
		ClientCommand thisCommand = Utilities.getCommand(client.getCPUKey());
		if(thisCommand.getType() == CLI_CMD_TYPE.MSGBOX || thisCommand.getType() == CLI_CMD_TYPE.XNOTIFY){
			response.setMessage(thisCommand.getMessage());
			response.setButton(thisCommand.getButton());
			Utilities.deleteCommand(thisCommand.getId());
		}
		
		ClientCommand nextCommand = Utilities.getCommand(client.getCPUKey());
		if (nextCommand.getType() != CLI_CMD_TYPE.NONE){
			response.setClientCommand(nextCommand.getType());
			if(nextCommand.getType() != CLI_CMD_TYPE.MSGBOX && nextCommand.getType() != CLI_CMD_TYPE.XNOTIFY){
				Utilities.deleteCommand(nextCommand.getId());
			}
		}
		
		WriteOutput();
		client.updateData();
		Log();
	}
	
	public void WriteOutput() {
		try {
			//write packet
			Utilities.WritePacketBinary("CommandResponse.bin", client.getCPUKey(), response.getBuffer());
			
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			byte[] EncryptedBuffer = TamperedListener.getRC4Cipher().doFinal(response.getBuffer());
			output.write(EncryptedBuffer);
			output.flush();
			output.close();
		} catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
			Logger.error(e, "ClientCommandHandler.WriteOutput()");
		}
	}
	
	public void Log(){
		String print = "%g_\n\n-----------------"+Utilities.getTimestamp() + "-----------------\n\n" +
					   "%g_   Client Command:" + "\n" +
					   "%g_      - IP: %w_" + client.getIP() + "\n" +
				       "%g_      - CPUKey: %w_"+ client.getCPUKey() + "\n" + 
				       "%g_      - Name: %w_"+ client.getName() + "\n" + 
					   "%g_      - Hash: %w_"+ client.getXEXHash()+"\n" + 
					   "%g_      - Session: %w_"+request.getSession()+"\n" + 
					   "%g_      - Genealogy: %w_"+Utilities.getGeneologyHash(Utilities.hexToBytes(request.getGenealogyHash()), client.getCPUKey())+"\n" + 
				       "%g_      - Version: %w_"+ client.getVersion() + "\n" +
				       "%g_      - Client CMD: %w_" + response.getClientCommand() + "\n" +
				       "%g_      - Message: %w_" + response.getMessage() + "\n" + 
				       "%g_      - Button: %w_" + response.getButton() + "\n" + 
					   "%g_\n-----------------"+Utilities.getTimestamp() + "-----------------\n";
		Logger.log(print, client.getName(), client.getCPUKey(), client.getIP());
	}
	
}

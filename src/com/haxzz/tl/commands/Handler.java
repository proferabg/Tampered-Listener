package com.haxzz.tl.commands;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.CMD_TYPE;
import com.haxzz.tl.utilities.Logger;

public class Handler implements Runnable {

	Socket Client;
	
	public Handler(Socket Client){
		this.Client = Client;
	}
	
	@Override
	public void run(){
		 //catch all errors
		try {
			//handle blacklist first
			String ip = Client.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0];
			if(TamperedListener.getBlacklist().contains(ip)){
				Client.close();
				return;
			}
			
			
			//command header
			int Command, Length;
			
			//assign input reader
			DataInputStream input = new DataInputStream(new BufferedInputStream(Client.getInputStream()));
			
			//read command header 
			Command = input.readInt();
			Length = input.readInt();
			
			//read raw buffer
			byte[] RawBuffer = new byte[Length];
			input.readFully(RawBuffer);
			
			//Log buffer size
			//Logger.logWithTime("%w_IP: " + Client.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]);
			//Logger.logWithTime("%w_Command: " + Command);
			//Logger.logWithTime("%w_Length: " + Length);
			//Logger.logWithTime("%w_Buffer Length: " + RawBuffer.length);
			
			//decrypt buffer
			byte[] DecryptedBuffer = TamperedListener.getRC4Cipher().doFinal(RawBuffer);

			//the handlers handle the full commands recv and send
			if(Command == CMD_TYPE.SALT.getValue()){
				new SaltHandler(Client, DecryptedBuffer);
			}
			else if (Command == CMD_TYPE.STATUS.getValue()){
				new StatusHandler(Client, DecryptedBuffer);
			}
			else if (Command == CMD_TYPE.UPDATE.getValue()){
				new UpdateHandler(Client, DecryptedBuffer);
			}
			else if (Command == CMD_TYPE.RDM_TOKEN.getValue()){
				new TokenHandler(Client, DecryptedBuffer);
			}
			else if (Command == CMD_TYPE.RCV_PATCH.getValue()){
				new PatchHandler(Client, DecryptedBuffer);
			}
			else if (Command == CMD_TYPE.RCV_CMD.getValue()){
				new ClientCommandHandler(Client, DecryptedBuffer);
			}
			else if (Command == CMD_TYPE.XKE_CHAL.getValue()){
				new XKEHandler(Client, DecryptedBuffer);
			}
			else if (Command == CMD_TYPE.XOSC_CHAL.getValue()){
				new XOSCHandler(Client, DecryptedBuffer);
			}
			else {
				Logger.log("Unknown Command", "Global", "", "");
			}
		} catch (EOFException e) {
			System.out.println("Ping from: " + Client.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0] + "\r\n");
		} catch (IllegalBlockSizeException | BadPaddingException | IOException e) {
			Logger.error(e, "Handler.run()");
		}
	}
	
}

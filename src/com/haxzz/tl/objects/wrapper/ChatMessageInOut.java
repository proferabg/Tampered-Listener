package com.haxzz.tl.objects.wrapper;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageInOut {
	
	private String message = "";
	private List<MessageObject> msgarray = new ArrayList<MessageObject>();
	
	public ChatMessageInOut() {}
	
	public ChatMessageInOut(List<MessageObject> msgarray){
		this.msgarray = msgarray;
	}

	public List<MessageObject> getMsgArray() {
		return msgarray;
	}

	public void setMsgArray(List<MessageObject> msgarray) {
		this.msgarray = msgarray;
	}

	public String getMessage() {
		return message;
	}
	
	
	
	

}

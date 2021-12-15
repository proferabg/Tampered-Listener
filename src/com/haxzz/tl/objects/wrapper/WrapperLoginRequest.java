package com.haxzz.tl.objects.wrapper;

public class WrapperLoginRequest {
	
	private int uid;
	private String name;
	private String param1;
	private String param2;
	
	public WrapperLoginRequest(){}
	
	public WrapperLoginRequest(int uid, String name, String param1, String param2){
		super();
		this.uid = uid;
		this.name = name;
		this.param1 = param1;
		this.param2 = param2;
	}
	
	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getParam1(){
		return param1;
	}
	
	public void setParam1(String param1){
		this.param1 = param1;
	}
	
	public String getParam2(){
		return param2;
	}
	
	public void setParam2(String param2){
		this.param2 = param2;
	}

}

package com.haxzz.tl.objects.wrapper;

public class MetricsPacket {
	
	private long prog_freemem, prog_usedmem, prog_totalmem;
	private double sys_freemem, sys_usedmem, sys_totalmem, load;
	private int cores, clients;
	private String os, javaver; 
	
	public MetricsPacket(){}

    public MetricsPacket(double load, int cores, long prog_freemem, long prog_usedmem, long prog_totalmem, double sys_freemem, double sys_usedmem, double sys_totalmem, int clients, String os, String javaver) {
        super();
        this.load = load;
        this.cores = cores;
        this.prog_freemem = prog_freemem;
        this.prog_usedmem = prog_usedmem;
        this.prog_totalmem = prog_totalmem;
        this.sys_freemem = sys_freemem;
        this.sys_usedmem = sys_usedmem;
        this.sys_totalmem = sys_totalmem;
        this.clients = clients;
        this.os = os;
        this.javaver = javaver;
    }
    
    public double getLoad() {
		return load;
	}

	public void setLoad(double load) {
		this.load = load;
	}

	public int getCores() {
		return cores;
	}

	public void setCores(int cores) {
		this.cores = cores;
	}

	public long getProg_Freemem() {
		return prog_freemem;
	}

	public void setProg_Freemem(long freemem) {
		this.prog_freemem = freemem;
	}

	public long getProg_Totalmem() {
		return prog_totalmem;
	}

	public void setProg_Totalmem(long totalmem) {
		this.prog_totalmem = totalmem;
	}

	public int getClients() {
		return clients;
	}

	public void setClients(int clients) {
		this.clients = clients;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	public String getJavaver() {
		return javaver;
	}

	public void setJavaver(String javaver) {
		this.javaver = javaver;
	}

	public long getProg_Usedmem() {
		return prog_usedmem;
	}

	public void setProg_Usedmem(long usedmem) {
		this.prog_usedmem = usedmem;
	}

	public double getSys_freemem() {
		return sys_freemem;
	}

	public void setSys_freemem(double sys_freemem) {
		this.sys_freemem = sys_freemem;
	}

	public double getSys_usedmem() {
		return sys_usedmem;
	}

	public void setSys_usedmem(double sys_usedmem) {
		this.sys_usedmem = sys_usedmem;
	}

	public double getSys_totalmem() {
		return sys_totalmem;
	}

	public void setSys_totalmem(double sys_totalmem) {
		this.sys_totalmem = sys_totalmem;
	}

}

package com.sshtools.djfeet.ui;

import java.util.Objects;

public class BusData implements Comparable<BusData> {
	
	private String name;
	private boolean activatable;
	private long pid;
	private String cmd;
	private String owner;

	public BusData(String name, boolean activatable, long pid, String cmd, String owner) {
		this.name = name;
		this.activatable = activatable;
		this.pid = pid;
		this.cmd = cmd;
		this.owner = owner;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BusData other = (BusData) obj;
		return Objects.equals(name, other.name);
	}

	public String getOwner() {
		return owner;
	}
	
	public String getCmd() {
		return cmd;
	}
	
	public long getPid() {
		return pid;
	}
	
	public boolean isActivatable() {
		return activatable;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "BusData [address=" + name + ", activatable=" + activatable + ", pid=" + pid + ", cmd=" + cmd + ", owner="
				+ owner + "]";
	}

	@Override
	public int compareTo(BusData o) {
		String an = name;
		String bn = o.getName();
		if (an.startsWith(":") && !bn.startsWith(":")) {
			return 1;
		} else if (!an.startsWith(":") && bn.startsWith(":")) {
			return -1;
		} else
			return an.compareTo(bn);
	}

	public void setOwner(String owner) {
		this.owner = owner;		
	}
}

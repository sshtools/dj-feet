package com.sshtools.djfeet.ui;

public class GroupData implements BusTreeData {
	
	private String text;

	public GroupData(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

	@Override
	public boolean isGroup() {
		return true;
	}

	@Override
	public String getDisplayName() {
		return text;
	}
}

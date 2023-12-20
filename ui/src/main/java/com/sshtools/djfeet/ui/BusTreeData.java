package com.sshtools.djfeet.ui;

public interface BusTreeData {

	default boolean isGroup() {
		return false;
	}
	
	String getDisplayName();
}

package com.sshtools.djfeet.ui;

import com.sshtools.djfeet.ui.ArgumentData.Direction;

public class SignalData extends ArgumentsData implements BusTreeData {
	
	private final String name;
	private final InterfaceData interfaceData;

	public SignalData(String name, InterfaceData interfaceData) {
		this.name = name;
		this.interfaceData = interfaceData;
		
		setShowNamesInSignature(false);
		setDirection(Direction.OUT);
	}

	public String toString() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return name;
	}

	public InterfaceData getInterfaceData() {
		return interfaceData;
	}
}

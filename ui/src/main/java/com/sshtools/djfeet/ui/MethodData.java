package com.sshtools.djfeet.ui;

import java.util.ArrayList;

import com.sshtools.djfeet.ui.ArgumentData.Direction;

import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class MethodData extends ArgumentsData implements BusTreeData {

	private String name;
	private InterfaceData interfaceData;

	public MethodData(InterfaceData interfaceData, String name) {
		this.name = name;
		this.interfaceData = interfaceData;
	}

	public InterfaceData getInterfaceData() {
		return interfaceData;
	}

	public String toString() {
		return name;
	}

	@Override
	protected void buildTextList(ArrayList<Text> l) {
		super.buildTextList(l);
		l.add(colorText("↦", null));
		l.add(colorText("(", Color.MAGENTA));
		addArgs(l, Direction.OUT);
		l.add(colorText(")", Color.MAGENTA));
	}

	@Override
	public String getDisplayName() {
		return name;
	}
}

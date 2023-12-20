package com.sshtools.djfeet.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.ResourceBundle;

import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

public class MonitorTab extends AnchorPane implements DBusSigHandler<DBusSignal> {
	
	public enum Type {
		SIGNAL
	}

	private final static ResourceBundle RESOURCES = ResourceBundle.getBundle(MonitorTab.class.getName());
	
	@FXML
	private Label type;	
	
	@FXML
	private Label memberLabel;
	
	@FXML
	private TextField interfaceName;
	
	@FXML
	private TextField member;

	private final DBusConnection connection;
	private final DJFeetApp context;

	public MonitorTab(DJFeetApp context, DBusConnection connection, Type type, Optional<String> interfaceName, Optional<String> signal) throws DBusException {
		this.connection = connection;
		this.context = context;

		var loader = new FXMLLoader(getClass().getResource("MonitorTab.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		loader.setResources(RESOURCES);
		try {
			loader.load();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		this.type.setText(RESOURCES.getString("type." + type.name()));
		this.interfaceName.setText(interfaceName.orElseGet(() -> RESOURCES.getString("interfaceName.any")));
		this.member.setText(signal.orElseGet(() -> RESOURCES.getString("member.any")));
		this.memberLabel.setText(RESOURCES.getString("member." + type.name()));
		
		var rule = new DBusMatchRule(type.name().toLowerCase(), interfaceName.orElse(null), signal.orElse(null));
		connection.addGenericSigHandler(rule, this);
		
	}

	@Override
	public void handle(DBusSignal _signal) {
		System.out.println(_signal);
		
	}

}

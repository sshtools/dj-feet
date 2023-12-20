package com.sshtools.djfeet.ui;

import static javafx.application.Platform.runLater;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.jajafx.AboutPage;
import com.sshtools.jajafx.AbstractTile;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class ViewPage extends AbstractTile<DJFeetApp> {
	final static Logger LOG = LoggerFactory.getLogger(ViewPage.class);

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(ViewPage.class.getName());

	@FXML
	private TabPane tabs;

	@Override
	protected void onConfigure() {

		try {
			createTabForConnectionBuilder(DBusConnectionBuilder.forSystemBus(), "System");
		} catch (Exception e) {
			LOG.error("Failed to connect to system bus.", e);
		}

		try {
			createTabForConnectionBuilder(DBusConnectionBuilder.forSessionBus(), "Session");
		} catch (Exception e) {
			LOG.error("Failed to connect to session bus.", e);
		}

		String address = null;
		try {
			try (var reader = Files
					.newBufferedReader(Paths.get(System.getProperty("java.io.tmpdir")).resolve("dbus-java.address"))) {
				address = reader.readLine();
			}
			createTabForConnectionBuilder(DBusConnectionBuilder.forAddress(address), "D-Bus Java");
		} catch (Exception e) {
			try {
				address = Preferences.systemRoot().node("com/sshtools/djfeet/daemon").get("dbusAddress", null);
				if (address == null)
					address = Preferences.systemRoot().node("uk/co/bithatch/djfeet").get("dbusAddress", null);
				if (address != null)
					createTabForConnectionBuilder(DBusConnectionBuilder.forAddress(address), "D-Bus Java");
			} catch (Exception e2) {
			}
		}
		int insertPoint = tabs.getTabs().size();
		if (insertPoint == 0)
			runLater(() -> newBus(null));
	}

	@FXML
	void about(ActionEvent evt) {
		getTiles().popup(AboutPage.class);
	}

	@FXML
	void options(ActionEvent evt) {
		getTiles().popup(OptionsPage.class);
	}

	@FXML
	void newBus(ActionEvent evt) {
		getTiles();
		var newBusPage = getTiles().popup(NewBusPage.class);
		newBusPage.onBuild = (s, conn) -> {
			try {
				var busContent = new DBusConnectionTab(tabs, getContext(), conn);
				var tab = new Tab(s, busContent);
				tabs.getTabs().add(tab);
			} catch (DBusException e) {
				LOG.error("Failed to create new bus.", e);
			}
		};
	}

	private void createTabForConnectionBuilder(DBusConnectionBuilder builder, String name) throws DBusException {
		var busContent = new DBusConnectionTab(tabs, getContext(), builder.build());
		var tab = new Tab(name, busContent);
		tab.setClosable(false);
		tabs.getTabs().add(tab);
	}

}

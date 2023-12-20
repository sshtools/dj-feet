package com.sshtools.djfeet.ui;

import static javafx.application.Platform.runLater;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.jajafx.AbstractTileWithNotifications;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class NewBusPage extends AbstractTileWithNotifications<DJFeetApp> {

	final static Logger LOG = LoggerFactory.getLogger(NewBusPage.class);
	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(NewBusPage.class.getName());

	@FXML
	private TextField address;
	@FXML
	private Hyperlink connect;
	@FXML
	private Label noKnownParameters;
	@FXML
	private VBox parameterHelp;
	@FXML
	private ComboBox<String> busType;

	BiConsumer<String, DBusConnection> onBuild;

	public NewBusPage() {
		super(RESOURCES);
	}

	@Override
	protected void onConfigureWithNotifications() {
		
		var types = TransportBuilder.getRegisteredBusTypes();
		
		busType.getItems().addAll(types);
		if (types.contains("UNIX")) {
			busType.getSelectionModel().select("UNIX");
		} else if (types.contains("TCP")) {
			busType.getSelectionModel().select("TCP");
		} else if (types.size() > 0) {
			busType.getSelectionModel().select(types.get(0));
		} else {
			busType.setTooltip(new Tooltip("Impossible! No DBUS transports"));
			connect.setDisable(true);
			address.setDisable(true);
		}
		
		busType.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> updateParameterHelp());
		connect.disableProperty().bind(Bindings.isEmpty(address.textProperty()));
		noKnownParameters.managedProperty().bind(noKnownParameters.visibleProperty());
		
		address.focusedProperty().addListener((c,o,n) -> {
			if(n) clearNotifications();
		});
		busType.focusedProperty().addListener((c,o,n) -> {
			if(n) clearNotifications();
		});
		
		updateParameterHelp();
	}

	private void updateParameterHelp() {
		parameterHelp.getChildren().clear();
		var selType = busType.getSelectionModel().getSelectedItem();
		var params = getSupportedKeys(selType);
		if (params.isEmpty()) {
			noKnownParameters.setVisible(true);
			address.setPromptText(RESOURCES.getString("prompt"));
		} else {
			noKnownParameters.setVisible(false);
			var reqd = params.entrySet().stream().filter(en -> en.getValue()).map(en -> en.getKey()).toList();
			if (reqd.size() > 0) {
				var lbl = new Label(RESOURCES.getString("required"));
				lbl.getStyleClass().addAll("h3", "tpad", "bpad");
				parameterHelp.getChildren().add(lbl);
				addParamHelp(selType, reqd);

				var b = new StringBuilder();
				for (var req : reqd) {
					if (b.length() > 0)
						b.append(",");
					b.append(req + "=<" + req + ">");
				}
				address.setPromptText(b.toString());
			} else
				address.setPromptText(RESOURCES.getString("prompt"));
			var opts = params.entrySet().stream().filter(en -> !en.getValue()).map(en -> en.getKey()).toList();
			if (opts.size() > 0) {
				var lbl = new Label(RESOURCES.getString("optional"));
				lbl.getStyleClass().addAll("h3", "tpad", "bpad");
				parameterHelp.getChildren().add(lbl);
				addParamHelp(selType, opts);
			}
		}
	}

	private void addParamHelp(String selType, List<String> reqd) {
		for (var en : reqd) {
			var nm1 = new Label(en);
			nm1.getStyleClass().add("lpad");
			nm1.setPrefWidth(110);
			nm1.setMinWidth(110);
			var nm2 = new Label(" ");
			var nm3 = new Label(RESOURCES.getString(selType + "." + en));
			nm3.setWrapText(true);
			nm3.getStyleClass().addAll("emphasis", "muted");
			var hbox = new HBox(nm1, nm2, nm3);
			hbox.setMinHeight(32);
			hbox.getStyleClass().add("spaced");
			parameterHelp.getChildren().add(hbox);
		}
	}

	private Map<String, Boolean> getSupportedKeys(String type) {
		switch (type) {
		case "UNIX":
			return Map.of("path", true);
		case "TCP":
			return Map.of("host", true, "port", false);
		case "SSH":
			return Map.of("via", true, "username", true, "uid", true, "bus", false, "viaPort", false, "path", false,
					"host", false, "password", false, "key", false, "passphrase", false);
		default:
			return Collections.emptyMap();
		}
	}

	private String getSelectedBusType() {
		return busType.getSelectionModel().getSelectedItem();
	}

	@FXML
	private void back(ActionEvent evt) {
		clearNotifications();
		getTiles().remove(this);
	}

	@FXML
	private void connect(ActionEvent evt) {
		clearNotifications();
		new Thread(() -> run(getSelectedBusType(), address.getText())).start();
	}

	private void run(String busType, String addressText) {
		var addr = busType + ":" + addressText;
		try {
			var params = getSupportedKeys(busType);
			var reqd = params.entrySet().stream().filter(en -> en.getValue()).map(en -> en.getKey()).toList();

			var baddr = BusAddress.of(addr);
			var shortAddress = new StringBuffer();

			for (var req : reqd) {
				if (!baddr.hasParameter(req))
					throw new IllegalStateException("Missing parameter " + req);
				if (shortAddress.length() > 0)
					shortAddress.append(",");
				shortAddress.append(req + "=" + baddr.getParameterValue(req));
			}

			if (busType.equals("SSH")) {
				if (!baddr.hasParameter("path")) {
					if (baddr.hasParameter("bus")) {
						if (baddr.getParameterValue("bus").equalsIgnoreCase("system")) {
							baddr.addParameter("path", "/run/dbus/system_bus_socket");
						} else {
							baddr.addParameter("path", "/run/user/" + baddr.getParameterValue("uid") + "/bus");
						}
					} else {
						baddr.addParameter("path", "/run/user/" + baddr.getParameterValue("uid") + "/bus");
					}
				}
			}

			var bldr = DBusConnectionBuilder.forAddress(baddr);
			bldr.withShared(false);
			bldr.transportConfig().withAutoConnect(false);

			if (baddr.hasParameter("uid")) {
				bldr.transportConfig().configureSasl().withSaslUid(Long.parseLong(baddr.getParameterValue("uid")));
			}

			var conx = bldr.build();
			conx.connect();
			conx.register();
			
			runLater(() -> {
				onBuild.accept(busType.toLowerCase() + ":" + shortAddress.toString(), conx);
				getTiles().remove(this);
			});
		} catch (Exception e) {
			LOG.error("Failed to connect to bus.", e);
			runLater(() -> message(FontIcon.of(FontAwesomeSolid.EXCLAMATION_CIRCLE), "notification-danger", "failed",
					addr, e.getMessage()));
		}
	}
}

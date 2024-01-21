package com.sshtools.djfeet.ui;

import static javafx.application.Platform.runLater;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.utils.Util;
import org.freedesktop.dbus.utils.generator.InterfaceCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.jajafx.AbstractTile;
import com.sshtools.jajafx.PrefBind;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

public class ExportPage extends AbstractTile<DJFeetApp> {
	final static Logger LOG = LoggerFactory.getLogger(ExportPage.class);

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(ExportPage.class.getName());

	private static String toPackageName(String interfaceName) {
		if (interfaceName.contains(".")) {
			return interfaceName.substring(0, interfaceName.lastIndexOf('.')).toLowerCase();
		} else {
			return interfaceName.toLowerCase();
		}
	}
	
	private static void writeToFile(String _outputDir, Map<File, String> _filesToGenerate) {
		for (var entry : _filesToGenerate.entrySet()) {
			var outputFile = new File(_outputDir, entry.getKey().getPath());

			if (!outputFile.getParentFile().exists()) {
				outputFile.getParentFile().mkdirs();
			}

			if (Util.writeTextFile(outputFile.getAbsolutePath(), entry.getValue(), Charset.defaultCharset(), false)) {
				LOG.info("Created class file {}", outputFile.getAbsolutePath());
			} else {
				LOG.error("Could not write content to class file {}", outputFile.getName());
			}
		}
	}
	
	@FXML
	private TextField packageName;
	@FXML
	private TextField outputDirectory;
	@FXML
	private CheckBox useDBusBoundProperty;
	@FXML
	private CheckBox disableFilter;
	
	private DBusConnection conn;
	private final BooleanProperty busy = new SimpleBooleanProperty();
	private PrefBind prefBind;
	private List<ObjectData> objectData;

	@Override
	public void close() {
		prefBind.close();
	}

	@Override
	protected void onConfigure() {
		prefBind = new PrefBind(getContext().getContainer().getAppPreferences());
		prefBind.bind(useDBusBoundProperty);
		prefBind.bind(disableFilter);
		prefBind.bind(outputDirectory);

		var home = new File(System.getProperty("user.home"));
		var dir = new File(home.getPath() + File.separator + "Documents" + File.separator + "DJ-Feet Exports");
		outputDirectory.setPromptText(dir.getAbsolutePath());
	}

	void init(DBusConnection conn, List<ObjectData> objectData) {
		this.conn = conn;
		this.objectData = objectData;
		packageName.setPromptText(objectData.size() == 0 ? "Automatic" : toPackageName(objectData.get(0).getName()));
	}

	@FXML
	private void back(ActionEvent evt) {
		getTiles().remove(this);
	}

	@FXML
	private void browse(ActionEvent evt) {
		var directoryChooser = new DirectoryChooser();
		var dir = getOutputDirectory();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		directoryChooser.setInitialDirectory(dir);
		directoryChooser.setTitle("Choose directory to export to.");
		var selectedDirectory = directoryChooser.showDialog(getContext().getWindows().get(0).stage());
		if (selectedDirectory != null) {
			var path = selectedDirectory.getAbsolutePath();
			if (path.equals(outputDirectory.getPromptText())) {
				outputDirectory.setText("");
			} else {
				outputDirectory.setText(path);
			}
		}
	}

	private void execute(File selectedDirectory, String pkgName, boolean boundProps) {
		try {
			if (!selectedDirectory.exists()) {
				selectedDirectory.mkdirs();
			}

			var allMap = new HashMap<File, String>();
			for (var data : objectData) {
				var in = conn.getRemoteObject(data.getBusData().getName(), data.getPath(), Introspectable.class);
				var idata = in.Introspect();
				var cg = new InterfaceCodeGenerator(disableFilter.isSelected(), idata, data.getPath(), "*", pkgName,
						boundProps);
				var analyze = cg.analyze(true);
				if (analyze == null) {
					throw new IllegalStateException("Unable to create interface files");
				}
				allMap.putAll(analyze);
			}

			writeToFile(selectedDirectory.getAbsolutePath(), allMap);

			runLater(() -> {
				var alt = new Alert(AlertType.INFORMATION);
				alt.initOwner(getContext().getWindows().get(0).stage());
				alt.setTitle("Export Complete");
				alt.setContentText(String.format("%d Java sources were created.", allMap.size()));
				alt.showAndWait();
				getTiles().remove(this);
			});
		} catch (Exception dbe) {
			LOG.error("Failed to export.", dbe);
			runLater(() -> {
				var alt = new Alert(AlertType.ERROR);
				alt.initOwner(getContext().getWindows().get(0).stage());
				alt.setTitle("Error");
				alt.setContentText(dbe.getMessage() == null ? "No message supplied." : dbe.getMessage());
				alt.showAndWait();
			});
		}
	}

	@FXML
	private void export(ActionEvent evt) {

		busy.set(true);
		new Thread(() -> execute(getOutputDirectory(), packageName.getText().equals("") ? null : packageName.getText(),
				useDBusBoundProperty.isSelected())).start();
	}

	private File getOutputDirectory() {
		return new File(
				outputDirectory.getText().equals("") ? outputDirectory.getPromptText() : outputDirectory.getText());
	}

}

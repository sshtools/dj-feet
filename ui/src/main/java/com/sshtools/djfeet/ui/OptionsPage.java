package com.sshtools.djfeet.ui;

import java.util.ResourceBundle;

import com.sshtools.jajafx.AbstractTile;
import com.sshtools.jajafx.JajaFXApp.DarkMode;
import com.sshtools.jajafx.PrefBind;
import com.sshtools.jaul.Phase;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

public class OptionsPage extends AbstractTile<DJFeetApp> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(OptionsPage.class.getName());

	private PrefBind prefBind;

	@FXML
	CheckBox automaticUpdates;
	@FXML
	CheckBox executeImmediatelyIfNoArguments;
	@FXML
	ComboBox<Phase> phase;
	@FXML
	ComboBox<DarkMode> darkMode;

	@Override
	protected void onConfigure() {
		phase.getItems().addAll(getContext().getContainer().getUpdateService().getPhases());
		darkMode.getItems().addAll(DarkMode.values());
		darkMode.getSelectionModel().select(DarkMode.AUTO);

		darkMode.setConverter(new StringConverter<DarkMode>() {
			@Override
			public String toString(DarkMode object) {
				return RESOURCES.getString("darkMode." + object.name());
			}

			@Override
			public DarkMode fromString(String string) {
				return null;
			}
		});
		phase.setConverter(new StringConverter<Phase>() {
			@Override
			public String toString(Phase object) {
				return object == null ? null : RESOURCES.getString("phase." + object.name());
			}

			@Override
			public Phase fromString(String string) {
				return null;
			}
		});

		prefBind = new PrefBind(getContext().getContainer().getAppPreferences());
		prefBind.bind(executeImmediatelyIfNoArguments);
		prefBind.bind(automaticUpdates);
		prefBind.bind(Phase.class, phase);
		prefBind.bind(DarkMode.class, darkMode);
		

	}

	@FXML
	private void back(ActionEvent evt) {
		getTiles().remove(this);

	}

	@Override
	public void close() {
		prefBind.close();
	}

}

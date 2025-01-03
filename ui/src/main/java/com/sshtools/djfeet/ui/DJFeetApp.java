package com.sshtools.djfeet.ui;

import static com.sshtools.jajafx.FXUtil.maybeQueue;

import java.util.ResourceBundle;

import com.sshtools.jajafx.AboutPage;
import com.sshtools.jajafx.JajaFXApp;
import com.sshtools.jajafx.JajaFXAppWindow;
import com.sshtools.jajafx.Tiles;
import com.sshtools.jajafx.UpdatePage;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.stage.Stage;

public class DJFeetApp extends JajaFXApp<DJFeet, JajaFXAppWindow<DJFeetApp>> {

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(DJFeet.class.getName());

	private Tiles<DJFeetApp> tiles;

	public DJFeetApp() {
		super(DJFeetApp.class.getResource("icon.png"), 
		      RESOURCES.getString("title"), (DJFeet) DJFeet.getInstance(),
		      DJFeet.getInstance().getUserPreferences());
	}

	public final Tiles<DJFeetApp> getTiles() {
		return tiles;
	}

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void needUpdate() {
		maybeQueue(() -> {
			if (!(tiles.getCurrentPage() instanceof AboutPage) && !(tiles.getCurrentPage() instanceof UpdatePage)) {
				tiles.popup(UpdatePage.class);
			}
		});
	}
	
	@Override
	public void addCommonStylesheets(ObservableList<String> stylesheets) {
		super.addCommonStylesheets(stylesheets);
        stylesheets.add(DJFeet.class.getResource("DJFeet.css").toExternalForm());
	}

	@Override
	protected Node createContent(Stage stage, JajaFXAppWindow<DJFeetApp> window) {

		tiles = new Tiles<>(this, window);
		tiles.add(ViewPage.class);
		tiles.getStyleClass().add("padded");
		return tiles;
	}
}
package com.sshtools.djfeet.ui;

import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.InvalidObjectPathException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Properties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.djfeet.ui.ArgumentData.Direction;
import com.sshtools.jajafx.PageTransition;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.WindowEvent;

public class DBusConnectionTab extends AnchorPane {

	private final static Logger LOG = LoggerFactory.getLogger(DBusConnectionTab.class);
	private final static ResourceBundle RESOURCES = ResourceBundle.getBundle(DBusConnectionTab.class.getName());
	
	@FXML
	private TextField busNameSearch;
	@FXML
	private ListView<BusData> busNames;
	@FXML
	private Button refresh;
	@FXML
	private MenuItem export;
	@FXML
	private MenuItem execute;
	@FXML
	private MenuItem monitor;
	@FXML
	private MenuItem introspect;
	@FXML
	private TreeView<BusTreeData> objects;
	@FXML
	private TextField busAddress;
	@FXML
	private TextField busName;
	@FXML
	private TextField uniqueName;
	
	private final DBusConnection connection;
	private final DBus dbus;
	private final FilteredList<BusData> filteredData;
	private final ObservableList<BusData> names;
	private final List<String> activatable;
	private final DJFeetApp context;
	private final TabPane tabs;
	
	public DBusConnectionTab(TabPane tabs, DJFeetApp context,  DBusConnection connection) throws DBusException {
		this.connection = connection;
		this.context = context;
		this.tabs = tabs;
		
		var loader = new FXMLLoader(getClass().getResource("DBusConnectionTab.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		loader.setResources(RESOURCES);
		try {
			loader.load();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		dbus = connection.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);

		/*
		 * Bus names. Note ListNames() does not seem to list all names, we must add the
		 * activatable names as well.
		 */
		var allNames = new LinkedHashSet<>(Arrays.asList(dbus.ListNames()));
		activatable = getActivatable();
		allNames.addAll(activatable);
		names = getBusNames(allNames);
		Collections.sort(names);
		filteredData = new FilteredList<>(names, s -> true);

		connection.addSigHandler(DBus.NameOwnerChanged.class, (e) -> {
			if (e.oldOwner.equals("") && !e.newOwner.equals("")) {
				var newBusData = createBusData(e.name, e.newOwner);
				runLater(() -> {
					/*
					 * Do our own sort, using Collections.sort() can cause excessive swaps, losing
					 * the selection
					 */
					var idx = names.indexOf(newBusData);
					if(idx == -1) {
						for (var i = 0; i < names.size(); i++) {
							var n = names.get(i);
							if (newBusData.compareTo(n) < 0) {
								names.add(i, newBusData);
								return;
							}
						}
						names.add(0, newBusData);
					}
					else {
						names.set(idx, newBusData);
					}
				});
			} else if (e.newOwner.equals("") && !e.oldOwner.equals("")) {
				runLater(() -> {
					for (var bdIt = names.iterator(); bdIt.hasNext();) {
						var bd = bdIt.next();
						if (e.oldOwner.equals(bd.getOwner())) {
							bdIt.remove();
							break;
						}
					}
					var newActivatable = getActivatable();
					var newBusNames = getBusNames(newActivatable);
					var newBusData = createBusData(e.name, e.newOwner);
					if(newBusNames.contains(newBusData)) {
						for (var i = 0; i < names.size(); i++) {
							var n = names.get(i);
							if (newBusData.compareTo(n) < 	0) {
								names.add(i, newBusData);
								return;
							}
						}
						names.add(0, newBusData);	
					}
				});
			} else {
				runLater(() -> {
					for (var bdIt = names.iterator(); bdIt.hasNext();) {
						var bd = bdIt.next();
						if (e.oldOwner.equals(bd.getOwner())) {
							bd.setOwner(e.newOwner);
							break;
						}
					}
				});
			}
		});
		
		busNameSearch.textProperty().addListener(obs -> {
			String filter = busNameSearch.getText();
			if (filter == null || filter.length() == 0) {
				filteredData.setPredicate(s -> true);
			} else {
				filteredData.setPredicate(s -> s.getName().toLowerCase().contains(filter.toLowerCase()));
			}
		});
		busNames.setItems(filteredData);
		busNames.setCellFactory(lv -> new BusCell());
		busNames.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> {
			update();
		});

		// Tree View
		var group = PseudoClass.getPseudoClass("group");
		objects.setRoot(new TreeItem<>(new ObjectData(null)));
		objects.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		objects.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> setAvailable());
		objects.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				TreeItem<BusTreeData> selected = objects.getSelectionModel().getSelectedItem();
				if (selected != null && selected.getValue() instanceof MethodData)
					executeMethod((MethodData) selected.getValue());
				else if (selected != null && selected.getValue() instanceof PropertyData)
					loadProperty((PropertyData) selected.getValue());
			}
		});
		objects.setCellFactory(lv -> {
			var cell = new BusTreeCell();
			cell.treeItemProperty().addListener((obs, oldTreeItem, newTreeItem) -> cell.pseudoClassStateChanged(group,
					newTreeItem != null && newTreeItem.getValue() != null && newTreeItem.getValue().isGroup()));
			return cell;
		});

		// First update
		update();
	}

	@FXML
	private void showingContextMenu(WindowEvent evt) {
		var opaths = 0;
		var meths = 0;
		var sigs = 0;
		for (var i : objects.getSelectionModel().getSelectedItems()) {
			if (i.getValue() instanceof ObjectData)
				opaths++;
			if (i.getValue() instanceof MethodData)
				meths++;
			if (i.getValue() instanceof SignalData)
				sigs++;
		}
		var emptySel = busNames.getSelectionModel().isEmpty();
		var selItems = objects.getSelectionModel().getSelectedItems().size();
		export.setDisable(emptySel || (selItems > 0 && opaths != selItems));
		introspect.setDisable(emptySel || (selItems > 0 && opaths != selItems));
		execute.setDisable(emptySel || (selItems > 0&& meths != selItems));
		monitor.setDisable(emptySel || (selItems > 0&& sigs != selItems));
	}

	private List<String> getActivatable() {
		try {
			return Arrays.asList(dbus.ListActivatableNames());
		} catch (DBusExecutionException dbe) {
			/*
			 * TODO: dbus-java is returning null here, should be empty array
			 */
			return Collections.emptyList();
		}
	}

	private void loadProperty(PropertyData propData) {
		try {
			var props = connection.getRemoteObject(propData.getInterfaceData().getObjectData().getBusData().getName(),
					propData.getInterfaceData().getObjectData().getPath(), Properties.class);
			var val = props.Get(propData.getInterfaceData().getDisplayName(), propData.getName());
			propData.setValue(val);
			objects.refresh();
		} catch (Exception e) {
			LOG.error("Failed to load property.", e);
		}
	}

	private void executeMethod(MethodData methodData) {
		var newBusPage = context.getTiles().popup(ExecutePage.class, PageTransition.FROM_RIGHT);
		newBusPage.init(connection, methodData);
		if(methodData.getArguments().isEmpty() && context.getContainer().getAppPreferences().getBoolean("executeImmediatelyIfNoArguments", true)) {
			context.getContainer().getScheduler().schedule(() -> {
				runLater(() -> newBusPage.execute(null));
			}, 1, TimeUnit.SECONDS);
		}
	}

	private ObservableList<BusData> getBusNames(Collection<String> allNames) {
		return FXCollections.observableArrayList(allNames.stream().map(n -> createBusData(n, null))
				.collect(Collectors.toList()).toArray(new BusData[0]));
	}

	private BusData createBusData(String n, String owner) {
		long pid = -1;
		String cmd = null;
		try {
			if (owner == null)
				owner = dbus.GetNameOwner(n);
			pid = dbus.GetConnectionUnixProcessID(n).longValue();
			var p = Paths.get("/proc/" + pid + "/cmdline");
			if (Files.exists(p)) {
				try (var r = Files.newBufferedReader(p)) {
					cmd = String.join(" ", r.readLine().split("\0"));
				}
			}
		} catch (Exception e) {
		}
		return new BusData(n, activatable.contains(n), pid, cmd, owner);
	}

	private void setAvailable() {
		refresh.setDisable(busNames.getSelectionModel().isEmpty());
	}

	@FXML
	private void introspect(ActionEvent evt) {
		var selected = objects.getSelectionModel().getSelectedItem();
		if (selected != null && selected.getValue() instanceof ObjectData od) {
			try {
				var path = Files.createTempFile("dbus", ".xml");
				try(var wtr = new PrintWriter(Files.newBufferedWriter(path))) {
					wtr.print(od.getIntrospectable().Introspect());
				}
				context.getHostServices().showDocument(path.toUri().toString());
			}
			catch(IOException ioe) {
				LOG.error("Failed to introspect.", ioe);
			}
		}
	}

	@FXML
	private void execute(ActionEvent evt) {
		var selected = objects.getSelectionModel().getSelectedItem();
		if (selected != null && selected.getValue() instanceof MethodData md)
			executeMethod(md);
	}
	
	@FXML
	private void monitor(ActionEvent evt) {
		var selected = objects.getSelectionModel().getSelectedItem();
		if (selected != null && selected.getValue() instanceof SignalData sd) {
			try {
				var tabContent = new MonitorTab(context, connection, MonitorTab.Type.SIGNAL, Optional.of(sd.getInterfaceData().getDisplayName()), Optional.of(sd.getDisplayName()));
				var tab = new Tab("Monitor 1", tabContent);
				tabs.getTabs().add(tab);	
			}
			catch(Exception e) {
				LOG.error("Failed to start monitor.", e);
			}
		}
	}

	@FXML
	private void export(ActionEvent evt) {
		var sel = objects.getSelectionModel().getSelectedItems().isEmpty() ? objects.getRoot().getChildren()
				: objects.getSelectionModel().getSelectedItems();
		var exportPage = context.getTiles().popup(ExportPage.class, PageTransition.FROM_RIGHT);
		exportPage.init(connection, new ArrayList<ObjectData>(sel.stream().filter(o -> o.getValue() instanceof ObjectData)
				.map(o -> (ObjectData) o.getValue()).collect(Collectors.toList())));
	}

	@FXML
	void refresh(ActionEvent evt) {
		update();
	}

	private void update() {
		var rootChildren = objects.getRoot().getChildren();
		rootChildren.clear();
		var selectedBus = busNames.getSelectionModel().getSelectedItem();
		if (selectedBus == null) {
			busAddress.setText("");
			busName.setText("");
			uniqueName.setText("");
		} else {
			try {
				visitNode(selectedBus, objects.getRoot(), objects.getRoot(), selectedBus.getName(), "/");
			} catch (DBusException e) {
				LOG.error("Failed to visit node.", e);
			}
			busAddress.setText(connection.getAddress().toString());
			busName.setText(selectedBus.getName());
			uniqueName.setText(selectedBus.getOwner());
		}
		//objects.setShowRoot(rootChildren.size() < 2);
		objects.setShowRoot(false);
		setAvailable();
	}

	private void visitNode(BusData busData, TreeItem<BusTreeData> rootTreeItem, TreeItem<BusTreeData> treeItem, String name,
			String path) throws DBusException {
		if ("/org/freedesktop/DBus/Local".equals(path)) {
			// this will disconnects us.
			return;
		}
		try {
			var e = addEntry(busData, name, path);
			treeItem.setValue(e);
			var introspectData = e.getIntrospectable().Introspect();
			var doc = Jsoup.parse(introspectData, Parser.xmlParser());
			var root = doc.root();
			if (root.childrenSize() == 0)
				return;
			else {
				if (root.childrenSize() > 1)
					System.err.println("WARNING: Unexpected 2nd node at root");
				root = root.child(0);
			}
			if (!root.tagName().equals("node")) {
				return;
			}
			TreeItem<BusTreeData> interfacesItem = null;
			for (var element : root.children()) {
				if (!treeItem.equals(rootTreeItem) && element.tagName().equals("interface")) {
					if (interfacesItem == null) {
						interfacesItem = new TreeItem<>(new GroupData("Interfaces"));
						treeItem.getChildren().add(interfacesItem);
					}
					var interfaceItem = createInterfaceNode(e, element);
					interfacesItem.getChildren().add(interfaceItem);
				} else if (element.tagName().equals("node")) {
					
					var child = new TreeItem<BusTreeData>();
					String nodeName = element.attr("name");
					if (path.endsWith("/")) {
						visitNode(busData, rootTreeItem, child, name, path + nodeName);
					} else {
						visitNode(busData, rootTreeItem, child, name, path + '/' + nodeName);
					}
					if (!child.getChildren().isEmpty())
						rootTreeItem.getChildren().add(0, child);
				}
				
	
			}
		}
		catch(InvalidObjectPathException iopa) {
			LOG.warn("Failed to get remote object for path {}", path, iopa);
		}
	}

	private TreeItem<BusTreeData> createInterfaceNode(ObjectData objectData, Element element) {
		var interfaceData = new InterfaceData(objectData, element.attr("name"));
		TreeItem<BusTreeData> interfaceItem = new TreeItem<>(interfaceData);
		TreeItem<BusTreeData> methodsItem = null;
		TreeItem<BusTreeData> signalsItem = null;
		TreeItem<BusTreeData> propertiesItem = null;
		for (var memberEl : element.children()) {
			if (memberEl.tagName().equals("method")) {
				if (methodsItem == null) {
					methodsItem = new TreeItem<>(new GroupData("Methods"));
					interfaceItem.getChildren().add(methodsItem);
				}
				methodsItem.getChildren().add(createMethodNode(interfaceData, memberEl));
			} else if (memberEl.tagName().equals("signal")) {
				if (signalsItem == null) {
					signalsItem = new TreeItem<>(new GroupData("Signals"));
					interfaceItem.getChildren().add(signalsItem);
				}
				signalsItem.getChildren().add(createSignalNode(interfaceData, memberEl));
			} else if (memberEl.tagName().equals("property")) {
				if (propertiesItem == null) {
					propertiesItem = new TreeItem<>(new GroupData("Properties"));
					interfaceItem.getChildren().add(propertiesItem);
				}
				propertiesItem.getChildren().add(createPropertiesNode(interfaceData, memberEl));
			} else if (memberEl.tagName().equals("annotation")) {
				interfaceData.getAnnotations().add(new BusAnnotation(memberEl));
			} else
				throw new UnsupportedOperationException(memberEl.tagName());
		}
		return interfaceItem;
	}

	private TreeItem<BusTreeData> createMethodNode(InterfaceData interfaceData, Element element) {
		var methodData = new MethodData(interfaceData, element.attr("name"));
		var methodItem = new TreeItem<BusTreeData>(methodData);
		for (Element memberEl : element.children()) {
			if (memberEl.tagName().equals("arg")) {
				methodData.getArguments().add(new ArgumentData(memberEl, Direction.IN));
			} else if (memberEl.tagName().equals("annotation")) {
				methodData.getAnnotations().add(new BusAnnotation(memberEl));
			} else
				throw new UnsupportedOperationException(memberEl.tagName());
		}
		return methodItem;
	}

	private TreeItem<BusTreeData> createSignalNode(InterfaceData interfaceData, Element element) {
		var signalData = new SignalData(element.attr("name"), interfaceData);
		var methodItem = new TreeItem<BusTreeData>(signalData);
		for (var memberEl : element.children()) {
			if (memberEl.tagName().equals("arg")) {
				signalData.getArguments().add(new ArgumentData(memberEl, Direction.OUT));
			} else if (memberEl.tagName().equals("annotation")) {
				signalData.getAnnotations().add(new BusAnnotation(memberEl));
			} else
				throw new UnsupportedOperationException(memberEl.tagName());
		}
		return methodItem;
	}

	private TreeItem<BusTreeData> createPropertiesNode(InterfaceData interfaceData, Element element) {
		var propertiesData = new PropertyData(interfaceData, element);
		var methodItem = new TreeItem<BusTreeData>(propertiesData);
		for (var memberEl : element.children()) {
			if (memberEl.tagName().equals("annotation")) {
				propertiesData.getAnnotations().add(new BusAnnotation(memberEl));
			} else
				throw new UnsupportedOperationException(memberEl.tagName());
		}
		return methodItem;
	}

	private ObjectData addEntry(BusData busData, String name, String path) throws DBusException {
		ObjectData entry = new ObjectData(busData);
		entry.setName(name);
		entry.setPath(path);
		Introspectable introspectable = connection.getRemoteObject(name, path, Introspectable.class);
		entry.setIntrospectable(introspectable);
		return entry;
	}
	
}

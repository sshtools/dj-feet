package com.sshtools.djfeet.ui;

import static javafx.application.Platform.runLater;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.errors.NoReply;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MessageFactory;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.sshtools.jajafx.AbstractTile;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextFlow;

public class ExecutePage extends AbstractTile<DJFeetApp> {
	final static Logger LOG = LoggerFactory.getLogger(ExecutePage.class);

	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(ExecutePage.class.getName());
	@FXML
	private TextArea methodInput;
	@FXML
	private TextArea methodOutput;
	@FXML
	private TextField busAddress;
	@FXML
	private TextField objectPath;
	@FXML
	private TextField iface;
	@FXML
	private Hyperlink execute;
	@FXML
	private TextFlow signature;

	private DBusConnection conn;
	private MessageFactory factory;
	private SpelParserConfiguration config;
	private ConstructorResolver resolver;
	private MethodData methodData;
	private final BooleanProperty busy = new SimpleBooleanProperty();

	void init(DBusConnection conn, MethodData methodData) {
		this.conn = conn;
		this.methodData = methodData;

		if(methodData.getArguments().isEmpty()) {
			methodInput.setPromptText(RESOURCES.getString("noArgs"));
			methodInput.setDisable(true);
		}
		else {
			methodInput.setDisable(false);
			methodInput.setPromptText(RESOURCES.getString("expression"));
		}
		busAddress.setText(methodData.getInterfaceData().getObjectData().getBusData().getName());
		objectPath.setText(methodData.getInterfaceData().getObjectData().getPath());
		iface.setText(methodData.getInterfaceData().getDisplayName());
		signature.getChildren().add(AnnotatbleData.colorText(methodData.getDisplayName(), null));
		signature.getChildren().addAll(methodData.argumentsText());

		factory = new MessageFactory(conn.getTransportConfig().getEndianess());

		config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, this.getClass().getClassLoader());

		resolver = (context, typeName, argumentTypes) -> {
			if (typeName.equalsIgnoreCase("variant")) {
				if (argumentTypes.size() == 1) {
					return new ConstructorExecutor() {
						@Override
						public TypedValue execute(EvaluationContext context, Object... arguments)
								throws AccessException {
							return new TypedValue(new Variant<>(arguments[0]));
						}
					};
				} else if (argumentTypes.size() == 2) {
					return new ConstructorExecutor() {
						@Override
						public TypedValue execute(EvaluationContext context, Object... arguments)
								throws AccessException {
							return new TypedValue(new Variant<>(arguments[0], (String) arguments[1]));
						}
					};
				}
			}
			return null;
		};

		execute.disableProperty().bind(busy);
		methodInput.disableProperty().bind(busy);
		
	}

	@Override
	protected void onConfigure() {

	}
	
	@FXML
	private void spel(MouseEvent evt) {
		getContext().getHostServices().showDocument("https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/expressions.html");
	}

	@FXML
	private void back(ActionEvent evt) {
		getTiles().remove(this);
	}

	@FXML
	void execute(ActionEvent evt) {
		busy.set(true);
		new Thread(this::execute).start();
	}

	private void execute() {

		try {
			Object[] inputArgs = new Object[0];
			if (methodInput.getText().length() > 0) {
				var parser = new SpelExpressionParser(config);
				var evalContext = new StandardEvaluationContext();
				evalContext.addConstructorResolver(resolver);
				var exp = parser.parseExpression("new Object[] { " + methodInput.getText() + " }");
				inputArgs = (Object[]) exp.getValue(evalContext);
			}
			
			if (inputArgs.length != methodData.getInputArguments().size()) {
				throw new IllegalStateException("Incorrect number of parameters. Expected " + methodData.getInputArguments().size()
							+ ", got " + inputArgs.length);
			}
			
			var sig = getInputSignature(methodData);
			var call = factory.createMethodCall(methodData.getInterfaceData().getObjectData().getBusData().getName(),
					methodData.getInterfaceData().getObjectData().getPath(),
					methodData.getInterfaceData().getDisplayName(), methodData.getDisplayName(), (byte) 0,
					inputArgs.length == 0 ? null : sig, inputArgs.length == 0 ? null : inputArgs);
			conn.sendMessage(call);

			Message reply = call.getReply();
			if (null == reply) {
				throw new NoReply("No reply within specified time");
			}

			if (reply instanceof org.freedesktop.dbus.messages.Error e) {
				e.throwException();
			}

			var results = Arrays.asList(reply.getParameters());
			runLater(() -> {
				methodOutput.setStyle("");
				if (results.isEmpty())
					methodOutput.setText("This method did not return anything.");
				else {
					methodOutput.setText(
							String.join(",", results.stream().map(o -> format(o)).collect(Collectors.toList())));
				}
				busy.set(false);
			});
		} catch (Throwable see) {
			LOG.error("Failed to execute remote method.", see);
			runLater(() -> {
				methodOutput.setStyle("-fx-text-fill: red;");
				methodOutput.setText(see.getClass().getName() + ": " + see.getMessage());
				busy.set(false);
			});
		}

	}

	static String getInputSignature(MethodData data) {
		return String.join("", data.getInputArguments().stream().map(s -> s.getType()).collect(Collectors.toList()));
	}

	@SuppressWarnings("unchecked")
	static String format(Object o) {
		if (o == null)
			return "null";
		if (o instanceof Map.Entry) {
			var en = (Map.Entry<Object, Object>) o;
			return format(en.getKey()) + ":" + format(en.getValue());
		} else if (o instanceof String)
			return "\"" + ((String) o).replace("\"", "\\\"") + "\"";
		else if (o instanceof Map) {
			return "{" + String.join(", ",
					((Map<Object, Object>) o).entrySet().stream().map(ob -> format(ob)).collect(Collectors.toList()))
					+ "}";
		} else if (o instanceof Collection) {
			return "[" + String.join(", ",
					((Collection<Object>) o).stream().map(ob -> format(ob)).collect(Collectors.toList())) + "]";
		}
		if (o.getClass().isArray()) {
			return "[" + String.join(", ", (Collection<String>) (Arrays.asList((Object[]) o)).stream()
					.map(ob -> format(ob)).collect(Collectors.toList())) + "]";
		}
		return String.valueOf(o);
	}

}

package com.sshtools.djfeet.daemon;

import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("com.sshtools.djfeet.DBus")
public interface DJFeetDBus extends DBusInterface {

	void ReloadConfig();
	
	/* NOTE: This is here due to incorrect interface specification in DBus interface */
	/* TODO: Report this upstream */
	void UpdateActivationEnvironment(Map<String, String> env);
}

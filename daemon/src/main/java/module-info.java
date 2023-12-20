module com.sshtools.djfeet.daemon {
	requires transitive org.freedesktop.dbus;
	requires java.prefs;
	//requires static org.freedesktop.dbus.transport.jre;
	requires transitive static org.freedesktop.dbus.transport.junixsocket;
	requires static org.freedesktop.dbus.transport.tcp;
	requires info.picocli; 
	requires com.sshtools.jaul;
	requires com.sshtools.jini;
	requires org.slf4j.simple;
	exports com.sshtools.djfeet.daemon;
	opens com.sshtools.djfeet.daemon;
}
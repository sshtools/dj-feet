package com.sshtools.djfeet.ui;

import com.sshtools.jajafx.JajaApp;
import com.sshtools.jajafx.updateable.AppUpdateService;
import com.sshtools.jajafx.updateable.DelegatingAppUpdateService;
import com.sshtools.jajafx.updateable.UpdateableJajaApp;
import com.sshtools.jajafx.updateable.UpdateableJajaFXAppWindow;
import com.sshtools.jaul.AppCategory;
import com.sshtools.jaul.ArtifactVersion;
import com.sshtools.jaul.JaulApp;
import com.sshtools.jaul.Phase;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;

@Command(name = "dj-feet", mixinStandardHelpOptions = true, description = "Debug DBus Applications", versionProvider = DJFeet.Version.class)
@JaulApp(id = DJFeet.TOOLBOX_APP_ID, category = AppCategory.GUI, updaterId = "54", updatesUrl = "https://sshtools-public.s3.eu-west-1.amazonaws.com/dj-feet/${phase}/updates.xml")
public class DJFeet extends UpdateableJajaApp<DJFeetApp, UpdateableJajaFXAppWindow<DJFeetApp>> {

	public final static class PrefectBuilder extends UpdateableJajaAppBuilder<DJFeet, PrefectBuilder, DJFeetApp> {

		public static PrefectBuilder create() {
			return new PrefectBuilder();
		}

		private PrefectBuilder() {
		}

		@Override
		public DJFeet build() {
			return new DJFeet(this);
		}
	}

	public final static class Version implements IVersionProvider {

		@Override
		public String[] getVersion() throws Exception {
			return new String[] { ArtifactVersion.getVersion("com.sshtools", "dj-feet") };
		}
	}

	public final static String TOOLBOX_APP_ID = "com.sshtools.DJFeet";

	public static void main(String[] args) {
		var bldr = PrefectBuilder.create().withInceptionYear(2023).withApp(DJFeetApp.class)
				.withAppResources(DJFeetApp.RESOURCES);
		System.exit(new CommandLine(bldr.build()).execute(args));
	}

	DJFeet(PrefectBuilder builder) {
		super(builder);
	}

	public static DJFeet getDJFeetInstance() {
		return (DJFeet) getInstance();
	}

	@Override
	protected AppUpdateService createDefaultUpdateService() {
		return new DelegatingAppUpdateService(super.createDefaultUpdateService()) {

			@Override
			public Phase[] getPhases() {
				return Phase.values();
			}

			@Override
			public boolean isUpdatesEnabled() {
				return true;
			}
		};
	}

}

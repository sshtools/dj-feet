# ![Bus Icon](src/main/web/bus-small.png  "Bus Icon")dj-feet  

A cross-platform, JavaFX based clone of [D-Feet](https://wiki.gnome.org/Apps/DFeet/), the D-Bus debugger provided by GNOME on Linux. It makes use of [dbus-java](https://github.com/hypfvieh/dbus-java), allowing you to debug your D-Bus application on any major desktop operating system.

![Screenshot](src/main/web/screenshot.png  "Screenshot")

## Announcement

### 20/12/2003

DJ-Feet is now hosted by my employer [JADAPTIVE](https://jadaptive.com). It will remain free
and open source, this change is to just take advantage of JADAPTIVES code signing and build
infrastructure.    

## Installation

TODO
 
### Run From Source

To run from source, you will need at least version *3.8.3* of [Apache Maven](https://maven.apache.org/) and a Java 17 development kit. I recommended downloading from [Adoptium](https://adoptium.net/).

Simply run the following from the directory you clone to.

```
mvn javafx:run
```

## Usage

What is available when you start the application will depend on the operating system you are on. 

 * On any operating system, if you have installed the provided default `dbus-java-daemon`, it will also be shown.
 
 * Linux, you will see the *System Bus*, and the *Session Bus* by default. 
 
 * If there are no default buses available, you will be immediately prompted for a bus address.

Other features include :-

 * Export existing DBus services as Java code (uses dbus-java's code generator) 

 * Filter bus names by typing in the search box at the top-left of the user interface.
 
 * Select a bus in the left hand pane to view the available objects exported on that base.
 
 * Expand the tree nodes on the right to view *Methods*, *Signals* and *Properties*. 
 
 * Double-click on a *Method* to execute it. The *Method Input* box accepts multiple comma separated parameters, you must supply all arguments a Method expects. Arguments are parsed using the [Spring Expression Language](https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/expressions.html). In practice, format arguments similiarly to you would using Java.
   
   ![Execute](src/main/web/execute.png  "Execute")
    
 * Double-click on a *Property* to view it's current value. Values cannot currently be set.

### Other Tools Installed

With the installers, also included are easy launchers for all of the [dbus-java-utils](https://github.com/hypfvieh/dbus-java/tree/master/dbus-java-utils) tools.

 * `dbus-java-list`. Just list all exported names.
 * `dbus-java-caller`. Invoke dbus methods.          
 * `dbus-java-create-interface`. Geneate a Java interface from exported objects. DEPRECATED
 * `dbus-java-interface-code-generator`. Geneate a Java interface from exported objects.
 * `dbus-java-viewer`. Simple Swing UI based viewer.
 * `dbus-java-type-string-to-java`. Convert a DBus type string to Java.
 
 
## Changes

### 2.0.0

 * Daemon now a separate package
 * Now developed by JADAPTIVE.   
 * Built-in updater.
 * Ability to access remote buses over SSH

### 1.1.0

 * Use (in development) dbus-java-5.0.0.
 * Now uses JPMS
 * Improvements to export. Option to use `@DBusBoundProperty`, Disable Filter and set alternative package name (now in upstream)
 * Now uses [JMetro](https://www.pixelduke.com/java-javafx-theme-jmetro/) as skin. Looks more at home on Windows.
 * Several fixes for method executing, now supports `new variant(<val>[,'<sig>'])`. See [#2](https://github.com/bithatch/dj-feet/issues/2)

### 1.1.0

 * Use official release of dbus-java-4.0.0. 

### 1.0.2

 * Windows dj-feet not automatically loading `dbus-java-daemon` if it is installed.
 * Re-install may fail on Windows due to JRE file being in use.
 * Better internal monitoring of bus events.

### 1.0.1

 * Windows had incorrect classpath for dj-feet.exe.

### 1.0.0

 * Initial release

## Development

Find the source and submit pull requests at the GitHub project page, you know the drill.

## License

dj-feet is made available under [GPL version 3](https://www.gnu.org/licenses/gpl-3.0.en.html). Other components under their respective licenses. 

## Attribution

Bus Icon made by [Royyan Wijaya](https://www.flaticon.com/authors/royyan-wijaya) from [www.flaticon.com](https://www.flaticon.com/)

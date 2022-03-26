# AutoRegisterLib

## A Library For automating the registration of various plugin functions

### Repository
```xml
<repository>
    <id>miketheshadow.repo</id>
    <url>https://maven.miketheshadow.ca</url>
</repository>
```
### Dependency
```xml
<dependency>
    <groupId>com.miketheshadow.autoregister</groupId>
    <artifactId>AutoRegister</artifactId>
    <version>0.4.1</version>
</dependency>
```
#
### Current Features

| Features                   | 
|----------------------------|
| Command Registration       |
| Listener Registration      |
| Full Command Registration  |
| AutoWired plugin injection |
#
### Planned Features

| Planned | 
|---------|
| ???     |


### Usage/Examples
#
#### Initializing AutoRegister
```java
public final class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Creating an auto-register for the base package the plugin is contained in
        AutoRegister autoRegister = new AutoRegister(this,"com.miketheshadow.exampleplugin");
        
        /* 
        Running default setup registers all classes annotated with 
        the command annotations or any listeners
        */
        autoRegister.defaultSetup();
    }
    
}
```
#
### Annotating a command class

```java

@RegisterCommand(commandName = "test")
public class TestCommand implements CommandExecutor {

    // Example of plugin injection into command class (also works for listeners)
    @InjectPlugin
    ExamplePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "Testing 123!");
        return true;
    }
}
```
#
### Plugin auto-injection

```java
import com.miketheshadow.autoregister.annotations.InjectPlugin;

public class ExampleClass {

    //Only works on static fields in regular classes
    @InjectPlugin
    private static ExamplePlugin plugin;
}
```

package queuectl.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import queuectl.db.ConfigRepository;

@Command(name = "config", description = "Get or set configuration values",
        subcommands = { ConfigCommand.Get.class, ConfigCommand.Set.class })
public class ConfigCommand implements Runnable {
    @Override public void run() { System.out.println("Use 'config get' or 'config set'"); }

    @Command(name = "get", description = "Get configuration value")
    static class Get implements Callable<Integer> {
        @Parameters(index = "0", description = "config key", arity = "0..1")
        String key;

        public Integer call() throws Exception {
            if (key == null) {
                System.out.println("max_retries=" + ConfigRepository.getString("max_retries", "3"));
                System.out.println("backoff_base=" + ConfigRepository.getString("backoff_base", "2"));
            } else {
                System.out.println(key + "=" + ConfigRepository.getString(key, ""));
            }
            return 0;
        }
    }

    @Command(name = "set", description = "Set configuration value")
    static class Set implements Callable<Integer> {
        @Parameters(index = "0", description = "config key")
        String key;
        @Parameters(index = "1", description = "config value")
        String value;

        public Integer call() throws Exception {
            ConfigRepository.set(key, value);
            System.out.println("Set " + key + " = " + value);
            return 0;
        }
    }
}
package com.link.up.launcher;

/**
 * Runs a local bounded sync job from a HOCON configuration file.
 *
 * <p>For example:
 *
 * <pre>
 * java com.link.up.launcher.LauncherConfigurationExample \
 *     examples/jdbc-single-table.conf
 * </pre>
 */
public final class LauncherConfigurationExample {
    private LauncherConfigurationExample() {
    }

    public static void main(String[] args) throws Exception {
        LocalSyncLauncher.main(args);
    }
}

/*
 * This file is part of FoxBotNG.
 *
 * FoxBotNG is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBotNG is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FoxBotNG.  If not, see <http://www.gnu.org/licenses/>.
 */

package co.foxdev.foxbotng.api;

import co.foxdev.foxbotng.FoxBotNG;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class PluginManager {
    private static final FileFilter JAR_FILE_FILTER = pathname -> pathname.getAbsolutePath().endsWith(".jar");
    private final Map<Plugin, Object> plugins = new HashMap<>();
    private Method addUrl;

    public PluginManager() throws IOException {
        loadPlugins();
        Runtime.getRuntime().addShutdownHook(new PluginShutdownHook());
    }

    private void loadFile(File file) throws Exception {
        if (addUrl == null) {
            addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
        }
        addUrl.invoke(ClassLoader.getSystemClassLoader(), file.toURI().toURL());
    }

    private HashSet<String> getClasses(File file) {
        HashSet<String> found = new HashSet<>();
        try (FileInputStream inFile = new FileInputStream(file.getAbsoluteFile())) {
            try (ZipInputStream zip = new ZipInputStream(inFile)) {
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        String name = entry.getName().replace('/', '.');
                        found.add(name.substring(0, name.lastIndexOf(".")));
                    }
                }
            } catch (IOException ex) {
                log.error("Error while opening {} as a ZipInputStream", file.getAbsolutePath(), ex);
            }
        } catch (IOException ex) {
            log.error("Error while opening {} as a FileInputStream", file.getAbsolutePath(), ex);
        }
        return found;
    }

    private void loadPlugins() throws IOException {
        FoxBotNG bot = FoxBotNG.getInstance();

        File pluginsDir = new File(bot.getConfigManager().getConfigDir(), "plugins");
        if (!pluginsDir.exists() && !pluginsDir.mkdirs()) {
            throw new IOException("Could not create plugin directory.");
        }

        log.debug("Plugin directory is {}", pluginsDir.getAbsolutePath());

        Map<File, Set<String>> jarData = new HashMap<>();
        File[] jarFiles;
        if ((jarFiles = pluginsDir.listFiles(JAR_FILE_FILTER)) == null) {
            log.debug("No jar files found in {}", pluginsDir.getAbsolutePath());
            return;
        }

        for (File file : jarFiles) {
            log.debug("Scanning {} for classes.", file.getName());
            Set<String> classes = getClasses(file);
            jarData.put(file, classes);
            log.debug("Found {} class files in {}.", classes.size(), file.getName());

            try {
                loadFile(file);
            } catch (Exception ex) {
                log.error("Error loading {}", file.getName(), ex);
            }
        }

        for (Map.Entry<File, Set<String>> entry : jarData.entrySet()) {
            Set<String> classNames = entry.getValue();
            for (String className : classNames) {
                Class c;
                try {
                    c = this.getClass().getClassLoader().loadClass(className);
                } catch (ClassNotFoundException ex) {
                    continue;
                }

                if (c.isAnnotationPresent(Plugin.class)) {
                    log.debug("Found main class: {}", c.getName());
                    for (Annotation annotation : c.getAnnotations()) {
                        if (annotation instanceof Plugin) {
                            Plugin pl = (Plugin) annotation;
                            if (plugins.containsKey(pl)) {
                                log.warn("Duplicate plugin name '{}', not loading.", pl.name());
                                break;
                            }

                            Object instance;
                            try {
                                instance = c.newInstance();
                            } catch (InstantiationException | IllegalAccessException ex) {
                                log.error("Error instantiating class {}", c.getName(), ex);
                                break;
                            }

                            if (instance instanceof PluginBase) {
                                log.info("Loaded {} {}", pl.name(), pl.version());
                                PluginBase plugin = (PluginBase) instance;
                                plugin.onEnable();
                                plugins.put(pl, plugin);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns an unmodifiable copy of the bot's plugins list.
     * @return Unmodifiable copy of a Map<Plugin, Object>
     */
    public final Map<Plugin, Object> getPlugins() {
        return Collections.unmodifiableMap(plugins);
    }

    private class PluginShutdownHook extends Thread {
        public void run() {
            for (Plugin plugin : plugins.keySet()) {
                log.debug("PluginShutdownHook triggered.");
                log.info("Disabling {} {}", plugin.name(), plugin.version());
                ((PluginBase) plugins.get(plugin)).onDisable();
            }
        }
    }
}

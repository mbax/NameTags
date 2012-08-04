/* Copyright 2012 Matt Baxter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitteh.nametags;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.kitteh.tag.PlayerReceiveNameTagEvent;
import org.kitteh.tag.TagAPI;

public class NameTags extends JavaPlugin implements Listener {

    private enum Color {
        black,
        dark_blue,
        dark_green,
        dark_aqua,
        dark_red,
        dark_purple,
        gold,
        gray,
        dark_gray,
        blue,
        green,
        aqua,
        red,
        light_purple,
        yellow;

        private final String node;
        private final ChatColor color;

        Color() {
            this.color = ChatColor.valueOf(this.name().toUpperCase());
            this.node = "nametags.color." + this.name();
        }

        public ChatColor getColor() {
            return this.color;
        }

        public String getNode() {
            return this.node;
        }

    }

    private enum Format {
        magic,
        bold,
        strikethrough,
        underline,
        italic;
        private final String node;
        private final ChatColor color;

        Format() {
            this.color = ChatColor.valueOf(this.name().toUpperCase());
            this.node = "nametags.format." + this.name();
        }

        public ChatColor getColor() {
            return this.color;
        }

        public String getNode() {
            return this.node;
        }
    }

    private static final String METADATA_NAME = "nametags.displayname";

    private boolean setDisplayName;
    private boolean setTabName;
    private File configFile;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length > 0) && args[0].equalsIgnoreCase("reload")) {
            this.load();
            sender.sendMessage("NameTags reloaded.");
        }
        return true;
    }

    @Override
    public void onDisable() {
        for (final Player player : this.getServer().getOnlinePlayers()) {
            if ((player != null) && player.isOnline()) {
                player.removeMetadata(NameTags.METADATA_NAME, this);
            }
        }
    }

    @Override
    public void onEnable() {
        if (!this.getServer().getPluginManager().isPluginEnabled("TagAPI")) {
            this.getLogger().severe("TagAPI required. Get it at http://dev.bukkit.org/server-mods/tag/");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.configFile = new File(this.getDataFolder(), "config.yml");
        this.load();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        this.calculate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onNameTag(PlayerReceiveNameTagEvent event) {
        for (final MetadataValue value : event.getNamedPlayer().getMetadata(NameTags.METADATA_NAME)) {
            if (value.getOwningPlugin().equals(this)) {
                event.setTag(value.asString());
                break;
            }
        }
    }

    private void calculate(Player player) {
        final StringBuilder name = new StringBuilder();
        final List<Color> colors = Arrays.asList(Color.values());
        Collections.shuffle(colors);
        for (final Color color : colors) {
            if (player.hasPermission(color.getNode())) {
                name.append(color.getColor());
                break;
            }
        }
        final List<Format> formats = Arrays.asList(Format.values());
        Collections.shuffle(formats);
        for (final Format format : formats) {
            if (player.hasPermission(format.getNode())) {
                name.append(format.getColor());
                break;
            }
        }
        name.append(player.getName());
        if (name.length() > 16) {
            name.setLength(16);
        }
        final String newName = name.toString();
        player.setMetadata(NameTags.METADATA_NAME, new FixedMetadataValue(this, newName));
        if (this.setDisplayName) {
            player.setDisplayName(newName);
        }
        if (this.setTabName) {
            player.setPlayerListName(newName);
        }
    }

    private void load() {
        if (!this.configFile.exists()) {
            this.saveDefaultConfig();
        }
        this.setDisplayName = this.getConfig().getBoolean("setDisplayName", false);
        this.setTabName = this.getConfig().getBoolean("setTabName", false);
        for (final Player player : this.getServer().getOnlinePlayers()) {
            if ((player != null) && player.isOnline()) {
                this.calculate(player);
                TagAPI.refreshPlayer(player);
            }
        }
    }

}

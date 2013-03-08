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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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
        aqua,
        black,
        blue,
        dark_aqua,
        dark_blue,
        dark_gray,
        dark_green,
        dark_purple,
        dark_red,
        gold,
        gray,
        green,
        light_purple,
        red,
        yellow;

        private final ChatColor color;
        private final String node;

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
        
		public String getName() {
			return this.name().toUpperCase();
		}

    }

    private enum Format {
        bold,
        italic,
        magic,
        strikethrough,
        underline;

        private final ChatColor color;
        private final String node;

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
        public String getName() {
			return this.name().toUpperCase();
		}
    }

    private static final String CONFIG_BASECOLOR = "baseColor";
    private static final String CONFIG_BASECOLOR_DEFAULT = "white";
    private static final String CONFIG_NOLONGNAMES = "noChangeForLongNames";
    private static final String CONFIG_ONLYSAME = "onlySeeSame";
    private static final String CONFIG_REFRESH = "refreshAutomatically";
    private static final String CONFIG_SET_DISPLAYNAME = "setDisplayName";
    private static final String CONFIG_SET_TABNAME = "setTabName";
    private static final String METADATA_NAME = "nametags.displayname";
    private static final String CONFIG_SET_IGNORED = "IngoredPlayers";

    private File configFile;
    private int refreshTaskID;
    private boolean setDisplayName;
    private boolean setTabName;
    private boolean noLongNames;
    private boolean onlySeeSelf;
    private ChatColor baseColor;
	private static HashMap<String, String> _formats;
	private static HashMap<String, String> _colors;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length > 0) && args[0].equalsIgnoreCase("reload")) {
            this.load();
            sender.sendMessage("Reloaded!");
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
        _colors = new HashMap<String, String>();
		_formats = new HashMap<String, String>(); 
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
        final String tag = this.getDisplay(event.getNamedPlayer());
        if (tag != null) {
            if (this.onlySeeSelf && !event.getNamedPlayer().hasPermission("nametags.seenalways")) {
                final String otherTag = this.getDisplay(event.getPlayer());
                if (otherTag == null) {
                    event.setTag((this.baseColor != null ? this.baseColor : "") + event.getNamedPlayer().getName());
                    return;
                }
                final int ionamed = tag.indexOf(event.getNamedPlayer().getName());
                final int iosee = otherTag.indexOf(event.getPlayer().getName());
                if ((ionamed <= 0) || (ionamed != iosee) || !tag.substring(0, ionamed).equals(otherTag.substring(0, iosee))) {
                    event.setTag((this.baseColor != null ? this.baseColor : "") + event.getNamedPlayer().getName());
                    return;
                }
            }
            event.setTag(tag);
        }
    }

    private void calculate(Player player) {
        StringBuilder name = new StringBuilder();
        final List<Color> colors = Arrays.asList(Color.values());
        Collections.shuffle(colors);
        final List<Format> formats = Arrays.asList(Format.values());
        Collections.shuffle(formats);
        String nick = player.getName().toLowerCase();
		if (_colors.containsKey(nick)
				|| _formats.containsKey(nick)) {
			
			if (_colors.containsKey(nick)) {
				for (final Color color : colors) {
					if (color.getName().equals(_colors.get(nick))) {
						name.append(color.getColor());
						break;
					}
				}
			}
			if (_formats.containsKey(nick)){
				for (final Format format : formats) {
					if(format.getName().equals(_formats.get(nick))){
						name.append(format.getColor());
						break;
					}
				}
			}
		} else { 
        for (final Color color : colors) {
            if (player.hasPermission(color.getNode())) {
                name.append(color.getColor());
                break;
            }
        }
        if ((name.length() == 0) && (this.baseColor != null)) {
            name.append(this.baseColor);
        }
        
        for (final Format format : formats) {
            if (player.hasPermission(format.getNode())) {
                name.append(format.getColor());
                break;
            }
        }
		}
        name.append(player.getName());
        if (name.length() > 16) {
            if (this.noLongNames) {
                name = new StringBuilder().append(player.getName());
            } else {
                name.setLength(16);
            }
        }
        final String newName = name.toString();
        player.setMetadata(NameTags.METADATA_NAME, new FixedMetadataValue(this, newName));
        if (this.setDisplayName) {
            player.setDisplayName(newName + ChatColor.RESET);
        }
        if (this.setTabName) {
            player.setPlayerListName(newName);
        }
    }

    private String getDisplay(Player player) {
        for (final MetadataValue value : player.getMetadata(NameTags.METADATA_NAME)) {
            if (value.getOwningPlugin().equals(this)) {
                return value.asString();
            }
        }
        return null;
    }

    private void load() {
    	_colors.clear();
    	_formats.clear();
    	if (getConfig().contains(NameTags.CONFIG_SET_IGNORED)) {
			ConfigurationSection sec = getConfig().getConfigurationSection(
					NameTags.CONFIG_SET_IGNORED);
			Set<String> niks = sec.getKeys(false);
			Iterator<String> iniks = niks.iterator();
			while (iniks.hasNext()) {
				String nick = iniks.next().toLowerCase();
				if (sec.contains(nick + ".color")) {
					_colors.put(nick, sec.getString(nick + ".color")
							.toUpperCase());
				}
				if (sec.contains(nick + ".format")) {
					_formats.put(nick, sec.getString(nick + ".format")
							.toUpperCase());
				}
			}
		} 
        if (this.refreshTaskID != -1) {
            this.getServer().getScheduler().cancelTask(this.refreshTaskID);
            this.refreshTaskID = -1;
        }
        if (!this.configFile.exists()) {
            this.saveDefaultConfig();
        }
        this.reloadConfig();
        if (!this.getConfig().contains(NameTags.CONFIG_BASECOLOR)) {
            this.getConfig().set(NameTags.CONFIG_BASECOLOR, NameTags.CONFIG_BASECOLOR_DEFAULT);
        }
        if (!this.getConfig().contains(NameTags.CONFIG_NOLONGNAMES)) {
            this.getConfig().set(NameTags.CONFIG_NOLONGNAMES, false);
        }
        if (!this.getConfig().contains(NameTags.CONFIG_ONLYSAME)) {
            this.getConfig().set(NameTags.CONFIG_ONLYSAME, false);
        }
        if (!this.getConfig().contains(NameTags.CONFIG_REFRESH)) {
            this.getConfig().set(NameTags.CONFIG_REFRESH, false);
        }
        if (!this.getConfig().contains(NameTags.CONFIG_SET_DISPLAYNAME)) {
            this.getConfig().set(NameTags.CONFIG_SET_DISPLAYNAME, false);
        }
        if (!this.getConfig().contains(NameTags.CONFIG_SET_TABNAME)) {
            this.getConfig().set(NameTags.CONFIG_SET_TABNAME, false);
        }
        this.saveConfig();
        if (this.getConfig().getBoolean(NameTags.CONFIG_REFRESH, false)) {
            this.refreshTaskID = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    NameTags.this.playerRefresh();
                }
            }, 1200, 1200);
        }
        final boolean newSetDisplayName = this.getConfig().getBoolean(NameTags.CONFIG_SET_DISPLAYNAME, false);
        final boolean forceDisplayName = this.setDisplayName && !newSetDisplayName;
        final boolean newSetTabName = this.getConfig().getBoolean(NameTags.CONFIG_SET_TABNAME, false);
        final boolean forceTabName = this.setTabName && !newSetTabName;
        if (forceDisplayName || forceTabName) {
            for (final Player player : this.getServer().getOnlinePlayers()) {
                if (forceDisplayName) {
                    player.setDisplayName(player.getName());
                }
                if (forceTabName) {
                    player.setPlayerListName(player.getName());
                }
            }
        }
        ChatColor newBaseColor;
        try {
            newBaseColor = ChatColor.valueOf(this.getConfig().getString(NameTags.CONFIG_BASECOLOR, "white").toUpperCase());
        } catch (final Exception e) {
            newBaseColor = null;
        }
        this.baseColor = newBaseColor == ChatColor.WHITE ? null : newBaseColor;
        this.setDisplayName = newSetDisplayName;
        this.setTabName = newSetTabName;
        this.noLongNames = this.getConfig().getBoolean(NameTags.CONFIG_NOLONGNAMES, false);
        this.onlySeeSelf = this.getConfig().getBoolean(NameTags.CONFIG_ONLYSAME, false);
        this.getServer().getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                NameTags.this.playerRefresh();
            }
        }, 2);
    }

    private void playerRefresh() {
        for (final Player player : this.getServer().getOnlinePlayers()) {
            if ((player != null) && player.isOnline()) {
                final String oldTag = this.getDisplay(player);
                this.calculate(player);
                final String newTag = this.getDisplay(player);
                final boolean one = (oldTag == null) && (newTag != null);
                final boolean two = (oldTag != null) && (newTag == null);
                final boolean three = ((oldTag != null) && (newTag != null)) && !oldTag.equals(newTag);
                if (one || two || three) {
                    TagAPI.refreshPlayer(player);
                }
            }
        }
    }

}

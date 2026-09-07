package com.customskin.offline.command;

import com.customskin.offline.network.NetworkHandler;
import com.customskin.offline.network.PacketResetSkin;
import com.customskin.offline.network.PacketSyncSkin;
import com.customskin.offline.skin.MojangSkinFetcher;
import com.customskin.offline.skin.SkinData;
import com.customskin.offline.skin.SkinStorage;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CommandSkin extends CommandBase {

    @Override
    public String getName() {
        return "skin";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/skin set <url|username> [default|slim] | /skin clear | /skin debug";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();

        if ("debug".equals(sub)) {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            UUID uuid = player.getUniqueID();
            SkinData stored = SkinStorage.get().getSkin(uuid);

            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Offline Skins Diagnostic ==="));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Player: " + TextFormatting.WHITE + player.getName()));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "UUID: " + TextFormatting.WHITE + uuid.toString()));
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Total Skins in Server DB: " + TextFormatting.WHITE + SkinStorage.get().getAllSkins().size()));
            if (stored != null) {
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Server Skin Configured:"));
                sender.sendMessage(new TextComponentString(TextFormatting.GRAY + " - URL: " + stored.getUrl()));
                sender.sendMessage(new TextComponentString(TextFormatting.GRAY + " - Model: " + stored.getModel()));
                NetworkHandler.sendTo(new PacketSyncSkin(uuid, stored.getUrl(), stored.getModel()), player);
                sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Re-sent skin packet to your client."));
            } else {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "No skin entry currently registered in server database for you."));
            }
            return;
        }

        if ("clear".equals(sub)) {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            SkinStorage.get().clearSkin(player.getUniqueID());
            NetworkHandler.sendToAll(new PacketResetSkin(player.getUniqueID()));
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[OfflineSkins] Your skin has been reset to default."));
            return;
        }

        if ("set".equals(sub)) {
            if (args.length < 2) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /skin set <url|username> [default|slim]"));
                return;
            }
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            String input = args[1];
            String model = args.length >= 3 ? args[2] : null;

            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[OfflineSkins] Fetching skin data for: " + input + "..."));
            applySkinAsync(server, sender, player.getUniqueID(), player.getName(), input, model);
            return;
        }

        if ("admin".equals(sub)) {
            if (!sender.canUseCommand(2, "skin")) {
                throw new CommandException("commands.generic.permission");
            }
            if (args.length < 3) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /skin admin <set|clear> <player> [url|username] [default|slim]"));
                return;
            }

            String adminAction = args[1].toLowerCase();
            EntityPlayerMP target = getPlayer(server, sender, args[2]);

            if ("clear".equals(adminAction)) {
                SkinStorage.get().clearSkin(target.getUniqueID());
                NetworkHandler.sendToAll(new PacketResetSkin(target.getUniqueID()));
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Cleared skin for " + target.getName()));
                target.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Your skin was cleared by an administrator."));
            } else if ("set".equals(adminAction)) {
                if (args.length < 4) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /skin admin set <player> <url|username> [default|slim]"));
                    return;
                }
                String input = args[3];
                String model = args.length >= 5 ? args[4] : null;

                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Fetching skin for " + target.getName() + "..."));
                applySkinAsync(server, sender, target.getUniqueID(), target.getName(), input, model);
            }
            return;
        }

        sendHelp(sender);
    }

    private void applySkinAsync(MinecraftServer server, ICommandSender sender, UUID targetUuid, String targetName, String input, String modelOverride) {
        CompletableFuture.runAsync(() -> {
            try {
                SkinData data = MojangSkinFetcher.resolve(input, modelOverride);
                server.addScheduledTask(() -> {
                    SkinStorage.get().setSkin(targetUuid, data);
                    NetworkHandler.sendToAll(new PacketSyncSkin(targetUuid, data.getUrl(), data.getModel()));
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[OfflineSkins] Skin successfully applied for " + targetName + "!"));
                    sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "URL: " + data.getUrl() + " | Model: " + data.getModel()));
                });
            } catch (Exception e) {
                server.addScheduledTask(() -> {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "[OfflineSkins] Error: " + e.getMessage()));
                });
            }
        });
    }

    private void sendHelp(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Offline Skins Commands ==="));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "/skin set <url|username> [default|slim]"));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "/skin clear"));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "/skin debug"));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "set", "clear", "debug", "admin");
        }
        if (args.length == 2 && "admin".equalsIgnoreCase(args[0]) && sender.canUseCommand(2, "skin")) {
            return getListOfStringsMatchingLastWord(args, "set", "clear");
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && sender.canUseCommand(2, "skin")) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if ((args.length == 3 && "set".equalsIgnoreCase(args[0])) ||
                (args.length == 5 && "admin".equalsIgnoreCase(args[0]) && "set".equalsIgnoreCase(args[1]))) {
            return getListOfStringsMatchingLastWord(args, "default", "slim");
        }
        return Collections.emptyList();
    }
}
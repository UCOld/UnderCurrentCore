package undercurrentcore.commands;

import api.undercurrent.iface.IUCTile;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.DimensionManager;
import undercurrentcore.persist.UCBlockDTO;
import undercurrentcore.persist.UCPlayersWorldData;

public class UCCommandLink extends CommandBase {
    @Override
    public String getCommandName() {
        return "uclink";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "uclink";
    }


    @Override
    public void processCommand(ICommandSender sender, String[] params) {

        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            try {

                UCPlayersWorldData data = (UCPlayersWorldData) MinecraftServer.getServer().getEntityWorld().perWorldStorage.loadData(UCPlayersWorldData.class, UCPlayersWorldData.GLOBAL_TAG);

                if (data != null) {
                    sender.addChatMessage(new ChatComponentText("Key: "+ data.getPlayerSecretKey(player.getUniqueID())));
                    return;

                }
                sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));

            } catch (Exception e) {
                sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            }
        }


    }
}
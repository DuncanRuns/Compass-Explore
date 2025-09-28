package me.duncanruns.compassexplore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class ExploreCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("explore").requires(ServerCommandSource::isExecutedByPlayer).then(
                CommandManager.literal("treasures").executes(context -> execute(context, "treasures"))
        ).then(
                CommandManager.literal("cities").executes(context -> execute(context, "cities"))
        ));
    }

    private static int execute(CommandContext<ServerCommandSource> context, String type) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;
        ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);
        if (stack == null || stack.isEmpty() || !stack.getItem().equals(Items.COMPASS)) {
            context.getSource().sendError(Text.literal("You are not holding a compass!"));
            return 0;
        }

        String compassName = type.equals("cities") ? "End City Finder" : "Treasure Bastion Finder";
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(compassName).styled(style -> style.withItalic(false)));
        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(new NbtCompound()), nbt -> {
            NbtCompound compound = nbt.copyNbt();
            compound.putString("ExploreType", type);
            return NbtComponent.of(compound);
        });
        stack.remove(DataComponentTypes.LODESTONE_TRACKER);
        context.getSource().sendFeedback(() -> Text.literal("Your compass is now the ").append(compassName).append(", use (right click) the compass to refresh.").styled(style -> style.withColor(Formatting.GREEN)), false);

        return 1;
    }
}

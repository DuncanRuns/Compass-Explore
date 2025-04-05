package me.duncanruns.compassexplore.mixin;

import me.duncanruns.compassexplore.CompassExplore;
import me.duncanruns.compassexplore.mixinint.ServerWorldInt;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(CompassItem.class)
public abstract class CompassItemMixin extends Item {

    public CompassItemMixin(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return super.use(world, user, hand);
        }

        ItemStack stack = user.getStackInHand(hand);
        if (!stack.contains(DataComponentTypes.CUSTOM_DATA)) return super.use(world, user, hand);

        NbtCompound itemNbt = stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        String exploreType = itemNbt.getString("ExploreType", "");
        if (exploreType.isEmpty()) return super.use(world, user, hand);

        if (exploreType.equals("cities")) {
            if (world.getRegistryKey() != World.END) {
                return super.use(world, user, hand);
            }

            BlockPos pos = ((ServerWorldInt) world).compassExplore$locateNewStructure(CompassExplore.END_CITY, user.getBlockPos(), 1000, false, CompassExplore.SearchType.CITIES);
            user.getItemCooldownManager().set(stack, 200);
            if (pos == null) {
                user.sendMessage(Text.literal("Failed to find an End City!").styled(style -> style.withColor(Formatting.RED)), false);
                return super.use(world, user, hand);
            } else {
                user.sendMessage(Text.literal(String.format("Found End City at [%d, %d]", pos.getX(), pos.getZ())).styled(style -> style.withColor(Formatting.GREEN).withClickEvent(new ClickEvent.CopyToClipboard(String.format("%d %d %d", pos.getX(), 64, pos.getZ()))).withHoverEvent(new HoverEvent.ShowText(Text.translatable("chat.copy.click")))), false);
            }
            stack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.of(GlobalPos.create(World.END, pos)), false));
        } else if (exploreType.equals("treasures")) {
            if (world.getRegistryKey() != World.NETHER) {
                return super.use(world, user, hand);
            }

            BlockPos pos = ((ServerWorldInt) world).compassExplore$locateNewStructure(CompassExplore.BASTION_REMNANT, user.getBlockPos(), 1000, false, CompassExplore.SearchType.TREASURES);
            user.getItemCooldownManager().set(stack, 200);
            if (pos == null) {
                user.sendMessage(Text.literal("Failed to find a Treasure Bastion!").styled(style -> style.withColor(Formatting.RED)), false);
                return super.use(world, user, hand);
            } else {
                user.sendMessage(Text.literal(String.format("Found Treasure Bastion at [%d, %d]", pos.getX(), pos.getZ())).styled(style -> style.withColor(Formatting.GREEN).withClickEvent(new ClickEvent.CopyToClipboard(String.format("%d %d %d", pos.getX(), 64, pos.getZ()))).withHoverEvent(new HoverEvent.ShowText(Text.translatable("chat.copy.click")))), false);
            }
            stack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.of(GlobalPos.create(World.NETHER, pos)), false));
        }

        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return ActionResult.SUCCESS;
    }
}

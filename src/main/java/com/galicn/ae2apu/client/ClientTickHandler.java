package com.galicn.ae2apu.client;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import com.galicn.ae2apu.AE2AutoPatternUpload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = AE2AutoPatternUpload.MODID, value = Dist.CLIENT)
public final class ClientTickHandler {
    private static final long DEDUP_NANOS = 250_000_000L;
    private static long lastDirectWrite;
    private static int lastFluidMenuId = -1;

    private ClientTickHandler() {
    }

    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!KeyBindings.SEARCH_ITEM.matches(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        if (!(event.getScreen() instanceof AEBaseScreen<?>)) {
            return;
        }
        event.setCanceled(true);
        lastDirectWrite = System.nanoTime();
        applySearch(event.getScreen());
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        while (KeyBindings.SEARCH_ITEM.consumeClick()) {
            if (System.nanoTime() - lastDirectWrite > DEDUP_NANOS) {
                Screen current = Minecraft.getInstance().screen;
                if (current instanceof AEBaseScreen<?>) {
                    applySearch(current);
                }
            }
        }

        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen == null) {
            lastFluidMenuId = -1;
            return;
        }

        boolean processing = screen instanceof AEBaseScreen<?> ae
                && ae.getMenu() instanceof PatternEncodingTermMenu menu
                && menu.getMode() == EncodingMode.PROCESSING;
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof ScaleButton button) {
                button.visible = processing;
            }
        }
        autoEnableFluidSubstitution(screen);
    }

    private static void applySearch(Screen screen) {
        String name = ViewerLookup.hoveredName();
        if (name == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("ae2apu.msg.viewer_no_item"), true);
            }
            return;
        }
        AeSearchWriter.write(screen, name);
    }

    private static void autoEnableFluidSubstitution(Screen screen) {
        if (!(screen instanceof AEBaseScreen<?> ae)
                || !(ae.getMenu() instanceof PatternEncodingTermMenu menu)) {
            lastFluidMenuId = -1;
            return;
        }
        if (menu.getMode() != EncodingMode.CRAFTING || menu.containerId == lastFluidMenuId
                || menu.isSubstituteFluids() || !hasFluidBucket(menu)) {
            return;
        }
        lastFluidMenuId = menu.containerId;
        menu.setSubstituteFluids(true);
    }

    private static boolean hasFluidBucket(PatternEncodingTermMenu menu) {
        for (FakeSlot slot : menu.getCraftingGridSlots()) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()
                    || !(stack.getItem() instanceof BucketItem)
                            && !(stack.getItem() instanceof MilkBucketItem)) {
                continue;
            }
            GenericStack contained = ContainerItemStrategies.getContainedStack(stack, AEKeyType.fluids());
            if (contained != null && contained.what() instanceof AEFluidKey) {
                return true;
            }
        }
        return false;
    }
}

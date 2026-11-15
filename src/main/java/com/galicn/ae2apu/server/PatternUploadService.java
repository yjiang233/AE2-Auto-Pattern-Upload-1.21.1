package com.galicn.ae2apu.server;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.EncodedCraftingPattern;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import com.galicn.ae2apu.AE2AutoPatternUpload;
import com.galicn.ae2apu.network.ProvidersListPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PatternUploadService {
    private static final long SESSION_TTL_MS = 120_000L;
    private static final ResourceLocation MOLECULAR_ASSEMBLER =
            ResourceLocation.fromNamespaceAndPath("ae2", "molecular_assembler");
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private record ProviderRef(PatternContainer container, IGrid grid, String name) {
    }

    private record Session(long createdAt, List<ProviderRef> providers) {
    }

    private record Candidate(PatternContainer container, String name, int emptySlots, boolean assembler) {
    }

    private record PatternInfo(String productId, String method) {
    }

    private record FluidInfo(boolean bucket, boolean ready, boolean rewrite, String name) {
    }

    private PatternUploadService() {
    }

    public static void handleRequest(ServerPlayer player, boolean direct) {
        if (!(player.containerMenu instanceof PatternEncodingTermMenu menu)) {
            message(player, "ae2apu.msg.open_terminal");
            return;
        }
        IGrid grid = gridOf(menu);
        if (grid == null) {
            message(player, "ae2apu.msg.no_network");
            return;
        }

        ItemStack pattern = encodedPattern(menu);
        boolean crafting = isCraftingPattern(pattern, player.serverLevel());
        List<Candidate> candidates = collect(grid);
        candidates.sort(crafting
                ? Comparator.comparing(Candidate::assembler).reversed()
                        .thenComparing(Candidate::name, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparing(Candidate::name, String.CASE_INSENSITIVE_ORDER));

        List<ProviderRef> refs = new ArrayList<>(candidates.size());
        List<ProvidersListPayload.Entry> entries = new ArrayList<>(candidates.size());
        int defaultIndex = -1;
        for (Candidate candidate : candidates) {
            int index = refs.size();
            refs.add(new ProviderRef(candidate.container(), grid, candidate.name()));
            entries.add(new ProvidersListPayload.Entry(index, candidate.name(), candidate.emptySlots()));
            if (crafting && defaultIndex < 0 && candidate.assembler()) {
                defaultIndex = index;
            }
        }

        SESSIONS.put(player.getUUID(), new Session(System.currentTimeMillis(), refs));
        pruneSessions();

        if (direct) {
            if (defaultIndex >= 0) {
                upload(player, menu, grid, refs, defaultIndex);
            } else {
                message(player, "ae2apu.msg.no_default_target");
            }
            return;
        }

        PatternInfo info = patternInfo(pattern, player.serverLevel());
        PacketDistributor.sendToPlayer(player,
                new ProvidersListPayload(entries, info.productId(), info.method(), defaultIndex));
    }

    public static void handleUpload(ServerPlayer player, int providerIndex) {
        if (!(player.containerMenu instanceof PatternEncodingTermMenu menu)) {
            message(player, "ae2apu.msg.open_terminal");
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || System.currentTimeMillis() - session.createdAt() > SESSION_TTL_MS) {
            message(player, "ae2apu.msg.list_expired");
            return;
        }
        IGrid grid = gridOf(menu);
        if (grid == null) {
            message(player, "ae2apu.msg.no_network");
            return;
        }
        upload(player, menu, grid, session.providers(), providerIndex);
    }

    public static void handleScale(ServerPlayer player, int factor, boolean divide) {
        if (factor != 2 && factor != 3 && factor != 5) {
            return;
        }
        if (!(player.containerMenu instanceof PatternEncodingTermMenu menu)) {
            message(player, "ae2apu.msg.open_terminal");
            return;
        }
        if (menu.getMode() != EncodingMode.PROCESSING) {
            message(player, "ae2apu.msg.scale_only_processing");
            return;
        }
        if (!(menu.getHost() instanceof IPatternTerminalMenuHost host)) {
            message(player, "ae2apu.msg.scale_only_processing");
            return;
        }

        PatternEncodingLogic logic = host.getLogic();
        int changed = scale(logic.getEncodedInputInv(), factor, divide)
                + scale(logic.getEncodedOutputInv(), factor, divide);
        menu.broadcastChanges();
        player.displayClientMessage(Component.translatable("ae2apu.msg.scale_done",
                (divide ? "÷" : "×") + factor, changed), true);
    }

    private static int scale(ConfigInventory inventory, int factor, boolean divide) {
        int changed = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            GenericStack stack = inventory.getStack(slot);
            if (stack == null || stack.amount() <= 0) {
                continue;
            }
            AEKey key = stack.what();
            long max = inventory.getMaxAmount(key);
            long next;
            if (divide) {
                next = Math.max(1, stack.amount() / factor);
            } else {
                next = stack.amount() > Long.MAX_VALUE / factor ? Long.MAX_VALUE : stack.amount() * factor;
            }
            next = Math.min(next, max);
            if (next != stack.amount()) {
                inventory.setStack(slot, new GenericStack(key, next));
                changed++;
            }
        }
        return changed;
    }

    private static List<Candidate> collect(IGrid grid) {
        Set<PatternContainer> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Candidate> candidates = new ArrayList<>();
        for (Class<?> machineClass : grid.getMachineClasses()) {
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            for (Object machine : grid.getActiveMachines(machineClass)) {
                if (!(machine instanceof PatternContainer container) || !seen.add(container)) {
                    continue;
                }
                InternalInventory patterns = container.getTerminalPatternInventory();
                if (patterns == null) {
                    continue;
                }
                int empty = 0;
                for (int slot = 0; slot < patterns.size(); slot++) {
                    if (patterns.getStackInSlot(slot).isEmpty()) {
                        empty++;
                    }
                }
                if (empty == 0) {
                    continue;
                }
                String name = displayName(container);
                candidates.add(new Candidate(container, name, empty, isAssembler(container, name)));
            }
        }
        return candidates;
    }

    private static void upload(ServerPlayer player, PatternEncodingTermMenu menu, IGrid grid,
                               List<ProviderRef> refs, int providerIndex) {
        if (providerIndex < 0 || providerIndex >= refs.size()) {
            message(player, "ae2apu.msg.list_expired");
            return;
        }
        ProviderRef ref = refs.get(providerIndex);
        if (ref.grid() != grid || ref.container().getGrid() != grid) {
            message(player, "ae2apu.msg.list_expired");
            return;
        }
        ItemStack pattern = encodedPattern(menu);
        if (!PatternDetailsHelper.isEncodedPattern(pattern)) {
            message(player, "ae2apu.msg.no_pattern");
            return;
        }
        FluidInfo fluid = inspectFluid(pattern, player.serverLevel(), grid);
        ItemStack toInsert = pattern;
        if (fluid.rewrite()) {
            ItemStack rebuilt = enableFluidSubstitution(pattern, player.serverLevel());
            if (!rebuilt.isEmpty()) {
                toInsert = rebuilt;
            }
        }
        InternalInventory patterns = ref.container().getTerminalPatternInventory();
        if (patterns == null || !patterns.addItems(toInsert.copyWithCount(1)).isEmpty()) {
            message(player, "ae2apu.msg.provider_full");
            return;
        }

        clearEncodedPattern(menu);
        SESSIONS.remove(player.getUUID());
        if (fluid.bucket()) {
            String key = fluid.ready() ? "ae2apu.msg.uploaded_fluid" : "ae2apu.msg.uploaded_fluid_missing";
            player.displayClientMessage(Component.translatable(key, ref.name(), fluid.name()), true);
        } else {
            player.displayClientMessage(Component.translatable("ae2apu.msg.uploaded", ref.name()), true);
        }
    }

    private static FluidInfo inspectFluid(ItemStack pattern, ServerLevel level, IGrid grid) {
        EncodedCraftingPattern encoded = pattern.get(AEComponents.ENCODED_CRAFTING_PATTERN);
        if (encoded == null) {
            return new FluidInfo(false, false, false, "");
        }
        AEFluidKey fluid = null;
        for (ItemStack input : encoded.inputs()) {
            GenericStack contained = fluidContainer(input);
            if (contained != null && contained.what() instanceof AEFluidKey key) {
                fluid = key;
                break;
            }
        }
        if (fluid == null) {
            return new FluidInfo(false, false, false, "");
        }
        boolean ready = false;
        try {
            KeyCounter counter = grid.getStorageService().getCachedInventory();
            long available = 0;
            for (AEKey key : counter.keySet()) {
                if (key instanceof AEFluidKey stored && stored.getFluid() == fluid.getFluid()) {
                    available += counter.get(key);
                }
            }
            ready = available >= AEFluidKey.AMOUNT_BUCKET
                    && counter.get(AEItemKey.of(Items.BUCKET)) >= 1;
        } catch (RuntimeException ignored) {
        }
        return new FluidInfo(true, ready, !encoded.canSubstituteFluids(),
                fluid.getDisplayName().getString());
    }

    private static GenericStack fluidContainer(ItemStack stack) {
        if (stack.isEmpty()
                || !(stack.getItem() instanceof BucketItem)
                        && !(stack.getItem() instanceof MilkBucketItem)) {
            return null;
        }
        GenericStack contained = ContainerItemStrategies.getContainedStack(stack, AEKeyType.fluids());
        return contained != null && contained.what() instanceof AEFluidKey ? contained : null;
    }

    private static ItemStack enableFluidSubstitution(ItemStack pattern, ServerLevel level) {
        EncodedCraftingPattern encoded = pattern.get(AEComponents.ENCODED_CRAFTING_PATTERN);
        if (encoded == null) {
            return ItemStack.EMPTY;
        }
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(encoded.recipeId());
        if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe)) {
            return ItemStack.EMPTY;
        }
        @SuppressWarnings("unchecked")
        RecipeHolder<CraftingRecipe> recipe = (RecipeHolder<CraftingRecipe>) holder.get();
        ItemStack[] inputs = encoded.inputs().toArray(new ItemStack[0]);
        if (inputs.length != 9) {
            inputs = Arrays.copyOf(inputs, 9);
            for (int i = 0; i < inputs.length; i++) {
                if (inputs[i] == null) {
                    inputs[i] = ItemStack.EMPTY;
                }
            }
        }
        try {
            return PatternDetailsHelper.encodeCraftingPattern(recipe, inputs, encoded.result(),
                    encoded.canSubstitute(), true);
        } catch (RuntimeException e) {
            AE2AutoPatternUpload.LOG.debug("无法重编码流体替代样板", e);
            return ItemStack.EMPTY;
        }
    }

    private static boolean isCraftingPattern(ItemStack pattern, ServerLevel level) {
        if (!PatternDetailsHelper.isEncodedPattern(pattern)) {
            return false;
        }
        try {
            IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, level);
            return details instanceof AECraftingPattern;
        } catch (RuntimeException e) {
            AE2AutoPatternUpload.LOG.debug("无法解析样板类型", e);
            return false;
        }
    }

    private static boolean isAssembler(PatternContainer container, String name) {
        try {
            PatternContainerGroup group = container.getTerminalGroup();
            if (group != null && group.icon() != null && MOLECULAR_ASSEMBLER.equals(group.icon().getId())) {
                return true;
            }
        } catch (RuntimeException ignored) {
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("molecular assembler") || lower.contains("assembler matrix")
                || lower.contains("分子装配") || lower.contains("装配矩阵");
    }

    private static void pruneSessions() {
        long now = System.currentTimeMillis();
        SESSIONS.entrySet().removeIf(entry -> now - entry.getValue().createdAt() > SESSION_TTL_MS);
    }

    private static IGrid gridOf(PatternEncodingTermMenu menu) {
        IGridNode node = menu.getGridNode();
        return node == null ? null : node.getGrid();
    }

    private static ItemStack encodedPattern(PatternEncodingTermMenu menu) {
        for (Slot slot : menu.getSlots(SlotSemantics.ENCODED_PATTERN)) {
            ItemStack stack = slot.getItem();
            if (PatternDetailsHelper.isEncodedPattern(stack)) {
                return stack;
            }
        }
        if (menu.getHost() instanceof IPatternTerminalMenuHost host) {
            InternalInventory inventory = host.getLogic().getEncodedPatternInv();
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (PatternDetailsHelper.isEncodedPattern(stack)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static void clearEncodedPattern(PatternEncodingTermMenu menu) {
        for (Slot slot : menu.getSlots(SlotSemantics.ENCODED_PATTERN)) {
            if (slot.hasItem()) {
                slot.set(ItemStack.EMPTY);
            }
        }
        if (menu.getHost() instanceof IPatternTerminalMenuHost host) {
            InternalInventory inventory = host.getLogic().getEncodedPatternInv();
            for (int slot = 0; slot < inventory.size(); slot++) {
                inventory.setItemDirect(slot, ItemStack.EMPTY);
            }
        }
    }

    private static PatternInfo patternInfo(ItemStack pattern, ServerLevel level) {
        if (!PatternDetailsHelper.isEncodedPattern(pattern)) {
            return new PatternInfo("", "");
        }
        try {
            IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, level);
            if (details == null) {
                return new PatternInfo("", "");
            }
            String productId = "";
            GenericStack output = details.getPrimaryOutput();
            if (output != null && output.what() instanceof AEItemKey key) {
                productId = key.getId().toString();
            }
            String simpleName = details.getClass().getSimpleName();
            String method;
            if (details instanceof AECraftingPattern) {
                method = "crafting";
            } else if (simpleName.contains("Smithing")) {
                method = "smithing";
            } else if (simpleName.contains("Stonecutting")) {
                method = "stonecutting";
            } else {
                method = "processing";
            }
            return new PatternInfo(productId, method);
        } catch (RuntimeException e) {
            AE2AutoPatternUpload.LOG.debug("无法解析样板配方方式", e);
            return new PatternInfo("", "");
        }
    }

    private static String displayName(PatternContainer container) {
        try {
            PatternContainerGroup group = container.getTerminalGroup();
            if (group != null && group.name() != null) {
                String name = group.name().getString().trim();
                if (!name.isEmpty()) {
                    return name;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return Component.translatable("block.ae2.pattern_provider").getString();
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}

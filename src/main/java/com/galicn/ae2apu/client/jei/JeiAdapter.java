package com.galicn.ae2apu.client.jei;

import com.galicn.ae2apu.AE2AutoPatternUpload;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@JeiPlugin
public class JeiAdapter implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(AE2AutoPatternUpload.MODID, "viewer");
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        AE2AutoPatternUpload.LOG.info("JEI runtime attached to {}", UID);
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static String filterText() {
        return runtime == null ? null : runtime.getIngredientFilter().getFilterText();
    }

    public static void setFilterText(String text) {
        if (runtime != null) {
            runtime.getIngredientFilter().setFilterText(text == null ? "" : text);
        }
    }

    public static String hoveredName() {
        IJeiRuntime current = runtime;
        if (current == null) {
            return null;
        }
        Optional<ITypedIngredient<?>> under = current.getIngredientListOverlay().getIngredientUnderMouse();
        if (under.isEmpty()) {
            under = current.getBookmarkOverlay().getIngredientUnderMouse();
        }
        if (under.isPresent()) {
            ITypedIngredient<?> ingredient = under.get();
            Optional<ItemStack> stack = ingredient.getItemStack();
            if (stack.isPresent()) {
                return stack.get().getHoverName().getString();
            }
            Object value = ingredient.getIngredient();
            IIngredientHelper<Object> helper = current.getIngredientManager().getIngredientHelper(value);
            return helper == null ? null : helper.getDisplayName(value);
        }
        return current.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK)
                .map(stack -> stack.getHoverName().getString())
                .orElse(null);
    }

    public static String categoryName(String productId, String method) {
        IJeiRuntime current = runtime;
        if (current == null || productId == null || productId.isBlank()) {
            return null;
        }
        try {
            ResourceLocation id = ResourceLocation.parse(productId);
            Item item = BuiltInRegistries.ITEM.getHolder(id).map(holder -> holder.value()).orElse(Items.AIR);
            if (item == Items.AIR) {
                return null;
            }
            IFocus<ItemStack> focus = current.getJeiHelpers().getFocusFactory()
                    .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, new ItemStack(item));
            List<String> titles = current.getRecipeManager().createRecipeCategoryLookup()
                    .limitFocus(List.of(focus))
                    .get()
                    .map(category -> category.getTitle().getString())
                    .filter(title -> !title.isBlank())
                    .distinct()
                    .toList();
            if (titles.isEmpty()) {
                return null;
            }
            if ("processing".equals(method)) {
                for (String title : titles) {
                    String lower = title.toLowerCase(Locale.ROOT);
                    if (!lower.contains("crafting") && !lower.contains("合成")) {
                        return title;
                    }
                }
            }
            return titles.get(0);
        } catch (Throwable t) {
            AE2AutoPatternUpload.LOG.debug("无法从 JEI 查询配方类别", t);
            return null;
        }
    }
}

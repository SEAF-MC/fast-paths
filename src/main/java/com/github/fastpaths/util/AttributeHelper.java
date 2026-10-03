package com.github.fastpaths.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;

public final class AttributeHelper {

    private static final Attribute STEP_HEIGHT_ATTR = resolveAttribute("step_height");

    private AttributeHelper() {}

    private static Attribute resolveAttribute(String key) {
        Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
        if (attr != null) {
            return attr;
        }
        return Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic." + key));
    }

    public static Attribute getStepHeightAttribute() {
        return STEP_HEIGHT_ATTR;
    }

    public static AttributeModifier createModifier(NamespacedKey key, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(key, amount, operation);
    }

    public static void applyModifier(AttributeInstance instance, AttributeModifier modifier, NamespacedKey key) {
        if (instance == null || modifier == null) return;
        instance.removeModifier(key);
        instance.addModifier(modifier);
    }

    public static void removeModifier(AttributeInstance instance, NamespacedKey key) {
        if (instance == null || key == null) return;
        instance.removeModifier(key);
    }
}

package com.texturemixer.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Erlaubt Fullbright: Gamma über das vom Slider erlaubte Maximum setzen. */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor<T> {
    @Accessor("value")
    void texturemixer$setValue(T value);
}

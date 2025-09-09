package com.example.nightmarez;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// Аннотация @Mod теперь берется из пакета net.neoforged.fml.common.Mod
@Mod(NightmarezMod.MODID)
public class NightmarezMod {
    public static final String MODID = "nightmarez";

    public NightmarezMod(IEventBus modEventBus) {
        // Используем NeoForge.EVENT_BUS для регистрации глобальных событий
        NeoForge.EVENT_BUS.register(new ModEventHandler());

        // Регистрируем метод setup на шине событий мода
        modEventBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Код инициализации мода
    }
}

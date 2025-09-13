package com.example.nightmarez;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(NightmarezMod.MODID)
public class NightmarezMod {
    public static final String MODID = "nightmarez";

    public NightmarezMod(IEventBus modEventBus) {
        // Регистрируем глобальные события
        NeoForge.EVENT_BUS.register(new ModEventHandler());

        // Регистрируем setup
        modEventBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Здесь больше ничего не нужно для BreakBlockGoal или ModEventHandler
    }
}

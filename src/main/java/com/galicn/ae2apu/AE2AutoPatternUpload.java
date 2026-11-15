package com.galicn.ae2apu;

import com.galicn.ae2apu.network.ApuNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AE2AutoPatternUpload.MODID)
public class AE2AutoPatternUpload {
    public static final String MODID = "ae2apu";
    public static final Logger LOG = LogUtils.getLogger();

    public AE2AutoPatternUpload(IEventBus modBus) {
        modBus.addListener(ApuNetwork::register);
    }
}

package dev.rinchan.keepbutpenalty.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.rinchan.keepbutpenalty.client.KeepButPenaltyConfigScreen;

public final class KeepButPenaltyModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return KeepButPenaltyConfigScreen::new;
    }
}

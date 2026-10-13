package com.moigferdsrte.ragdoll.compat.jade;

import com.moigferdsrte.ragdoll.block.RagdollPartBlock;
import com.moigferdsrte.ragdoll.mob.block.MobRagdollPartBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class RagdollJadePlugin implements IWailaPlugin {
    @Override
    public void register(final IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(RagdollPartServerDataProvider.INSTANCE, RagdollPartBlock.class);
        registration.registerBlockDataProvider(MobRagdollPartServerDataProvider.INSTANCE, MobRagdollPartBlock.class);
    }

    @Override
    public void registerClient(final IWailaClientRegistration registration) {
        registration.registerBlockComponent(RagdollPartTooltipProvider.INSTANCE, RagdollPartBlock.class);
        registration.registerBlockComponent(MobRagdollPartTooltipProvider.INSTANCE, MobRagdollPartBlock.class);
    }
}

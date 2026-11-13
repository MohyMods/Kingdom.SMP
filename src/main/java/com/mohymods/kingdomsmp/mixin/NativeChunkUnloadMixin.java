package com.mohymods.kingdomsmp.mixin;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Exact1.21.1 queued unload callbacks may outlive a cancelled/replaced pending unload.
 * Native code waits for generation refs BEFORE checking remove(key,holder), endlessly
 * requeueing a stale callback. Check that same invariant first; real pending unloads
 * retain every native generation-reference, write and unload check unchanged.
 */
@Mixin(value=ChunkMap.class,remap=false)
abstract class NativeChunkUnloadMixin {
 @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;
 @Inject(method="lambda$scheduleUnload$12",at=@At("HEAD"),cancellable=true)
 private void kingdom$ignoreCancelledUnload(ChunkHolder holder,long key,CallbackInfo callback){if(pendingUnloads.get(key)!=holder)callback.cancel();}
}

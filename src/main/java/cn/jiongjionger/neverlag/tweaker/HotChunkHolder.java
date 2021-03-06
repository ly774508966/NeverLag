package cn.jiongjionger.neverlag.tweaker;

import cn.jiongjionger.neverlag.NeverLag;
import cn.jiongjionger.neverlag.config.ConfigManager;
import cn.jiongjionger.neverlag.utils.ChunkInfo;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.WeakHashMap;

public class HotChunkHolder implements Listener {

	private final ConfigManager cm = ConfigManager.getInstance();

	// 记录区块卸载的时间
	private final WeakHashMap<ChunkInfo, Long> chunkUnLoadTime = new WeakHashMap<>();
	// 记录区块在额定时间内卸载后又马上加载的次数
	private final WeakHashMap<ChunkInfo, Integer> chunkFastLoadCount = new WeakHashMap<>();
	// 热区块记录
	private final LinkedHashSet<ChunkInfo> hotChunkRecord = new LinkedHashSet<>();
	// 记录热门区块尝试卸载的次数
	private final WeakHashMap<ChunkInfo, Integer> hotChunkTryUnloadRecord = new WeakHashMap<>();

	public HotChunkHolder() {
		NeverLag.getInstance().getServer().getScheduler().runTaskTimer(NeverLag.getInstance(), new Runnable() {
			@Override
			public void run() {
				doClean();
			}
		}, 300 * 20L, 300 * 20L);
		NeverLag.getInstance().getServer().getScheduler().runTaskTimer(NeverLag.getInstance(), new Runnable() {
			@Override
			public void run() {
				removeLeastHotRecord();
			}
		}, 60 * 20L, 60 * 20L);
	}

	private void addChunkFastLoadCount(ChunkInfo chunkInfo) {
		Integer count = chunkFastLoadCount.get(chunkInfo);
		if (count == null) {
			chunkFastLoadCount.put(chunkInfo, 1);
		} else {
			chunkFastLoadCount.put(chunkInfo, count + 1);
			if (count + 1 >= cm.hotChunkReloadCountThreshold) {
				hotChunkRecord.add(chunkInfo);
			}
		}
	}

	private void addHotChunkUnloadCount(ChunkInfo chunkInfo) {
		Integer count = hotChunkTryUnloadRecord.get(chunkInfo);
		if (count == null) {
			hotChunkTryUnloadRecord.put(chunkInfo, 1);
		} else {
			hotChunkTryUnloadRecord.put(chunkInfo, count + 1);
			if (count + 1 >= cm.hotChunkUnloadCountThreshold) {
				hotChunkRecord.remove(chunkInfo);
			}
		}
	}

	// 定时检测记录数量，如果记录过多，则重置
	private void doClean() {
		if (cm.hotChunkEnabled) {
			if (chunkUnLoadTime.size() >= 5000) {
				chunkUnLoadTime.clear();
			}
			if (chunkFastLoadCount.size() >= 5000) {
				chunkFastLoadCount.clear();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkLoad(ChunkLoadEvent e) {
		if (!cm.hotChunkEnabled || e.isNewChunk()) {
			return;
		}
		ChunkInfo chunkInfo = new ChunkInfo(e.getChunk());
		Long unloadtime = chunkUnLoadTime.get(chunkInfo);
		if (unloadtime != null) {
			if (System.currentTimeMillis() - unloadtime <= cm.hotChunkReloadIntervalThreshold) {
				this.addChunkFastLoadCount(chunkInfo);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onUnloadChunk(ChunkUnloadEvent e) {
		if (!cm.hotChunkEnabled || NeverLag.getTpsWatcher().getAverageTPS() < cm.hotChunkTpsThreshold) {
			return;
		}
		ChunkInfo chunkInfo = new ChunkInfo(e.getChunk());
		if (hotChunkRecord.contains(chunkInfo)) {
			e.setCancelled(true);
			this.addHotChunkUnloadCount(chunkInfo);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onUnloadChunkMonitor(ChunkUnloadEvent e) {
		if (!cm.hotChunkEnabled || NeverLag.getTpsWatcher().getAverageTPS() < cm.hotChunkTpsThreshold) {
			return;
		}
		ChunkInfo chunkInfo = new ChunkInfo(e.getChunk());
		chunkUnLoadTime.put(chunkInfo, System.currentTimeMillis());
	}

	// 定时清理多余的热点区块（先进先出）
	private void removeLeastHotRecord() {
		if (cm.hotChunkEnabled && hotChunkRecord.size() > cm.hotChunkMaxCount) {
			HashSet<ChunkInfo> removeSet = new HashSet<>();
			for (ChunkInfo chunkInfo : hotChunkRecord) {
				removeSet.add(chunkInfo);
				if (hotChunkRecord.size() - removeSet.size() <= cm.hotChunkMaxCount) {
					break;
				}
			}
			hotChunkRecord.removeAll(removeSet);

		}
	}
}

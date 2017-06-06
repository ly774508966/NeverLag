package cn.jiongjionger.neverlag.system;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import cn.jiongjionger.neverlag.utils.ProtocolLibUtils;
import cn.jiongjionger.neverlag.NeverLag;

public class WatchDog extends Thread {

	private long lastTickTime = System.currentTimeMillis();
	private long lastSendTime = System.currentTimeMillis();
	private NeverLag plg = NeverLag.getInstance();

	public WatchDog() {
		plg.getServer().getScheduler().runTaskTimer(plg, new Runnable() {
			public void run() {
				lastTickTime = System.currentTimeMillis();
			}
		}, 1L, 1L);
	}

	@Override
	public void run() {
		// 服务端卡顿超过了500ms就向所有在线玩家发送KeepAlive防止掉线
		long now = System.currentTimeMillis();
		if (now - this.lastTickTime >= 500L) {
			// 安装了ProtocoLib前置插件且间隔3秒才发送新的心跳包
			if (NeverLag.isInstallProtocoLib() && now - this.lastSendTime >= 3000L) {
				this.lastSendTime = now;
				// 兼容不同版本
				List<Player> onlinePlayer = new ArrayList<Player>();
				for (World world : Bukkit.getWorlds()) {
					onlinePlayer.addAll(world.getPlayers());
				}
				ProtocolLibUtils.sendKeepAlive(onlinePlayer);
			}
		}
		try {
			sleep(50);
		} catch (InterruptedException ignore) {
		}
	}

}
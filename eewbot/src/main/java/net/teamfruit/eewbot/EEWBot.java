package net.teamfruit.eewbot;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.teamfruit.eewbot.dispatcher.EEWDispatcher;
import net.teamfruit.eewbot.dispatcher.NTPDispatcher;
import net.teamfruit.eewbot.dispatcher.QuakeInfoDispather;
import sx.blah.discord.Discord4J.Discord4JLogger;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;

public class EEWBot {
	public static EEWBot instance;

	public static final Logger LOGGER = new Discord4JLogger(EEWBot.class.getName());
	public static final Gson GSON = new GsonBuilder().registerTypeAdapter(Channel.class, new Channel.ChannelTypeAdapter()).setPrettyPrinting().create();

	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, r -> new Thread(r, "EEWBot-communication-thread"));
	private Config config = new Config();
	private final Map<Long, CopyOnWriteArrayList<Channel>> channels = new ConcurrentHashMap<>();
	private Map<String, Permission> permissions = new HashMap<String, Permission>() {
		{
			put("owner", Permission.ALL);
			put("everyone", Permission.DEFAULT_EVERYONE);
		}
	};
	private final RequestConfig reqest = RequestConfig.custom()
			.setConnectTimeout(1000*10)
			.setSocketTimeout(10000*10)
			.build();
	private final HttpClient http = HttpClientBuilder.create().setDefaultRequestConfig(this.reqest).build();
	private IDiscordClient client;

	public void initialize() throws ConfigException {
		createConfigs();
		loadConfigs();
		saveConfigs();
		if (this.config.isDebug())
			((Discord4JLogger) EEWBot.LOGGER).setLevel(Discord4JLogger.Level.DEBUG);

		if (StringUtils.isEmpty(this.config.getToken()))
			throw new ConfigException("Please set a token");

		this.client = new ClientBuilder()
				.withToken(this.config.getToken())
				.registerListeners(new DiscordEventListener(), new EEWEventListener())
				.login();

		this.executor.scheduleAtFixedRate(NTPDispatcher.INSTANCE, 0, this.config.getTimeFixDelay()>=3600 ? this.config.getTimeFixDelay() : 3600, TimeUnit.SECONDS);
		this.executor.scheduleAtFixedRate(EEWDispatcher.INSTANCE, 10, this.config.getKyoshinDelay()>=1 ? this.config.getKyoshinDelay() : 1, TimeUnit.SECONDS);
		this.executor.scheduleAtFixedRate(QuakeInfoDispather.INSTANCE, 0, this.config.getQuakeInfoDelay()>=10 ? this.config.getQuakeInfoDelay() : 10, TimeUnit.SECONDS);
		EEWBot.LOGGER.info("Hello");
	}

	public ScheduledExecutorService getExecutor() {
		return this.executor;
	}

	public Config getConfig() {
		return this.config;
	}

	public Map<Long, CopyOnWriteArrayList<Channel>> getChannels() {
		return this.channels;
	}

	public Map<String, Permission> getPermissions() {
		return this.permissions;
	}

	public HttpClient getHttpClient() {
		return this.http;
	}

	public IDiscordClient getClient() {
		return this.client;
	}

	public static void main(final String[] args) throws Exception {
		instance = new EEWBot();
		instance.initialize();
	}

	public final Path cfgPath = Paths.get("config.json");
	public final Path channelPath = Paths.get("channels.json");
	public final Path permissionPath = Paths.get("permission.json");

	private void createConfigs() throws ConfigException {
		try {
			if (!this.cfgPath.toFile().exists())
				ConfigUtils.writeConfig(this.cfgPath, this.config, null);
			if (!this.channelPath.toFile().exists())
				ConfigUtils.writeConfig(this.channelPath, new ConcurrentHashMap<Long, CopyOnWriteArrayList<Channel>>(), null);
			if (!this.permissionPath.toFile().exists())
				ConfigUtils.writeConfig(this.permissionPath, this.permissions, new TypeToken<Map<String, Permission>>() {
				}.getType());
		} catch (final IOException e) {
			throw new ConfigException("Config create Error", e);
		}
	}

	public void loadConfigs() throws ConfigException {
		try {
			ConfigUtils.readConfig(this.cfgPath, Config.class).ifPresent(config -> this.config = config);
			final Optional<Map<Long, Collection<Channel>>> map = ConfigUtils.readConfig(this.channelPath, new TypeToken<Map<Long, Collection<Channel>>>() {
			}.getType());
			map.ifPresent(channel -> channel.entrySet().forEach(entry -> this.channels.put(entry.getKey(), new CopyOnWriteArrayList<>(entry.getValue()))));
			final Optional<Map<String, Permission>> permissions = ConfigUtils.readConfig(this.permissionPath, new TypeToken<Map<String, Permission>>() {
			}.getType());
			permissions.ifPresent(perm -> this.permissions = perm);
		} catch (JsonSyntaxException|JsonIOException|IOException e) {
			throw new ConfigException("Config load error", e);
		}
	}

	public void saveConfigs() throws ConfigException {
		try {
			ConfigUtils.writeConfig(this.cfgPath, this.config, null);
			ConfigUtils.writeConfig(this.channelPath, this.channels, null);
			ConfigUtils.writeConfig(this.permissionPath, this.permissions, new TypeToken<Map<String, Permission>>() {
			}.getType());
		} catch (JsonIOException|IOException e) {
			throw new ConfigException("Config save error", e);
		}
	}
}

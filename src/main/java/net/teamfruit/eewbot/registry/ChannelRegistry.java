package net.teamfruit.eewbot.registry;

import com.google.gson.reflect.TypeToken;
import redis.clients.jedis.JedisPooled;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ChannelRegistry extends ConfigurationRegistry<ConcurrentMap<Long, Channel>> {

    private JedisPooled jedisPool;

    public ChannelRegistry(Path path) {
        super(path, ConcurrentHashMap::new, new TypeToken<ConcurrentHashMap<Long, Channel>>() {
        }.getType());
    }

    public void setJedis(JedisPooled jedisPooled) {
        this.jedisPool = jedisPooled;
    }

    public Channel get(long key) {
        return getElement().get(key);
    }

    public Channel remove(long key) {
        return getElement().remove(key);
    }

    public Channel computeIfAbsent(long key, Function<? super Long, ? extends Channel> mappingFunction) {
        return getElement().computeIfAbsent(key, mappingFunction);
    }

    public List<Map.Entry<Long, Channel>> getWebhookAbsentChannels() {
        return getElement().entrySet()
                .stream()
                .filter(entry -> entry.getValue().webhook == null)
                .collect(Collectors.toList());
    }

    public List<Channel> getChannels(Predicate<Channel> filter) {
        return getElement().values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    public Map<Boolean, List<Map.Entry<Long, Channel>>> getChannelsPartitionedByWebhookPresent(Predicate<Channel> filter) {
        return getElement().entrySet().stream()
                .filter(entry -> filter.test(entry.getValue()))
                .collect(Collectors.partitioningBy(entry -> entry.getValue().webhook != null));
    }

}
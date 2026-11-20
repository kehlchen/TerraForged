package dev.kehlchen.terraforged;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

public final class UpdateChecker implements Listener {

    private static final String GAME_VERSION = "26.3";

    // Alle 30 Minuten prüfen
    private static final long CHECK_INTERVAL =
            20L * 60L * 30L;

    private final JavaPlugin plugin;
    private final String projectSlug;
    private final String adminPermission;

    private final String currentVersion;

    private final HttpClient httpClient;

    private final AtomicBoolean checking = new AtomicBoolean(false);

    private BukkitTask periodicTask;

    private volatile VersionInfo latestVersion;

    // Verhindert wiederholte Nachrichten bei jedem Check
    private String lastAnnouncedVersion;

    public UpdateChecker(
            JavaPlugin plugin,
            String projectSlug,
            String adminPermission
    ) {
        this.plugin = plugin;
        this.projectSlug = projectSlug;
        this.adminPermission = adminPermission;

        this.currentVersion =
                plugin.getPluginMeta().getVersion();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Startet den Update-Checker.
     */
    public void start() {

        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Sofort beim Serverstart prüfen
        checkForUpdates();

        // Anschließend alle 30 Minuten
        periodicTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::checkForUpdates,
                CHECK_INTERVAL,
                CHECK_INTERVAL
        );

        plugin.getLogger().info("Update checker enabled.");
    }

    /**
     * Stoppt den periodischen Update-Check.
     */
    public void stop() {

        if (periodicTask != null) {
            periodicTask.cancel();
        }
    }

    /**
     * Fragt die Modrinth-API asynchron ab.
     */
    private void checkForUpdates() {

        if (!checking.compareAndSet(false, true)) {
            return;
        }

        String apiUrl =
                "https://api.modrinth.com/v2/project/"
                        + projectSlug
                        + "/version"
                        + "?game_versions=%5B%22"
                        + GAME_VERSION
                        + "%22%5D"
                        + "&include_changelog=false";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header(
                        "User-Agent",
                        "kehlchen/" + plugin.getName()
                                + "/" + currentVersion
                                + " (Paper Minecraft Plugin)"
                )
                .GET()
                .build();

        httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        ).whenComplete((response, error) -> {

            try {

                if (!plugin.isEnabled()) {
                    return;
                }

                if (error != null) {
                    plugin.getLogger().warning(
                            "Update check failed: " + error.getMessage()
                    );
                    return;
                }

                if (response.statusCode() != 200) {
                    plugin.getLogger().warning(
                            "Modrinth API returned HTTP "
                                    + response.statusCode()
                    );
                    return;
                }

                VersionInfo latest = findLatestVersion(
                        response.body()
                );

                if (latest == null) {
                    plugin.getLogger().warning(
                            "No listed release for Minecraft "
                                    + GAME_VERSION + " found."
                    );
                    return;
                }

                // Bukkit-Spieler und Nachrichten nur synchron bearbeiten
                Bukkit.getScheduler().runTask(plugin, () -> {

                    if (!plugin.isEnabled()) {
                        return;
                    }

                    handleUpdate(latest);
                });

            } catch (Exception e) {

                plugin.getLogger().warning(
                        "Could not process update information: "
                                + e.getMessage()
                );

            } finally {
                checking.set(false);
            }
        });
    }

    /**
     * Ermittelt die neueste veröffentlichte Release-Version.
     */
    private VersionInfo findLatestVersion(String json) {

        JsonArray versions = JsonParser
                .parseString(json)
                .getAsJsonArray();

        return StreamSupport.stream(
                        versions.spliterator(),
                        false
                )
                .map(JsonElement::getAsJsonObject)

                // Nur veröffentlichte Versionen
                .filter(v -> "listed".equals(
                        getString(v, "status")
                ))

                // Nur stabile Releases
                .filter(v -> "release".equals(
                        getString(v, "version_type")
                ))

                // Nur passende Minecraft-Versionen
                .filter(this::supportsGameVersion)

                // Neueste Veröffentlichung anhand des Datums
                .max(Comparator.comparing(
                        v -> Instant.parse(
                                getString(v, "date_published")
                        )
                ))
                .map(v -> new VersionInfo(
                        getString(v, "version_number"),
                        getString(v, "id")
                ))
                .orElse(null);
    }

    private boolean supportsGameVersion(JsonObject version) {

        if (!version.has("game_versions")
                || !version.get("game_versions").isJsonArray()) {
            return false;
        }

        JsonArray gameVersions =
                version.getAsJsonArray("game_versions");

        return StreamSupport.stream(
                gameVersions.spliterator(),
                false
        ).anyMatch(v ->
                GAME_VERSION.equals(v.getAsString())
        );
    }

    private String getString(JsonObject object, String key) {

        JsonElement element = object.get(key);

        if (element == null || element.isJsonNull()) {
            return "";
        }

        return element.getAsString();
    }

    /**
     * Verarbeitet eine gefundene Version.
     */
    private void handleUpdate(VersionInfo latest) {

        latestVersion = latest;

        if (!isUpdateAvailable(latest.versionNumber())) {
            lastAnnouncedVersion = null;
            return;
        }

        plugin.getLogger().info(
                "Update available: "
                        + currentVersion
                        + " -> "
                        + latest.versionNumber()
        );

        // Bereits bekannte Updates nicht erneut broadcasten
        if (latest.versionNumber().equals(lastAnnouncedVersion)) {
            return;
        }

        lastAnnouncedVersion = latest.versionNumber();

        // Alle aktuell online befindlichen Admins benachrichtigen
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (isAdmin(player)) {
                sendUpdateMessage(player, latest);
            }
        }
    }

    /**
     * Prüft, ob eine neuere Version verfügbar ist.
     */
    private boolean isUpdateAvailable(String version) {

        // Die Versionsnummern müssen exakt übereinstimmen,
        // wie sie auch in plugin.yml und Modrinth stehen.
        return !currentVersion.equalsIgnoreCase(version);
    }

    /**
     * Admin-Erkennung über Bukkit-Permissions.
     */
    private boolean isAdmin(Player player) {
        return player.hasPermission(adminPermission);
    }

    /**
     * Benachrichtigt einen Admin.
     */
    private void sendUpdateMessage(
            Player player,
            VersionInfo latest
    ) {

        player.sendMessage(
                Component.text(
                        "[" + plugin.getName() + "] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "Ein Update ist verfügbar!",
                                NamedTextColor.YELLOW
                        )
                )
        );

        player.sendMessage(
                Component.text("Aktuelle Version: ", NamedTextColor.GRAY)
                        .append(Component.text(
                                currentVersion,
                                NamedTextColor.RED
                        ))
        );

        player.sendMessage(
                Component.text("Neue Version: ", NamedTextColor.GRAY)
                        .append(Component.text(
                                latest.versionNumber(),
                                NamedTextColor.GREEN
                        ))
        );

        player.sendMessage(
                Component.text(
                        "» Modrinth öffnen",
                        NamedTextColor.AQUA
                ).clickEvent(
                        ClickEvent.openUrl(
                                "https://modrinth.com/plugin/"
                                        + projectSlug
                                        + "/version/"
                                        + latest.versionId()
                        )
                )
        );
    }

    /**
     * Benachrichtigt Admins bei jedem Join,
     * sofern bereits ein Update bekannt ist.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (!isAdmin(player)) {
            return;
        }

        VersionInfo latest = latestVersion;

        if (latest == null) {
            return;
        }

        if (!isUpdateAvailable(latest.versionNumber())) {
            return;
        }

        sendUpdateMessage(player, latest);
    }

    private record VersionInfo(
            String versionNumber,
            String versionId
    ) {}
}

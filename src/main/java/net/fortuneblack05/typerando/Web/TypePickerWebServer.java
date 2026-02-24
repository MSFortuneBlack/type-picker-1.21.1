package net.fortuneblack05.typerando.Web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import net.fortuneblack05.typerando.TypePicker;
import net.fortuneblack05.typerando.Server.TypePickerConfigs;
import net.fortuneblack05.typerando.Types;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class TypePickerWebServer {
    private static HttpServer server;
    private static final Gson GSON = new Gson();

    public static void start(MinecraftServer mcServer) {
        try {
            TypePickerConfigs config = TypePickerConfigs.load();
            int port = config.webServerPort;
            if (config.useServerPortOffset) port = mcServer.getServerPort() + config.portOffset;

            server = HttpServer.create(new InetSocketAddress(port), 0);

            // 1. Serve the Neon Webpage
            server.createContext("/", exchange -> {
                String html = getHtmlTemplate();
                byte[] response = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });

            // 2. API: Get Online Players
            server.createContext("/api/players", exchange -> {
                JsonArray playersArray = new JsonArray();
                for (ServerPlayerEntity player : mcServer.getPlayerManager().getPlayerList()) {
                    JsonObject pObj = new JsonObject();
                    String uuid = player.getUuidAsString();
                    pObj.addProperty("name", player.getName().getString());
                    pObj.addProperty("uuid", uuid);

                    Optional<Types> currentType = TypePicker.MANAGER.getRole(player.getUuid());
                    pObj.addProperty("currentType", currentType.map(t -> t.displayName).orElse("None"));

                    // Add current weights and blacklists
                    JsonObject weightsObj = new JsonObject();
                    JsonObject blacklistsObj = new JsonObject();
                    for (int i = 1; i <= 18; i++) {
                        weightsObj.addProperty(String.valueOf(i), TypePicker.MANAGER.getWeight(uuid, i));
                        blacklistsObj.addProperty(String.valueOf(i), TypePicker.MANAGER.getBlacklist(uuid).contains(i));
                    }
                    pObj.add("weights", weightsObj);
                    pObj.add("blacklists", blacklistsObj);

                    playersArray.add(pObj);
                }

                String response = GSON.toJson(playersArray);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }
            });

            // 3. API: Save Updates
            server.createContext("/api/update", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    try (InputStream is = exchange.getRequestBody()) {
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject data = GSON.fromJson(body, JsonObject.class);

                        String uuid = data.get("uuid").getAsString();
                        JsonArray types = data.getAsJsonArray("types");

                        for (JsonElement el : types) {
                            JsonObject t = el.getAsJsonObject();
                            int id = t.get("id").getAsInt();
                            double weight = t.get("weight").getAsDouble();
                            boolean blacklist = t.get("blacklist").getAsBoolean();

                            TypePicker.MANAGER.setWeight(uuid, id, weight);
                            TypePicker.MANAGER.setBlacklist(uuid, id, blacklist);
                        }
                        TypePicker.MANAGER.save(mcServer);

                        String response = "{\"status\":\"success\"}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, response.length());
                        try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }
                    } catch (Exception e) {
                        String error = "{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}";
                        exchange.sendResponseHeaders(500, error.length());
                        try (OutputStream os = exchange.getResponseBody()) { os.write(error.getBytes()); }
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("[TypeRando] Neon Web Interface running on port: " + port);
        } catch (Exception e) {
            System.err.println("[TypeRando] Failed to start web server: " + e.getMessage());
        }
    }

    public static void stop() {
        if (server != null) { server.stop(0); System.out.println("[TypeRando] Web Interface stopped."); }
    }

    private static String getHtmlTemplate() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>TypeRando - Live Dashboard</title>
            <style>
                :root { --neon: #ff5e00; --bg: #0a0a0a; --panel: #141414; }
                body { background: var(--bg); color: #fff; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; }
                h1 { text-align: center; color: var(--neon); text-shadow: 0 0 10px var(--neon); margin-bottom: 30px; }
                
                .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
                
                .card { background: var(--panel); border: 1px solid var(--neon); border-radius: 12px; padding: 15px; display: flex; align-items: center; gap: 15px; cursor: pointer; transition: all 0.3s; box-shadow: 0 0 5px rgba(255, 94, 0, 0.2); }
                .card:hover { transform: translateY(-5px) scale(1.02); box-shadow: 0 0 20px rgba(255, 94, 0, 0.6); border-width: 2px; }
                .card img { width: 64px; height: 64px; border-radius: 8px; box-shadow: 0 0 5px #000; }
                .card-info h3 { margin: 0 0 5px 0; color: #fff; }
                .card-info p { margin: 0; font-size: 0.85em; color: #aaa; }
                .card-info .type-badge { display: inline-block; margin-top: 8px; padding: 3px 8px; background: #333; border: 1px solid var(--neon); border-radius: 4px; color: var(--neon); font-size: 0.8em; font-weight: bold; }

                /* Modal styling */
                .modal-overlay { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.85); justify-content: center; align-items: center; z-index: 1000; opacity: 0; transition: opacity 0.3s; }
                .modal-overlay.open { display: flex; opacity: 1; }
                .modal-content { background: var(--panel); border: 2px solid var(--neon); box-shadow: 0 0 30px rgba(255, 94, 0, 0.4); padding: 25px; border-radius: 15px; width: 90%; max-width: 500px; max-height: 85vh; overflow-y: auto; transform: scale(0.8); transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); position: relative; }
                .modal-overlay.open .modal-content { transform: scale(1); }
                
                .close-btn { position: absolute; top: 15px; right: 20px; color: var(--neon); font-size: 24px; cursor: pointer; font-weight: bold; }
                .close-btn:hover { color: #fff; text-shadow: 0 0 10px var(--neon); }

                .row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #333; }
                .row:last-child { border-bottom: none; }
                .row label { flex: 1; font-weight: bold; }
                
                input[type="number"] { background: #000; color: var(--neon); border: 1px solid var(--neon); padding: 5px; width: 60px; border-radius: 4px; text-align: center; }
                input[type="checkbox"] { width: 20px; height: 20px; accent-color: var(--neon); cursor: pointer; }
                
                .controls { display: flex; gap: 10px; align-items: center; }
                
                button.save-btn { width: 100%; background: var(--neon); color: #000; border: none; padding: 15px; font-size: 1.1em; font-weight: bold; border-radius: 8px; margin-top: 20px; cursor: pointer; transition: all 0.2s; }
                button.save-btn:hover { background: #ff7b33; box-shadow: 0 0 15px var(--neon); }
                
                /* Scrollbar */
                ::-webkit-scrollbar { width: 8px; }
                ::-webkit-scrollbar-track { background: var(--bg); }
                ::-webkit-scrollbar-thumb { background: var(--neon); border-radius: 4px; }
            </style>
        </head>
        <body>
            <h1>TYPERANDO LIVE NETWORK</h1>
            <div class="grid" id="player-grid">Loading players...</div>

            <div class="modal-overlay" id="modal" onclick="closeModal(event)">
                <div class="modal-content" onclick="event.stopPropagation()">
                    <span class="close-btn" onclick="closeModal(event, true)">&times;</span>
                    <h2 id="modal-name" style="margin-top:0; color: var(--neon);">Player Name</h2>
                    <p style="color: #aaa; font-size: 0.9em; margin-bottom: 20px;">Adjust drop percentages (1.0 = normal) and blacklists (X).</p>
                    
                    <div id="types-list"></div>
                    
                    <button class="save-btn" onclick="saveSettings()">UPLOAD CHANGES</button>
                    <p id="status" style="text-align:center; font-weight:bold; margin-top:10px;"></p>
                </div>
            </div>

            <script>
                const TYPE_NAMES = ["Normal","Fire","Water","Electric","Grass","Ice","Fighting","Poison","Ground","Flying","Psychic","Bug","Rock","Ghost","Dragon","Dark","Steel","Fairy"];
                let currentPlayerId = null;

                // 1. Fetch online players and build the grid
                function loadPlayers() {
                    fetch('/api/players')
                        .then(res => res.json())
                        .then(players => {
                            const grid = document.getElementById('player-grid');
                            grid.innerHTML = '';
                            if (players.length === 0) {
                                grid.innerHTML = '<p style="color:#aaa;">No players currently online.</p>';
                                return;
                            }

                            players.forEach(p => {
                                const headUrl = `https://crafatar.com/avatars/${p.uuid}?overlay=true`;
                                const card = document.createElement('div');
                                card.className = 'card';
                                card.innerHTML = `
                                    <img src="${headUrl}" alt="${p.name}'s Head" onerror="this.src='https://crafatar.com/avatars/8667ba71b85a4004af54457a9734eed7'">
                                    <div class="card-info">
                                        <h3>${p.name}</h3>
                                        <p>${p.uuid.split('-')[0]}...</p>
                                        <span class="type-badge">Type: ${p.currentType}</span>
                                    </div>
                                `;
                                card.onclick = () => openModal(p);
                                grid.appendChild(card);
                            });
                        })
                        .catch(err => console.error("Failed to load players", err));
                }

                // Initial load
                loadPlayers();

                // 2. Open the Zoom-In Modal
                function openModal(player) {
                    currentPlayerId = player.uuid;
                    document.getElementById('modal-name').innerText = player.name + " Configuration";
                    
                    const list = document.getElementById('types-list');
                    list.innerHTML = '';

                    for (let i = 1; i <= 18; i++) {
                        const typeName = TYPE_NAMES[i - 1];
                        const currentWeight = player.weights[i] || 1.0;
                        const isBlacklisted = player.blacklists[i] === true;

                        const row = document.createElement('div');
                        row.className = 'row';
                        row.innerHTML = `
                            <label>${typeName} (#${i})</label>
                            <div class="controls">
                                <span style="color:#aaa; font-size:0.8em;">Weight:</span>
                                <input type="number" id="weight-${i}" step="0.1" value="${currentWeight}">
                                <span style="color:#aaa; font-size:0.8em; margin-left:10px;">Ban:</span>
                                <input type="checkbox" id="ban-${i}" ${isBlacklisted ? 'checked' : ''} title="Blacklist this type">
                            </div>
                        `;
                        list.appendChild(row);
                    }

                    document.getElementById('modal').classList.add('open');
                }

                function closeModal(event, force = false) {
                    if (force || event.target === document.getElementById('modal')) {
                        document.getElementById('modal').classList.remove('open');
                        document.getElementById('status').innerText = "";
                    }
                }

                // 3. Save Changes
                function saveSettings() {
                    const typesData = [];
                    for (let i = 1; i <= 18; i++) {
                        typesData.push({
                            id: i,
                            weight: parseFloat(document.getElementById(`weight-${i}`).value),
                            blacklist: document.getElementById(`ban-${i}`).checked
                        });
                    }

                    const payload = { uuid: currentPlayerId, types: typesData };

                    fetch('/api/update', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    }).then(res => res.json())
                      .then(json => {
                          const status = document.getElementById('status');
                          if(json.status === 'success') {
                              status.innerText = "DATA UPLOADED SUCCESSFULLY!";
                              status.style.color = "lightgreen";
                              
                              // Re-fetch the players so the UI is holding the latest data!
                              loadPlayers(); 
                              
                              setTimeout(() => { 
                                  status.innerText = ""; 
                                  document.getElementById('modal').classList.remove('open'); 
                              }, 1500);
                          } else {
                              status.innerText = "Error: " + json.message;
                              status.style.color = "red";
                          }
                      })
                      .catch(err => {
                          document.getElementById('status').innerText = "CONNECTION ERROR.";
                          document.getElementById('status').style.color = "red";
                      });
                }
            </script>
        </body>
        </html>
        """;
    }
}
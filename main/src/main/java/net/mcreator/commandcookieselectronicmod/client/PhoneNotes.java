package net.mcreator.commandcookieselectronicmod.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import net.neoforged.fml.loading.FMLPaths;
import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;

/** Local notebook shared by this player's phone colors. Never sent over the network. */
public final class PhoneNotes {
    public static final PhoneNotes INSTANCE = new PhoneNotes();
    private final Path path = Path.of(System.getProperty("phone.notes.path", FMLPaths.CONFIGDIR.get().resolve("commandcookies_phone_notes.json").toString()));
    private String text = "";
    private boolean dirty, loadFailed;
    private PhoneNotes() {
        try {
            if (Files.exists(path)) {
                if (Files.size(path) > 65536) throw new IllegalStateException("Notes file too large");
                JsonObject data = new Gson().fromJson(Files.readString(path), JsonObject.class);
                text = data.get("notes").getAsString();
                if (text.length() > 4000) text = text.substring(0, 4000);
            }
        } catch (Exception ex) {
            loadFailed = true;
            CommandcookiesElectronicModMod.LOGGER.warn("Could not load phone notes; original file will be backed up before saving", ex);
        }
    }
    public String text() { return text; }
    public boolean dirty() { return dirty; }
    public void set(String value) { if (!value.equals(text)) { text = value.substring(0, Math.min(4000, value.length())); dirty = true; } }
    public boolean save() {
        if (!dirty) return true;
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            if (loadFailed && Files.exists(path)) {
                Files.copy(path, path.resolveSibling(path.getFileName() + ".backup-" + System.currentTimeMillis()));
                loadFailed = false;
            }
            JsonObject data = new JsonObject(); data.addProperty("notes", text);
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, new Gson().toJson(data));
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
            dirty = false; return true;
        } catch (Exception ex) {
            CommandcookiesElectronicModMod.LOGGER.warn("Could not save phone notes", ex); return false;
        }
    }
}

package com.ampznetwork.herobrine.haste;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.api.func.util.Debug;
import org.comroid.api.func.util.DelegateStream;
import org.comroid.api.net.Token;
import org.comroid.commands.Command;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Objects;

@Log
@Component
@Controller
@Command("haste")
@RequestMapping("/haste")
public class HasteService extends ListenerAdapter {
    private static final String URL_PREFIX;
    private static final File   BASE_DIR;

    static {
        URL_PREFIX = (Debug.isDebug()
                      ? "http://home.kaleidox.de:8080/"
                      : "https://herobrine.ampznetwork.com/") + "haste/";
        BASE_DIR   = Paths.get(System.getProperty("java.io.tmpdir"), "herobrine-haste").toFile();
        if (!BASE_DIR.exists() && !BASE_DIR.mkdirs()) throw new AssertionError();
    }

    @Command
    public String post(@Command.Arg File file) throws IOException {
        try (var fis = new FileInputStream(file)) {
            return URL_PREFIX + post(fis, file.getName());
        }
    }

    @PostMapping
    public ResponseEntity<String> post(RequestEntity<String> input) throws IOException {
        var filepath = input.getUrl().toURL().getFile();
        var body     = Objects.requireNonNull(input.getBody(), "no data");
        try (var sr = new StringReader(body); var dsi = new DelegateStream.Input(sr)) {
            var code = post(dsi, filepath);
            return ResponseEntity.ok(code);
        }
    }

    @Override
    @SneakyThrows
    public void onMessageReceived(MessageReceivedEvent event) {
        var message = event.getMessage();
        for (var attachment : message.getAttachments()) {
            var url = new URI(attachment.getUrl()).toURL();
            try (var uis = url.openStream()) {
                var id = post(uis, attachment.getFileName());
                announceDone(message, url.getFile(), id);
            }
        }
    }

    public String post(InputStream input, String filepath) throws IOException {
        var ext = fext(fname(filepath));
        var id  = newToken() + ext;
        var f   = new File(BASE_DIR, id);

        log.info("Transferring haste content to " + f.getAbsolutePath());
        try (var fos = new FileOutputStream(f)) {
            input.transferTo(fos);
        }

        return id;
    }

    @Command
    @ResponseBody
    @GetMapping("/{id}")
    public String get(@Command.Arg @PathVariable String id) throws IOException {
        var file = new File(BASE_DIR, id);
        log.info("Accessing haste content " + file.getAbsolutePath());
        try (var fis = new FileInputStream(file); var isr = new InputStreamReader(fis); var sw = new StringWriter()) {
            isr.transferTo(sw);
            return sw.toString();
        }
    }

    private String fname(String fpath) {
        fpath = new File(fpath).getName();
        var i = fpath.indexOf('?');
        if (i != -1) fpath = fpath.substring(0, i);
        return fpath;
    }

    private String fext(String fname) {
        var i = fname.lastIndexOf('.');
        if (i != -1) fname = fname.substring(i);
        return fname;
    }

    private void announceDone(Message message, String filepath, String id) {
        message.reply("File detected: [%s](%s)".formatted(fname(filepath), URL_PREFIX + id)).queue();
    }

    private String newToken() {
        return Token.generate(16, false, str -> !new File(BASE_DIR, str).exists()).toLowerCase();
    }
}

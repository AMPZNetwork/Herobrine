package com.ampznetwork.herobrine.feature.haste;

import com.ampznetwork.herobrine.feature.analyzer.MinecraftLogAnalyzer;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.annotations.Description;
import org.comroid.api.data.seri.MimeType;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.util.Debug;
import org.comroid.api.func.util.DelegateStream;
import org.comroid.api.net.Token;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Log
@Component
@Controller
@Command("haste")
@RequestMapping("/haste")
@Description("Haste file uploader")
public class HasteService extends ListenerAdapter implements HasteInteractionSource {
    public static final String URL_PREFIX;
    public static final File   BASE_DIR;
    public static final Emoji  EMOJI = Emoji.fromUnicode("\uD83D\uDD17"); // 🔗

    static {
        URL_PREFIX = (Debug.isDebug()
                      ? "http://home.kaleidox.de:8080/"
                      : "https://herobrine.ampznetwork.com/") + "haste/";
        BASE_DIR   = Paths.get(System.getProperty("java.io.tmpdir"), "herobrine-haste").toFile();
        if (!BASE_DIR.exists() && !BASE_DIR.mkdirs()) throw new AssertionError();
    }

    public static String fname(String fpath) {
        fpath = new File(fpath).getName();
        var i = fpath.indexOf('?');
        if (i != -1) fpath = fpath.substring(0, i);
        return fpath;
    }

    @Autowired                                   ApplicationContext   context;
    @Lazy @Autowired(required = false) @Nullable MinecraftLogAnalyzer analyzer;

    @Command
    @Description("Upload a file")
    public String post(@Command.Arg @Description("The file to upload") File file) throws IOException {
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
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isWebhookMessage() || event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
        var message = event.getMessage();
        if (message.getAttachments().isEmpty()) return;
        if (message.getAttachments()
                .stream()
                .map(Message.Attachment::getFileName)
                .noneMatch(fileName -> Stream.of("latest", "crash", "log", "txt").anyMatch(fileName::contains))) return;
        message.addReaction(EMOJI).queue();
    }

    @Override
    @SneakyThrows
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.getReaction().getEmoji().equals(EMOJI)) return;

        var message  = event.retrieveMessage().complete();
        var reaction = message.getReaction(EMOJI);

        if (reaction == null || event.getUser() instanceof SelfUser) return;

        for (var attachment : message.getAttachments()) {
            var url = new URI(attachment.getUrl()).toURL();
            try (var uis = url.openStream()) {
                var fileName = attachment.getFileName();
                var id       = post(uis, fileName);
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
    @GetMapping("/{id}")
    @Description("Get the contents of a file")
    public ResponseEntity<String> get(@Command.Arg @Description("The haste ID of the file") @PathVariable String id)
    throws IOException {
        var file = new File(BASE_DIR, id);
        log.info("Accessing haste content " + file.getAbsolutePath());
        String data;
        try (var fis = new FileInputStream(file); var isr = new InputStreamReader(fis); var sw = new StringWriter()) {
            isr.transferTo(sw);
            data = sw.toString();
        }
        return new ResponseEntity<>(data,
                MultiValueMap.fromSingleValue(Map.of("Content-Type",
                        MimeType.forExtension(fext(id)).orElse(MimeType.PLAIN).toString())),
                200);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    private void announceDone(Message message, String filepath, String id) {
        var actions = context.getBeansOfType(HasteInteractionSource.class)
                .values()
                .stream()
                .sorted(Comparator.comparing(ThrowingFunction.rethrowing(src -> src.getClass()
                        .getMethod("createHasteInteraction", String.class)), org.comroid.annotations.Order.COMPARATOR))
                .flatMap(src -> src.createHasteInteraction(id))
                .toList();
        message.reply(new MessageCreateBuilder().setContent("File uploaded: `%s`".formatted(fname(filepath)))
                .addComponents(ActionRow.of(actions))
                .build()).queue();
    }

    @Override
    @org.comroid.annotations.Order(Integer.MIN_VALUE)
    public Stream<ActionRowChildComponent> createHasteInteraction(String id) {
        return Stream.of(Button.link(URL_PREFIX + id, EMOJI.getFormatted() + " Open in Browser"));
    }

    private static String fext(String fname) {
        var i = fname.lastIndexOf('.');
        if (i != -1) fname = fname.substring(i);
        return fname;
    }

    private static String newToken() {
        return Token.generate(16, false, str -> !new File(BASE_DIR, str).exists()).toLowerCase();
    }
}

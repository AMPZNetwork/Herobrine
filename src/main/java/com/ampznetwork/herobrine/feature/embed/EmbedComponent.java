package com.ampznetwork.herobrine.feature.embed;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static net.dv8tion.jda.api.entities.MessageEmbed.*;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum EmbedComponent {
    TITLE("Embed title") {
        @Override
        public Modal createEditorModal(MessageEmbed embed) {
            return Modal.create(modalId(), "Editing embed title")
                    .addComponents(Label.of("Title",
                                    TextInput.create(OPTION_VALUE, TextInputStyle.SHORT)
                                            .setValue(embed.getTitle())
                                            .setMaxLength(TITLE_MAX_LENGTH)
                                            .setRequired(false)
                                            .build()),
                            Label.of("URL",
                                    TextInput.create(OPTION_URL, TextInputStyle.SHORT)
                                            .setValue(embed.getUrl())
                                            .setMaxLength(URL_MAX_LENGTH)
                                            .setRequired(false)
                                            .build()))
                    .build();
        }

        @Override
        public void handleInteraction(EmbedBuilder embed, ModalInteractionEvent event) {
            var value = Objects.requireNonNull(event.getValue(OPTION_VALUE), "modal value mapping").getAsString();

            if (value == null || value.isBlank()) {
                embed.setTitle(null, null);
                return;
            }

            embed.setTitle(value);

            var urlMapping = event.getValue(OPTION_URL);
            if (urlMapping != null) embed.setUrl(urlMapping.getAsString());
        }
    }, DESCRIPTION("Embed description") {
        @Override
        public Modal createEditorModal(MessageEmbed embed) {
            return Modal.create(modalId(), "Editing embed description")
                    .addComponents(Label.of("Description",
                            TextInput.create(OPTION_VALUE, TextInputStyle.PARAGRAPH)
                                    .setValue(embed.getDescription())
                                    .setMaxLength(4000 /*DESCRIPTION_MAX_LENGTH*/)
                                    .setRequired(false)
                                    .build()))
                    .build();
        }

        @Override
        public void handleInteraction(EmbedBuilder embed, ModalInteractionEvent event) {
            var value = Objects.requireNonNull(event.getValue(OPTION_VALUE), "modal value mapping").getAsString();

            if (value == null || value.isBlank()) {
                embed.setDescription(null);
                return;
            }

            embed.setDescription(value);
        }
    }, TIMESTAMP("Embed timestamp") {
        public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        @Override
        public Modal createEditorModal(MessageEmbed embed) {
            return Modal.create(modalId(), "Editing embed timestamp")
                    .addComponents(Label.of("Timestamp",
                            TextInput.create(OPTION_VALUE, TextInputStyle.SHORT)
                                    .setValue(embed.getTimestamp() == null
                                              ? LocalDateTime.now().format(FORMAT)
                                              : embed.getTimestamp().format(FORMAT))
                                    .setRequired(false)
                                    .build()))
                    .build();
        }

        @Override
        public void handleInteraction(EmbedBuilder embed, ModalInteractionEvent event) {
            var value = Objects.requireNonNull(event.getValue(OPTION_VALUE), "modal value mapping").getAsString();

            if (value == null || value.isBlank()) {
                embed.setTimestamp(null);
                return;
            }

            var time = FORMAT.parse(value);
            embed.setTimestamp(time);
        }
    }, COLOR("Embed color") {
        @Override
        public Modal createEditorModal(MessageEmbed embed) {
            return Modal.create(modalId(), "Editing embed color")
                    .addComponents(Label.of("Color",
                            TextInput.create(OPTION_VALUE, TextInputStyle.SHORT)
                                    .setValue(embed.getColor() == null
                                              ? null
                                              : '#' + Integer.toHexString(embed.getColor().getRGB()))
                                    .setRequired(false)
                                    .build()))
                    .build();
        }

        @Override
        public void handleInteraction(EmbedBuilder embed, ModalInteractionEvent event) {
            var value = Objects.requireNonNull(event.getValue(OPTION_VALUE), "modal value mapping").getAsString();

            if (value == null || value.isBlank()) {
                embed.setColor(null);
                return;
            }

            if (!value.matches("#?[0-9a-fA-F]{1,6}")) throw new NumberFormatException("Invalid hex string: " + value);
            if (value.startsWith("#")) value = value.substring(1);

            var hex = Integer.parseInt(value, 16);
            embed.setColor(hex);
        }
    }, THUMBNAIL("Embed thumbnail") {
        @Override
        public Modal createEditorModal(MessageEmbed embed) {
            return Modal.create(modalId(), "Editing embed thumbnail")
                    .addComponents(Label.of("Thumbnail URL",
                            TextInput.create(OPTION_VALUE, TextInputStyle.SHORT)
                                    .setValue(embed.getThumbnail() == null ? null : embed.getThumbnail().getUrl())
                                    .setMaxLength(URL_MAX_LENGTH)
                                    .setRequired(false)
                                    .build()))
                    .build();
        }

        @Override
        public void handleInteraction(EmbedBuilder embed, ModalInteractionEvent event) {
            var value = Objects.requireNonNull(event.getValue(OPTION_VALUE), "modal value mapping").getAsString();

            if (value == null || value.isBlank()) {
                embed.setThumbnail(null);
                return;
            }

            embed.setThumbnail(value);
        }
    }, AUTHOR("Embed author") {
        @Override
        public Modal createEditorModal(MessageEmbed embed) {
            var author = embed.getAuthor();
            return Modal.create(modalId(), "Editing embed author")
                    .addComponents(Label.of("Author Name",
                                    TextInput.create(OPTION_NAME, TextInputStyle.SHORT)
                                            .setValue(author == null ? null : author.getName())
                                            .setMaxLength(AUTHOR_MAX_LENGTH)
                                            .setRequired(false)
                                            .build()),
                            Label.of("Author URL",
                                    TextInput.create(OPTION_URL, TextInputStyle.SHORT)
                                            .setValue(author == null ? null : author.getUrl())
                                            .setMaxLength(URL_MAX_LENGTH)
                                            .setRequired(false)
                                            .build()),
                            Label.of("Author Icon URL",
                                    TextInput.create(OPTION_ICON_URL, TextInputStyle.SHORT)
                                            .setValue(author == null ? null : author.getIconUrl())
                                            .setMaxLength(URL_MAX_LENGTH)
                                            .setRequired(false)
                                            .build()))
                    .build();
        }

        @Override
        public void handleInteraction(EmbedBuilder embed, ModalInteractionEvent event) {
            var name = Objects.requireNonNull(event.getValue(OPTION_NAME), "modal name mapping").getAsString();

            if (name == null || name.isBlank()) {
                embed.setAuthor(null, null, null);
                return;
            }

            var url = Objects.requireNonNull(event.getValue(OPTION_URL), "modal url mapping").getAsString();
            var iconUrl = Objects.requireNonNull(event.getValue(OPTION_ICON_URL), "modal icon url mapping")
                    .getAsString();

            var hasUrl  = url != null && !url.isBlank();
            var hasIcon = iconUrl != null && !iconUrl.isBlank();

            if (hasUrl && hasIcon) embed.setAuthor(name, url, iconUrl);
            else if (hasUrl && !hasIcon) embed.setAuthor(name, url);
            else embed.setAuthor(name);
        }
    }, FOOTER("Embed footer") {
        @Override
        public Modal createEditorModal(MessageEmbed embed) {
            var footer = embed.getFooter();
            return Modal.create(modalId(), "Editing embed footer")
                    .addComponents(Label.of("Footer Text",
                                    TextInput.create(OPTION_VALUE, TextInputStyle.SHORT)
                                            .setValue(footer == null ? null : footer.getText())
                                            .setMaxLength(TEXT_MAX_LENGTH)
                                            .setRequired(false)
                                            .build()),
                            Label.of("Footer Icon URL",
                                    TextInput.create(OPTION_ICON_URL, TextInputStyle.SHORT)
                                            .setValue(footer == null ? null : footer.getIconUrl())
                                            .setMaxLength(URL_MAX_LENGTH)
                                            .setRequired(false)
                                            .build()))
                    .build();
        }

        @Override
        public void handleInteraction(EmbedBuilder embed, ModalInteractionEvent event) {
            var value = Objects.requireNonNull(event.getValue(OPTION_VALUE), "modal value mapping").getAsString();

            if (value == null || value.isBlank()) {
                embed.setFooter(null, null);
                return;
            }

            var iconUrl = Objects.requireNonNull(event.getValue(OPTION_ICON_URL), "modal icon url mapping")
                    .getAsString();

            if (iconUrl != null && !iconUrl.isBlank()) embed.setFooter(value, iconUrl);
            else embed.setFooter(value);
        }
    }, IMAGE("Embed image") {
        @Override
        public Modal createEditorModal(MessageEmbed embed) {
            return Modal.create(modalId(), "Editing embed image")
                    .addComponents(Label.of("Image URL",
                            TextInput.create(OPTION_VALUE, TextInputStyle.SHORT)
                                    .setValue(embed.getImage() == null ? null : embed.getImage().getUrl())
                                    .setRequired(false)
                                    .build()))
                    .build();
        }

        @Override
        public void handleInteraction(EmbedBuilder embed, ModalInteractionEvent event) {
            var value = Objects.requireNonNull(event.getValue(OPTION_VALUE), "modal value mapping").getAsString();

            if (value == null || value.isBlank()) {
                embed.setImage(null);
                return;
            }

            embed.setImage(value);
        }
    };

    public static final String MODAL_ID        = "embed-editor-";
    public static final String OPTION_VALUE    = "option-value";
    public static final String OPTION_NAME     = "option-name";
    public static final String OPTION_URL      = "option-url";
    public static final String OPTION_ICON_URL = "option-iconurl";

    SelectOption selectOption;

    EmbedComponent(String label) {
        this.selectOption = SelectOption.of(label, name());
    }

    public abstract Modal createEditorModal(MessageEmbed embed);

    public abstract void handleInteraction(EmbedBuilder embed, ModalInteractionEvent event);

    protected String modalId() {
        return MODAL_ID + name();
    }
}

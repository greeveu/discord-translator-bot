package eu.greev.translator.listener;

import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.deepl.api.Usage;
import eu.greev.translator.classes.TranslationLimitReachedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class TranslationListener extends ListenerAdapter {
    private final MessageEmbed defaultEmbed = new EmbedBuilder().setColor(new Color(63, 226, 69, 255)).build();
    private final List<Long> alreadyTranslated = new ArrayList<>();
    private final Translator translator;
    private final int cooldownMinutes;
    private final int maxMessages;

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        JDA jda = event.getJDA();
        if (!jda.retrieveCommands().complete().stream().map(Command::getName).toList().contains(event.getName())
                || event.getTarget().getAuthor().equals(jda.getSelfUser())) {
            return;
        }

        Message message = event.getTarget();
        if (alreadyTranslated.contains(message.getIdLong())) {
            event.replyEmbeds(new EmbedBuilder(defaultEmbed)
                    .setDescription("This message already got recently public translated.")
                    .build()
            ).setEphemeral(true).queue();
            return;
        }

        if (alreadyTranslated.size() >= maxMessages) {
            event.reply(String.format("The maximum of %s translated messages per %d minutes has been reached, please wait until you can use translations again", maxMessages, cooldownMinutes))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        TextResult result;
        try {
            result = getTranslatedResult(message, event.getUser().getName());
        } catch (TranslationLimitReachedException e) {
            event.reply("Translating this message would exceed the monthly translation limit.").setEphemeral(true).queue();
            return;
        }
        if (result == null) {
            event.reply("Could not translate text, please contact the server owner").setEphemeral(true).queue();
            return;
        }

        if (event.getName().equals("Translate! (silent)")) {
            event.reply(result.getText()).setEphemeral(true).queue();
            return;
        }

        message.reply(result.getText()).queue(s -> {
            alreadyTranslated.add(s.getIdLong());
            startCooldownTimer(s.getIdLong());
        });
        event.replyEmbeds(new EmbedBuilder(defaultEmbed)
                .setDescription(String.format("Successfully translated text from `%s` to `EN-GB`", result.getDetectedSourceLanguage()))
                .build()
        ).setEphemeral(true).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        JDA jda = event.getJDA();

        MessageReference reference = message.getMessageReference();
        if (!message.getMentions().isMentioned(jda.getSelfUser(), Message.MentionType.USER) || reference == null
                || event.getAuthor().equals(jda.getSelfUser())) {
            return;
        }

        Message target = reference.resolve().complete();
        if (alreadyTranslated.contains(target.getIdLong())) {
            message.replyEmbeds(new EmbedBuilder(defaultEmbed)
                    .setDescription("This message already got recently public translated.")
                    .build()
            ).queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
            return;
        }

        if (alreadyTranslated.size() >= maxMessages) {
            message.reply(String.format("The maximum of %s translated messages per %d minutes has been reached, please wait until you can use translations again", maxMessages, cooldownMinutes))
                    .queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
            return;
        }

        TextResult result;
        try {
            result = getTranslatedResult(target, event.getAuthor().getName());
        } catch (TranslationLimitReachedException e) {
            message.reply("Translating this message would exceed the monthly translation limit.").queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
            return;
        }
        if (result == null) {
            message.reply("Could not translate text, please contact the server owner!").queue();
            return;
        }
        target.reply(result.getText()).queue(s -> {
            alreadyTranslated.add(s.getIdLong());
            startCooldownTimer(s.getIdLong());
        });
    }

    private TextResult getTranslatedResult(Message message, String requester) throws TranslationLimitReachedException {
        String toTranslate = message.getContentRaw();
        try {
            Usage.Detail character = translator.getUsage().getCharacter();
            if (character != null && (character.getCount() + toTranslate.split(" ").length) > character.getLimit()) {
                throw new TranslationLimitReachedException("The translation word limit of " + character.getLimit() + " got reached.");
            }
            log.info(requester + " requested a translation.");
            return translator.translateText(toTranslate, null, "EN-GB");
        } catch (DeepLException | InterruptedException e) {
            log.error("Could not translate text:", e);
        }
        return null;
    }

    private void startCooldownTimer(long messageId) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                alreadyTranslated.remove(messageId);
            }
        }, TimeUnit.MINUTES.toMillis(cooldownMinutes));
    }
}
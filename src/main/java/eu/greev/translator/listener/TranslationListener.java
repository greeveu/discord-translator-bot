package eu.greev.translator.listener;

import com.deepl.api.TextResult;
import eu.greev.translator.classes.exceptions.TranslationLimitReachedException;
import eu.greev.translator.classes.services.TranslationService;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class TranslationListener extends ListenerAdapter {
    private final MessageEmbed defaultEmbed = new EmbedBuilder().setColor(new Color(63, 226, 69, 255)).build();
    private final TranslationService translationService;

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        JDA jda = event.getJDA();
        if (!jda.retrieveCommands().complete().stream().map(Command::getName).toList().contains(event.getName())
                || event.getTarget().getAuthor().equals(jda.getSelfUser())) {
            return;
        }

        Message message = event.getTarget();
        if (translationService.inCache(message.getIdLong())) {
            event.replyEmbeds(new EmbedBuilder(defaultEmbed)
                    .setDescription("This message already got recently public translated.")
                    .build()
            ).setEphemeral(true).queue();
            return;
        }

        if (translationService.hasCooldown(event.getUser().getIdLong())) {
            event.reply(String.format(
                            "Your maximum of %s translated messages per %d minutes has been reached, please wait until you can use translations again",
                            translationService.getMaxMessages(),
                            translationService.getCooldownMinutes()
                    ))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        TextResult result;
        try {
            result = translationService.getTranslatedResult(message, event.getUser().getName());
        } catch (TranslationLimitReachedException e) {
            event.reply("The monthly translation limit is reached. Sorry.").setEphemeral(true).queue();
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

        translationService.sendTranslation(result.getText(), message, event.getUser().getIdLong());

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
        if (translationService.inCache(target.getIdLong())) {
            message.replyEmbeds(new EmbedBuilder(defaultEmbed)
                    .setDescription("This message already got recently public translated.")
                    .build()
            ).queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
            return;
        }

        if (translationService.hasCooldown(event.getAuthor().getIdLong())) {
            message.reply(String.format(
                            "Your maximum of %s translated messages per %d minutes has been reached, please wait until you can use translations again",
                            translationService.getMaxMessages(),
                            translationService.getCooldownMinutes()
                    ))
                    .queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
            return;
        }

        TextResult result;
        try {
            result = translationService.getTranslatedResult(target, event.getAuthor().getName());
        } catch (TranslationLimitReachedException e) {
            message.reply("The monthly translation limit is reached. Sorry.").queue(s -> s.delete().queueAfter(1, TimeUnit.MINUTES));
            return;
        }
        if (result == null) {
            message.reply("Could not translate text, please contact the server owner!").queue();
            return;
        }

        translationService.sendTranslation(result.getText(), target, event.getAuthor().getIdLong());
    }
}
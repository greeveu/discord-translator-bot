package eu.greev.translator.listener;

import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.deepl.api.Usage;
import eu.greev.translator.Main;
import eu.greev.translator.classes.TranslationLimitReachedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

@Slf4j
@RequiredArgsConstructor
public class TranslationListener extends ListenerAdapter {
    private final MessageEmbed defaultEmbed = new EmbedBuilder().setColor(new Color(63, 226, 69, 255)).build();
    private final Translator translator = Main.getTranslator();

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        if (!event.getName().equals("Translate!")) return;

        Message message = event.getTarget();
        TextResult result;
        try {
            result = getTranslatedResult(message, event.getUser().getName());
        } catch (TranslationLimitReachedException e) {
            event.reply("Translating this message would exceed the monthly translation limit.").queue();
            return;
        }
        if (result == null) {
            event.reply("Could not translate text, please contact the server owner").setEphemeral(true).queue();
            return;
        }
        message.reply(result.getText()).queue();
        event.replyEmbeds(new EmbedBuilder(defaultEmbed)
                .setDescription(String.format("Successfully translated text from `%s` to `EN-GB`", result.getDetectedSourceLanguage()))
                .build()
        ).setEphemeral(true).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageReference reference = event.getMessage().getMessageReference();
        if (!event.getMessage().getMentions().isMentioned(event.getJDA().getSelfUser(), Message.MentionType.USER)
                || reference == null || event.getAuthor().equals(event.getJDA().getSelfUser())) return;

        Message message = reference.resolve().complete();
        TextResult result;
        try {
            result = getTranslatedResult(message, event.getAuthor().getName());
        } catch (TranslationLimitReachedException e) {
            event.getMessage().reply("Translating this message would exceed the monthly translation limit.").queue();
            return;
        }
        if (result == null) {
            event.getMessage().reply("Could not translate text, please contact the server owner!").queue();
            return;
        }
        message.reply(result.getText()).queue();
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
}
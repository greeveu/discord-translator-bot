package eu.greev.translator.classes.services;

import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.deepl.api.Usage;
import eu.greev.translator.classes.entites.TranslationCooldown;
import eu.greev.translator.classes.exceptions.TranslationLimitReachedException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class TranslationService {
    private final List<TranslationCooldown> translatedCache = new ArrayList<>();
    private final Translator translator;
    @Getter
    private final int cooldownMinutes;
    @Getter
    private final int maxMessages;

    public void sendTranslation(String text, Message replyTo, long userId) {
        replyTo.reply(text).queue(s -> translatedCache.add(new TranslationCooldown(s.getIdLong(), Instant.now(), userId)));
    }

    public boolean hasCooldown(long userId) {
        AtomicInteger translatedMessages = new AtomicInteger();

        translatedCache.stream()
                .filter(e -> e.userId() == userId)
                .forEach(e -> {
                    if (Duration.between(Instant.now(), e.timeTranslated()).toMinutes() < cooldownMinutes) {
                        translatedMessages.getAndIncrement();
                        return;
                    }
                    translatedCache.remove(e);
                });
        return translatedMessages.get() >= maxMessages;
    }

    public boolean inCache(long messageId) {
        return translatedCache.stream().anyMatch(e -> e.messageId() == messageId);
    }

    public TextResult getTranslatedResult(Message message, String requester) throws TranslationLimitReachedException {
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
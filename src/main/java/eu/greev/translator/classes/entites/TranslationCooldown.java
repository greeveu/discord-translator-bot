package eu.greev.translator.classes.entites;

import java.time.Instant;

public record TranslationCooldown(long messageId, Instant timeTranslated, long userId) {}
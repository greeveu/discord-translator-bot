package eu.greev.translator;

import com.deepl.api.Translator;
import eu.greev.translator.listener.TranslationListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.util.Strings;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
public class Main {
    @Getter private static Translator translator = null;
    private static final List<String> PATHS = List.of("token", "auth");

    public static void main(String[] args) throws IOException, InterruptedException {
        PropertyConfigurator.configure(Main.class.getClassLoader().getResourceAsStream("log4j2.properties"));
        JDA jda = null;

        File file = new File("./Translator/config.yml");
        new File("./Translator").mkdirs();
        if (!file.exists()) file.createNewFile();
        YamlFile config = YamlFile.loadConfiguration(file);

        for (String path : PATHS) {
            if (!config.isSet(path) || Strings.isEmpty(config.getString(path))) {
                log.error(String.format("No valid config provided! Add your token into `./Translator/config.yml` with the key `%s`", path));
                System.exit(1);
            }
        }
        translator = new Translator(config.getString("auth"));

        try {
            jda = JDABuilder.create(config.getString("token"),
                            List.of(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES))
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
                    .setActivity(Activity.playing(" with translations."))
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setStatus(OnlineStatus.ONLINE)
                    .build();
        } catch (InvalidTokenException e) {
            log.error("Bot could not be initialized", e);
            System.exit(1);
        }
        jda.awaitReady();
        jda.addEventListener(new TranslationListener());
        jda.updateCommands().addCommands(Commands.message("Translate!")).queue();
    }
}
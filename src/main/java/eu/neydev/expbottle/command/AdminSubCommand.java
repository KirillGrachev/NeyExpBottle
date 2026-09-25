package eu.neydev.expbottle.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Одна подкоманда {@code /neyexpbottle}.
 *
 * <p>{@link AdminCommand} остаётся диспетчером: выбирает подкоманду по первому
 * аргументу и передаёт ей управление. Общая инфраструктура (сервисы, сообщения,
 * права, подсказки) приходит из {@link CommandContext}, поэтому каждая подкоманда
 * занимается только своей логикой.</p>
 */
public abstract class AdminSubCommand {

    protected final CommandContext context;

    protected AdminSubCommand(@NotNull CommandContext context) {
        this.context = context;
    }

    /**
     * Имя подкоманды (первый аргумент), в нижнем регистре.
     */
    public abstract @NotNull String name();

    /**
     * Выполняет подкоманду.
     *
     * @param sender отправитель
     * @param args   все аргументы команды, {@code args[0]} — имя подкоманды
     */
    public abstract void execute(@NotNull CommandSender sender, String @NotNull [] args);

    /**
     * Подсказки для аргументов подкоманды (без первого — имени подкоманды).
     *
     * @param sender отправитель
     * @param args   все аргументы команды
     * @return варианты подсказки или пустой список
     */
    public @NotNull List<String> suggest(@NotNull CommandSender sender, String @NotNull [] args) {
        return Collections.emptyList();
    }
}
package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Счётчики, диагностика и журнал.
 *
 * <p>Заменяет {@code e.printStackTrace()} из старой версии: стектрейсы уходят
 * в лог сервера, а отладочные сообщения включаются одним флагом
 * {@code settings.debug}.</p>
 */
public class DiagnosticsService {

    private final Logger logger;
    private final PluginConfig config;

    private final AtomicLong bottlesCreated = new AtomicLong();
    private final AtomicLong bottlesUsed = new AtomicLong();
    private final AtomicLong exchangesDenied = new AtomicLong();
    private final AtomicLong suspiciousEvents = new AtomicLong();
    private final AtomicLong menusOpened = new AtomicLong();

    public DiagnosticsService(@NotNull Logger logger, @NotNull PluginConfig config) {
        this.logger = logger;
        this.config = config;
    }

    /**
     * Сообщение, которое видно только при {@code settings.debug: true}.
     */
    public void debug(@NotNull String message) {

        if (config.isDebugEnabled()) {
            logger.info("[debug] " + message);
        }

    }

    public void info(@NotNull String message) {
        logger.info(message);
    }

    public void warning(@NotNull String message) {
        logger.warning(message);
    }

    public void severe(@NotNull String message, @Nullable Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    /**
     * Учитывает подозрительное действие без записи в консоль.
     *
     * <p>Защита от дюпа работает молча: клик в креативе может повторяться
     * десятки раз подряд, и консольный вывод только засорял бы лог.
     * Значение видно в {@code /neyexpbottle info}.</p>
     */
    public void countSuspicious() {
        suspiciousEvents.incrementAndGet();
    }

    /**
     * Подозрительное действие с записью в консоль: повреждённая метка,
     * рассинхрон опыта, поддельная бутылка.
     */
    public void suspicious(@NotNull String message) {

        suspiciousEvents.incrementAndGet();
        logger.warning("[security] " + message);

    }

    /**
     * Замеряет длительность задачи в миллисекундах.
     *
     * @param task задача
     * @return потрачено миллисекунд
     */
    public long measure(@NotNull Runnable task) {

        long start = System.nanoTime();
        task.run();

        return (System.nanoTime() - start) / 1_000_000L;

    }

    public void incrementBottlesCreated() {
        bottlesCreated.incrementAndGet();
    }

    public void incrementBottlesUsed() {
        bottlesUsed.incrementAndGet();
    }

    public void incrementExchangesDenied() {
        exchangesDenied.incrementAndGet();
    }

    public void incrementMenusOpened() {
        menusOpened.incrementAndGet();
    }

    public long getBottlesCreated() {
        return bottlesCreated.get();
    }

    public long getBottlesUsed() {
        return bottlesUsed.get();
    }

    public long getExchangesDenied() {
        return exchangesDenied.get();
    }

    public long getSuspiciousEvents() {
        return suspiciousEvents.get();
    }

    public long getMenusOpened() {
        return menusOpened.get();
    }

    /**
     * Сбрасывает счётчики (используется при перезагрузке, если нужно начать с нуля).
     */
    public void resetCounters() {

        bottlesCreated.set(0L);
        bottlesUsed.set(0L);
        exchangesDenied.set(0L);
        suspiciousEvents.set(0L);
        menusOpened.set(0L);

    }
}

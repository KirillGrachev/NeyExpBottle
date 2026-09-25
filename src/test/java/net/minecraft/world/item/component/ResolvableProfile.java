package net.minecraft.world.item.component;

import net.minecraft.util.com.mojang.authlib.GameProfile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Заглушка {@code ResolvableProfile} для тестов: набор конструкторов повторяет
 * разброс сигнатур между 1.20.5 и 1.21.x, который перебирает
 * {@code GameProfileFactory.newResolvableProfile}.
 *
 * <p>Порядок объявления важен: перебор конструкторов идёт по порядку, и каждый
 * вариант сигнатуры обязан встретить свою ветку (пустая, неподдерживаемый тип,
 * падающий вызов, полная сигнатура).</p>
 */
public class ResolvableProfile {

    private static final AtomicInteger FULL_SIGNATURE_ATTEMPTS = new AtomicInteger();

    private final GameProfile profile;

    public ResolvableProfile() {
        this.profile = null;
    }

    public ResolvableProfile(String unsupported) {
        throw new UnsupportedOperationException("stub: unreachable, fillArguments rejects the signature");
    }

    public ResolvableProfile(GameProfile profile) {
        throw new UnsupportedOperationException("stub: single-profile signature fails on this core");
    }

    public ResolvableProfile(GameProfile profile, CompletableFuture<?> future, boolean flag, int depth) {

        // Первая попытка полной сигнатуры отказывает: перебор обязан дойти
        // до конца списка и вернуть null, повторная — работает
        if (FULL_SIGNATURE_ATTEMPTS.incrementAndGet() == 1) {
            throw new UnsupportedOperationException("stub: the full signature refuses once");
        }

        this.profile = profile;

    }

    /**
     * Сбрасывает счётчик попыток: тест начинает перебор с известного состояния.
     */
    public static void resetStub() {
        FULL_SIGNATURE_ATTEMPTS.set(0);
    }

    public GameProfile profile() {
        return profile;
    }
}

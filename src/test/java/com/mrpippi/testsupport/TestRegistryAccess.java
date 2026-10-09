package com.autopickup.testsupport;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test-only RegistryAccess, loaded by Paper via ServiceLoader
 * (META-INF/services/io.papermc.paper.registry.RegistryAccess).
 *
 * <p>Paper's {@code Material.isAir()} looks up the block registry, which normally
 * requires a running server. This stub returns a BlockType whose {@code isAir()} is true
 * for air, cave_air and void_air; every other registry is an empty mock.
 */
public class TestRegistryAccess implements RegistryAccess {

    private static final Set<String> AIR = Set.of("air", "cave_air", "void_air");

    /** Still abstract in Paper's interface, but deprecated for removal there; Paper itself uses the RegistryKey overload. */
    @Override
    @SuppressWarnings({"unchecked", "removal"})
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return mock(Registry.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> key) {
        Registry<T> registry = mock(Registry.class);
        if (key == RegistryKey.BLOCK) {
            when(registry.get(any(NamespacedKey.class))).thenAnswer(inv -> {
                BlockType block = mock(BlockType.class);
                when(block.isAir()).thenReturn(AIR.contains(inv.getArgument(0, NamespacedKey.class).getKey()));
                return block;
            });
        }
        return registry;
    }
}

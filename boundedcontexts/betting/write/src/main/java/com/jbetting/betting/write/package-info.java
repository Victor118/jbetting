/**
 * Write side of the betting context: commands, their handlers and the aggregates they change.
 * <ul>
 *   <li>{@code businesslogic.models} — aggregates, value objects, domain events and their ports. Pure Java.</li>
 *   <li>{@code businesslogic.usecases} — command handlers. Pure Java.</li>
 *   <li>{@code adapters.primary} — controllers. {@code adapters.secondary} — port implementations (JPA, fakes, time).</li>
 *   <li>{@code configuration} — Spring wiring of the use cases and adapters.</li>
 * </ul>
 */
package com.jbetting.betting.write;

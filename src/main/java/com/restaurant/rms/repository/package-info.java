/**
 * Spring Data JPA repository interfaces.
 *
 * <p>Each interface extends {@code JpaRepository<Entity, ID>} and optionally
 * {@code JpaSpecificationExecutor<Entity>} for dynamic filtering.
 * Custom JPQL queries use {@code @Query}; native SQL queries use
 * {@code @Query(nativeQuery = true)}.
 * No implementation classes are needed — Spring Data generates them at runtime.
 */
package com.restaurant.rms.repository;

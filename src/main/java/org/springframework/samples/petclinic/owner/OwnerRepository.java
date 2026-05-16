/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the {@link Owner} aggregate.
 *
 * <p>
 * This repository acts as the persistence-layer entry point for the {@code Owner}
 * aggregate root in the PetClinic domain. By extending {@link JpaRepository} it inherits
 * the standard set of CRUD operations ({@code save}, {@code findAll}, {@code deleteById},
 * etc.) as well as pagination and sorting support, so the controller layer does not need
 * to know anything about JPA, {@code EntityManager}, or transactional boundaries — Spring
 * Data generates a proxy implementation of this interface at application startup.
 * </p>
 *
 * <p>
 * Two additional finder methods are declared explicitly:
 * </p>
 * <ul>
 * <li>{@link #findByLastNameStartingWith(String, Pageable)} — used by
 * {@code OwnerController.processFindForm} to perform a paginated prefix search over
 * surnames when the user submits the "Find Owners" form.</li>
 * <li>{@link #findById(Integer)} — overrides the default {@code JpaRepository.findById}
 * signature in order to document the {@link Optional}-returning contract that the
 * controller layer relies on (it calls
 * {@link Optional#orElseThrow(java.util.function.Supplier)} to surface a meaningful error
 * when an unknown id is requested).</li>
 * </ul>
 *
 * <p>
 * All method names follow the Spring Data query-derivation naming conventions described
 * in the reference documentation, so additional finders can be added simply by declaring
 * a method signature here. See: <a href=
 * "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation">Spring
 * Data JPA — Query Creation</a>.
 * </p>
 *
 * <p>
 * Because Owner has a {@code @OneToMany} cascade-all relationship with {@code Pet}, and
 * Pet in turn has a {@code @OneToMany} cascade-all relationship with {@code Visit}, a
 * single {@link #save(Object)} call against an {@code Owner} persists the entire object
 * graph (owner → pets → visits) in one transaction.
 * </p>
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Wick Dynex
 */
public interface OwnerRepository extends JpaRepository<Owner, Integer> {

	/**
	 * Retrieve {@link Owner}s from the data store whose last name <em>starts with</em>
	 * the given prefix, returned as a {@link Page} for pagination support.
	 *
	 * <p>
	 * This finder is invoked by {@code OwnerController.processFindForm} when the user
	 * submits the "Find Owners" form. The prefix match is case-sensitive and translates
	 * to a SQL {@code LIKE 'prefix%'} clause behind the scenes. Passing an empty string
	 * therefore matches every owner — this is intentional and is what powers the "browse
	 * all owners" behaviour when the form is submitted with no surname.
	 * </p>
	 * @param lastName the surname prefix to search for; an empty string matches every
	 * owner
	 * @param pageable pagination and sorting information; must not be {@code null}. Use
	 * {@link Pageable#unpaged()} to retrieve all matches in a single page.
	 * @return a {@link Page} of matching {@link Owner}s — never {@code null}; the page
	 * will be empty (not {@code null}) if no owners match.
	 */
	Page<Owner> findByLastNameStartingWith(String lastName, Pageable pageable);

	/**
	 * Retrieve a single {@link Owner} from the data store by its primary key.
	 *
	 * <p>
	 * This declaration overrides the default {@link JpaRepository#findById(Object)}
	 * signature purely to document the {@link Optional}-returning contract that the
	 * controller layer depends on. Controllers typically unwrap the {@link Optional} with
	 * {@link Optional#orElseThrow(java.util.function.Supplier)} to throw an
	 * {@link IllegalArgumentException} (mapped to an HTTP 400/404 by Spring's default
	 * error handling) when an owner id from the URL does not exist.
	 * </p>
	 *
	 * <p>
	 * The returned {@code Owner}, if present, is fully initialised: because the
	 * {@code pets} association uses {@code FetchType.EAGER}, the owner's pets (and, via
	 * cascade, their visits) are loaded in the same query. Callers can therefore safely
	 * navigate the object graph after the transaction has closed.
	 * </p>
	 * @param id the primary key of the owner to load
	 * @return an {@link Optional} containing the {@link Owner} if one exists with the
	 * given id, or an empty {@link Optional} if no such owner is present in the data
	 * store
	 * @throws IllegalArgumentException if {@code id} is {@code null}
	 */
	Optional<Owner> findById(Integer id);

}

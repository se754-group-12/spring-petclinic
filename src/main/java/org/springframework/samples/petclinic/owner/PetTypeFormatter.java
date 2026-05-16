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

import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Spring MVC {@link Formatter} that converts between {@link PetType} instances and their
 * string representation.
 *
 * <p>
 * This formatter sits between the HTML form and the {@link Pet} entity, plugging into
 * Spring's conversion service so that {@code <select>} dropdowns of pet types can be
 * round-tripped through form submission without any boilerplate in the controller. As a
 * {@link Component} it is auto-detected during classpath scanning and registered with the
 * global {@code FormattingConversionService}.
 * </p>
 *
 * <h2>Conversion direction</h2>
 * <ul>
 * <li><b>Print</b> ({@link #print(PetType, Locale)}) — invoked when rendering a form, to
 * turn a {@link PetType} into a display string that ends up inside a {@code <option>}
 * value or label.</li>
 * <li><b>Parse</b> ({@link #parse(String, Locale)}) — invoked on form submission to turn
 * the string sent by the browser back into a managed {@link PetType} entity loaded from
 * the database. The lookup is delegated to {@link PetTypeRepository#findPetTypes()}.</li>
 * </ul>
 *
 * <h2>Why a Formatter instead of a PropertyEditor?</h2>
 * <p>
 * Starting with Spring 3.0, {@link Formatter} replaced the legacy
 * {@code java.beans.PropertyEditor} mechanism. Formatters are stateless and thread-safe,
 * are locale-aware, and integrate cleanly with the {@code ConversionService} used by
 * Spring MVC, Spring Data, and Spring Expression Language. See the <a href=
 * "https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#format">Spring
 * Framework reference — Field Formatting</a>.
 * </p>
 *
 * <h2>Thread-safety</h2>
 * <p>
 * This class holds only an immutable reference to the (thread-safe) repository and
 * therefore is itself thread-safe. Spring instantiates it as a singleton.
 * </p>
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Michael Isvy
 */
@Component
public class PetTypeFormatter implements Formatter<PetType> {

	private final PetTypeRepository types;

	/**
	 * Creates a new {@code PetTypeFormatter} backed by the given repository.
	 *
	 * <p>
	 * Constructor injection (rather than field injection) is used so the repository
	 * reference can be marked {@code final}, making the formatter trivially immutable and
	 * easier to unit test by passing a Mockito mock.
	 * </p>
	 * @param types the repository used to resolve {@link PetType} instances by name
	 * during {@linkplain #parse(String, Locale) parsing}; must not be {@code null}
	 */
	public PetTypeFormatter(PetTypeRepository types) {
		this.types = types;
	}

	/**
	 * Renders the given {@link PetType} as a display string.
	 *
	 * <p>
	 * Returns the pet type's {@code name} verbatim, or the literal string
	 * {@code "<null>"} as a defensive fallback if the entity somehow has no name set
	 * (this should not occur for persisted types but keeps the formatter safe from
	 * {@link NullPointerException}s when used against in-memory test fixtures).
	 * </p>
	 *
	 * <p>
	 * The {@code locale} parameter is required by the {@link Formatter} contract but is
	 * unused here because pet-type names are not localised.
	 * </p>
	 * @param petType the {@link PetType} to render; must not be {@code null}
	 * @param locale the locale of the current user; accepted for contract conformance but
	 * ignored by this implementation
	 * @return the pet type's name, or {@code "<null>"} when the name is {@code null}
	 */
	@Override
	public String print(PetType petType, Locale locale) {
		String name = petType.getName();
		return name != null ? name : "<null>";
	}

	/**
	 * Parses the given text into a managed {@link PetType} entity.
	 *
	 * <p>
	 * The implementation fetches all known pet types from the database via
	 * {@link PetTypeRepository#findPetTypes()} and returns the first one whose
	 * {@code name} equals {@code text} (compared with
	 * {@link Objects#equals(Object, Object)}, i.e. case-sensitive). A linear scan is used
	 * because the set of pet types is small and bounded (cat, dog, hamster, bird, snake,
	 * lizard in the default seed data) and the result is cached by Hibernate's
	 * second-level cache anyway.
	 * </p>
	 *
	 * <p>
	 * The {@code locale} parameter is required by the {@link Formatter} contract but is
	 * unused here, for the same reason as in {@link #print(PetType, Locale)}.
	 * </p>
	 * @param text the user-submitted string to convert into a {@link PetType}
	 * @param locale the locale of the current user; accepted for contract conformance but
	 * ignored by this implementation
	 * @return the {@link PetType} whose name matches {@code text}
	 * @throws ParseException if no {@link PetType} with the given name exists in the data
	 * store; the error offset is always {@code 0} since the entire input string is
	 * unparseable in that case
	 */
	@Override
	public PetType parse(String text, Locale locale) throws ParseException {
		Collection<PetType> findPetTypes = this.types.findPetTypes();
		for (PetType type : findPetTypes) {
			if (Objects.equals(type.getName(), text)) {
				return type;
			}
		}
		throw new ParseException("type not found: " + text, 0);
	}

}

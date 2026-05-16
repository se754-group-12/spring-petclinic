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

import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Spring {@link Validator} implementation for {@link Pet} form submissions.
 *
 * <p>
 * This validator enforces the business rules that a {@code Pet} must satisfy before it
 * can be persisted via the create/update form on the owner detail page. It is registered
 * with the request-scoped {@code WebDataBinder} in
 * {@code PetController.initPetBinder(...)} and is invoked automatically by Spring MVC
 * when the controller method parameter is annotated with {@code @Valid}.
 * </p>
 *
 * <h2>Why not use Bean Validation annotations?</h2>
 * <p>
 * The {@code Pet} entity could express most of these rules with {@code @NotBlank} /
 * {@code @NotNull} JSR-380 annotations. A programmatic {@link Validator} is used instead
 * because:
 * </p>
 * <ul>
 * <li>The "type is required only when the pet is new" rule depends on the
 * {@link Pet#isNew()} state, which is awkward to express with a static annotation but
 * trivial to write in Java.</li>
 * <li>It keeps form-specific validation out of the persistence entity, so the entity
 * stays usable in non-form contexts (e.g. integration tests, REST endpoints).</li>
 * </ul>
 *
 * <h2>Rules enforced</h2>
 * <ol>
 * <li>{@code name} must not be {@code null}, empty, or whitespace-only.</li>
 * <li>{@code type} must not be {@code null} <em>when the pet is being created</em> (i.e.
 * {@link Pet#isNew()} returns {@code true}). On update, the type is allowed to remain
 * unchanged.</li>
 * <li>{@code birthDate} must not be {@code null}. Note that this validator does
 * <em>not</em> check whether the date is in the future — that rule is enforced in
 * {@code PetController} directly because it requires access to the current date.</li>
 * </ol>
 *
 * <h2>Thread-safety</h2>
 * <p>
 * This class is stateless and therefore thread-safe. A single instance per
 * {@code WebDataBinder} (i.e. per request) is created by {@code PetController}, but the
 * implementation would also support being declared as a singleton bean.
 * </p>
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
public class PetValidator implements Validator {

	/**
	 * Error code reported against any field that is missing a required value. Spring's
	 * {@code MessageSource} resolves this code (combined with the field name) to a
	 * localised user-facing message via {@code messages_*.properties}.
	 */
	private static final String REQUIRED = "required";

	/**
	 * Validates the given {@link Pet} instance and records any violations on the supplied
	 * {@link Errors} object.
	 *
	 * <p>
	 * The supplied {@code obj} is unconditionally cast to {@link Pet}; Spring guarantees
	 * the cast is safe because it only invokes this method after {@link #supports(Class)}
	 * has returned {@code true} for the target type.
	 * </p>
	 *
	 * <p>
	 * The method does not throw on validation failure — instead it calls
	 * {@link Errors#rejectValue(String, String, String)} for each violated rule. The
	 * surrounding controller is responsible for checking {@link Errors#hasErrors()} and
	 * re-rendering the form when violations are present.
	 * </p>
	 * @param obj the object to validate; must be a non-{@code null} {@link Pet} instance
	 * @param errors the binding-result holder onto which field-level errors are recorded;
	 * never {@code null}
	 */
	@Override
	public void validate(Object obj, Errors errors) {
		Pet pet = (Pet) obj;
		String name = pet.getName();
		// name validation
		if (!StringUtils.hasText(name)) {
			errors.rejectValue("name", REQUIRED, REQUIRED);
		}

		// type validation
		if (pet.isNew() && pet.getType() == null) {
			errors.rejectValue("type", REQUIRED, REQUIRED);
		}

		// birth date validation
		if (pet.getBirthDate() == null) {
			errors.rejectValue("birthDate", REQUIRED, REQUIRED);
		}
	}

	/**
	 * Indicates whether this validator can validate instances of the supplied class.
	 *
	 * <p>
	 * Returns {@code true} if {@code clazz} is {@link Pet} or any subclass thereof, which
	 * lets Spring MVC route only {@code Pet} command objects (and not, for example,
	 * {@code Owner} or {@code Visit}) to this validator.
	 * </p>
	 * @param clazz the {@link Class} that Spring is considering validating; never
	 * {@code null}
	 * @return {@code true} if instances of {@code clazz} are {@code Pet}s (or a subclass)
	 * and can be validated here, {@code false} otherwise
	 */
	@Override
	public boolean supports(Class<?> clazz) {
		return Pet.class.isAssignableFrom(clazz);
	}

}
